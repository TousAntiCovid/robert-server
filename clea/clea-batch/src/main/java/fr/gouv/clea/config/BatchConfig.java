package fr.gouv.clea.config;

import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceExposedVisits;
import fr.gouv.clea.identification.*;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.processors.SinglePlaceClusterBuilder;
import fr.gouv.clea.indexation.readers.IteratorItemReader;
import fr.gouv.clea.indexation.readers.MemoryMapItemReader;
import fr.gouv.clea.indexation.writers.IndexationWriter;
import fr.gouv.clea.mapper.SinglePlaceClusterPeriodMapper;
import fr.gouv.clea.prefixes.ListItemReader;
import fr.gouv.clea.prefixes.PrefixesMemoryWriter;
import fr.gouv.clea.prefixes.PrefixesStorageService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.sql.DataSource;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static fr.gouv.clea.config.BatchConstants.*;

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
                .start(clusterIdentification())
                .next(prefixesComputing())
                .next(clusterIndexation())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<String> ltidDBReader() {
        JdbcCursorItemReader<String> reader = new JdbcCursorItemReader<>();
        reader.setVerifyCursorPosition(false);
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
        CompositeItemProcessor<String, List<SinglePlaceClusterPeriod>> compositeProcessor = new CompositeItemProcessor<>();
        compositeProcessor.setDelegates(List.of(
                exposedVisitBuilder(),                  // from String to ExposedVisit
                singleClusterPlaceBuilder(),            // from ExposedVisit to SingleClusterPlace
                singlePlaceClusterPeriodListBuilder())  // from SingleClusterPlace to List<SinglePlaceClusterPeriods>
        );

        return stepBuilderFactory.get("identification")
                .<String, List<SinglePlaceClusterPeriod>>chunk(1000)
                .reader(ltidDBReader())
                .processor(compositeProcessor)
                .writer(new SinglePlaceClusterPeriodListWriter(mapper, dataSource))
                .taskExecutor(taskExecutor())
                .throttleLimit(10)
                .build();
    }

    @Bean
    public Step prefixesComputing() {
        return stepBuilderFactory.get("prefixes")
                .<List<String>, List<String>>chunk(1000)
                .reader(ltidListDBReader())
                .writer(new PrefixesMemoryWriter(properties, prefixesStorageService))
                .taskExecutor(taskExecutor())
                .throttleLimit(10)
                .build();
    }

    @Bean
    public Step clusterIndexation() {
        MemoryMapItemReader reader = new MemoryMapItemReader((prefixesStorageService.getPrefixWithAssociatedLtidsMap().entrySet())::iterator);
        return stepBuilderFactory.get("indexation")
                .<Map.Entry<String, List<String>>, ClusterFile>chunk(1)
                .reader(reader)
                .processor(new SinglePlaceClusterBuilder(dataSource, mapper, properties)) // build a Map of ClusterFile at once
                .writer(new IndexationWriter(properties, prefixesStorageService)) // build Files and index
                .build();
    }

    @Bean
    TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("batch-ident");
    }

    @Bean
    @StepScope
    public ItemProcessor<String, SinglePlaceExposedVisits> exposedVisitBuilder() {
        return new SinglePlaceExposedVisitsBuilder(dataSource);
    }

    @Bean
    @StepScope
    public ItemProcessor<SinglePlaceExposedVisits, SinglePlaceCluster> singleClusterPlaceBuilder() {
        return new SinglePlaceExposedVisitsProcessor(properties, riskConfigurationService);
    }

    @Bean
    @StepScope
    public ItemProcessor<SinglePlaceCluster, List<SinglePlaceClusterPeriod>> singlePlaceClusterPeriodListBuilder() {
        return new ClusterToPeriodsProcessor(mapper);
    }
}
