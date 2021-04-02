package fr.gouv.clea.config;

import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceExposedVisits;
import fr.gouv.clea.identification.ExposedVisitPartitioner;
import fr.gouv.clea.identification.RiskConfigurationService;
import fr.gouv.clea.identification.SinglePlaceExposedVisitsItemWriter;
import fr.gouv.clea.identification.SinglePlaceExposedVisitsProcessor;
import fr.gouv.clea.identification.reader.ExposedVisitItemReader;
import fr.gouv.clea.identification.reader.SinglePlaceExposedVisitItemReader;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.processors.IndexationProcessor;
import fr.gouv.clea.indexation.readers.IndexationReader;
import fr.gouv.clea.indexation.writers.IndexationWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
	private BatchProperties properties;

	@Autowired
	private DataSource dataSource;

	@Bean
	public Job identificationPartitionedJob(Step identificationPartitionedMasterStep) {
		return this.jobBuilderFactory.get("partitionedJob")
				.incrementer(new RunIdIncrementer())
				.start(identificationPartitionedMasterStep)
				.next(clusterIndexation())
				.build();
	}

	@Bean
	public Step identificationPartitionedMasterStep(PartitionHandler partitionHandler) {
		return this.stepBuilderFactory.get("identification-partitioned-step-master")
				.partitioner("partitioner", partitioner())
				.partitionHandler(partitionHandler)
				.build();
	}

	@Bean
	public Step clusterIndexation() {
		return stepBuilderFactory.get("clusterIndexation")
				.<List<SinglePlaceCluster>, HashMap<String, ClusterFile>> chunk(1)
				.reader(new IndexationReader())
				.processor(new IndexationProcessor(properties))
				.writer(new IndexationWriter(properties))
				.build();
	}

	@Bean
	public Step identificationStepWorker(ExposedVisitItemReader exposedVisitItemReader) {
		final SinglePlaceExposedVisitItemReader reader = new SinglePlaceExposedVisitItemReader();
		reader.setDelegate(exposedVisitItemReader);
		return stepBuilderFactory.get("identification-step-worker")
				.listener(promotionListener())
				.<SinglePlaceExposedVisits, SinglePlaceCluster>chunk(properties.getChunkSize())
				.reader(reader)
				.processor(new SinglePlaceExposedVisitsProcessor(properties, riskConfigurationService))
				.writer(new SinglePlaceExposedVisitsItemWriter())
				.build();
	}

	@Bean
	public TaskExecutorPartitionHandler partitionHandler(TaskExecutor taskExecutor, Step identificationStepWorker) {
		TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();

		partitionHandler.setGridSize(properties.getGridSize());
		partitionHandler.setStep(identificationStepWorker);
		partitionHandler.setTaskExecutor(taskExecutor());
		return partitionHandler;
	}

	@Bean
	public ExposedVisitPartitioner partitioner() {
		return new ExposedVisitPartitioner(this.dataSource);
	}

	@Bean
	@StepScope
	public ExposedVisitItemReader exposedVisitItemReader(PagingQueryProvider pagingQueryProvider,
												 @Value("#{stepExecutionContext['ltid']}") UUID ltid) {

		return new ExposedVisitItemReader(this.dataSource, pagingQueryProvider, Map.of("ltid", ltid));
	}

	@Bean
	@StepScope
	public SqlPagingQueryProviderFactoryBean pagingQueryProvider(@Value("#{stepExecutionContext['ltid']}") UUID ltid) {

		SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setSelectClause("*");
		factoryBean.setFromClause(EXPOSED_VISITS_TABLE);
		factoryBean.setWhereClause(LTID_COLUMN + " = :ltid");
		factoryBean.setSortKeys(Map.of(PERIOD_COLUMN, Order.ASCENDING, TIMESLOT_COLUMN,Order.ASCENDING));
		return factoryBean;
	}

	@Bean
	public ExecutionContextPromotionListener promotionListener() {
		ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
		listener.setKeys(new String[] { CLUSTERMAP_JOB_CONTEXT_KEY });
		return listener;
	}
	
	@Bean
	TaskExecutor taskExecutor() {
	    return new SimpleAsyncTaskExecutor("batch-ident");
	}

}
