package fr.gouv.clea.config;

import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceExposedVisits;
import fr.gouv.clea.identification.ExposedVisitRowMapper;
import fr.gouv.clea.identification.RiskConfigurationService;
import fr.gouv.clea.identification.processor.ClusterToPeriodsProcessor;
import fr.gouv.clea.identification.processor.SinglePlaceExposedVisitsBuilder;
import fr.gouv.clea.identification.processor.SinglePlaceExposedVisitsProcessor;
import fr.gouv.clea.identification.writer.SinglePlaceClusterPeriodListWriter;
import fr.gouv.clea.indexation.IndexationPartitioner;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.processors.SinglePlaceClusterBuilder;
import fr.gouv.clea.indexation.readers.StepExecutionContextReader;
import fr.gouv.clea.indexation.writers.IndexationWriter;
import fr.gouv.clea.init.EmptyIntermediateDBTasklet;
import fr.gouv.clea.mapper.SinglePlaceClusterPeriodMapper;
import fr.gouv.clea.prefixes.ListItemReader;
import fr.gouv.clea.prefixes.PrefixesMemoryWriter;
import fr.gouv.clea.prefixes.PrefixesStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static fr.gouv.clea.config.BatchConstants.*;

@Slf4j
@Configuration
public class BatchConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private RiskConfigurationService riskConfigurationService;

    @Autowired
    private PrefixesStorageService prefixesStorageService;

    @Autowired
    private BatchProperties properties;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SinglePlaceClusterPeriodMapper mapper;

    @Bean
    public Job identificationJob() {
        return this.jobBuilderFactory.get("identificationJob")
                .incrementer(new RunIdIncrementer())
                .start(emptyIntermediateDBstep())
                .next(clusterIdentification())
                .next(prefixesComputing())
                .next(masterStepIndexation())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<String> ltidDBReader() {

        JdbcCursorItemReader<String> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("select distinct " + LTID_COL + " from " + EXPOSED_VISITS_TABLE + " order by " + LTID_COL);
        reader.setRowMapper((rs, i) -> rs.getString(1));
        return reader;
    }

    /**
     * Agregates elements from delegate to a list and returns it all in one
     *
     * @return list of ltids as strings
     */
    @Bean
    public ItemReader<List<String>> ltidListDBReader() {
        JdbcCursorItemReader<String> reader = new JdbcCursorItemReader<>();
        reader.setSaveState(false);
        reader.setDataSource(dataSource);
        reader.setVerifyCursorPosition(false);
        reader.setSql("select distinct " + LTID_COL + " from " + SINGLE_PLACE_CLUSTER_PERIOD_TABLE + " ORDER BY " + LTID_COL);
        reader.setRowMapper((rs, i) -> rs.getString(1));
        return new ListItemReader(reader);
    }

    // =================
    // Cluster identification & indexation steps
    // =================

    @Bean
    public Step clusterIdentification() {
        final CompositeItemProcessor<String, List<SinglePlaceClusterPeriod>> compositeProcessor = new CompositeItemProcessor<>();
        compositeProcessor.setDelegates(List.of(
                exposedVisitBuilder(),                  // from String to ExposedVisit
                singleClusterPlaceBuilder(),            // from ExposedVisit to SingleClusterPlace
                singlePlaceClusterPeriodListBuilder())  // from SingleClusterPlace to List<SinglePlaceClusterPeriods>
        );

        final SynchronizedItemStreamReader<String> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(ltidDBReader());
        return stepBuilderFactory.get("identification")
                .<String, List<SinglePlaceClusterPeriod>>chunk(1000)
                .reader(reader)
                .processor(compositeProcessor)
                .writer(new SinglePlaceClusterPeriodListWriter(dataSource))
                .taskExecutor(taskExecutor())
                .throttleLimit(20)
                .build();
    }

    @Bean
    public Step emptyIntermediateDBstep() {
        return stepBuilderFactory.get("emptyIntermediateDB")
                .tasklet(emptyIntermediateDB())
                .build();
    }

    @Bean
    public Step prefixesComputing() {
        return stepBuilderFactory.get("prefixes")
                .<List<String>, List<String>>chunk(1000)
                .reader(ltidListDBReader())
                .writer(new PrefixesMemoryWriter(properties, prefixesStorageService))
                .build();
    }

    @Bean
    public Step clusterIndexation() {
//        MemoryMapItemReader reader = new MemoryMapItemReader((prefixesStorageService.getPrefixWithAssociatedLtidsMap().entrySet())::iterator);
        return stepBuilderFactory.get("indexation")
                //FIXME: set config for chunk size
                .<Map.Entry<String, List<String>>, ClusterFile>chunk(1)
                .reader(memoryMapItemReader(null, null))
                .processor(singlePlaceClusterBuilder()) // build a Map of ClusterFile at once
                .writer(indexationWriter()) // build Files and index
                .build();
    }

    @Bean
    public IndexationWriter indexationWriter() {
        return new IndexationWriter(properties, prefixesStorageService);
    }

    @Bean
    public ItemProcessor<Map.Entry<String, List<String>>, ClusterFile> singlePlaceClusterBuilder() {
        return new SinglePlaceClusterBuilder(dataSource, mapper, properties);
    }

    @Bean
    @StepScope
    public ItemReader<Map.Entry<String, List<String>>> memoryMapItemReader(@Value("#{stepExecutionContext['prefix']}") String prefix,
                                                                                      @Value("#{stepExecutionContext['ltids']}") List<String> ltids) {
        return new StepExecutionContextReader(prefix, ltids);
    }

    @Bean
    public TaskExecutorPartitionHandler partitionHandler() {
        final TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        //FIXME: set config for gridSize
        partitionHandler.setGridSize(5);
        partitionHandler.setStep(clusterIndexation());
        partitionHandler.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return partitionHandler;
    }

    @Bean
    public Step masterStepIndexation() {
        return this.stepBuilderFactory.get("indexationMaster")
                .partitioner("partitioner", prefixPartitioner())
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    public Partitioner prefixPartitioner() {
        log.info("callToPartitioner");
        return new IndexationPartitioner(prefixesStorageService);
    }

    @Bean
    TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("batch-ident");
    }

    @Bean
    public Tasklet emptyIntermediateDB() {
        return new EmptyIntermediateDBTasklet(dataSource);
    }

    @Bean
    public ItemProcessor<String, SinglePlaceExposedVisits> exposedVisitBuilder() {
        return new SinglePlaceExposedVisitsBuilder(dataSource, new ExposedVisitRowMapper());
    }

    @Bean
    public ItemProcessor<SinglePlaceExposedVisits, SinglePlaceCluster> singleClusterPlaceBuilder() {
        return new SinglePlaceExposedVisitsProcessor(properties, riskConfigurationService);
    }

    @Bean
    public ItemProcessor<SinglePlaceCluster, List<SinglePlaceClusterPeriod>> singlePlaceClusterPeriodListBuilder() {
        return new ClusterToPeriodsProcessor(mapper);
    }
}
