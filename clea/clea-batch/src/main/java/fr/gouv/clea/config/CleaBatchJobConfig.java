package fr.gouv.clea.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CleaBatchJobConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Bean
    public Job cleaBatchJob(Step clusterIdentification, Step clustersIndexation, Step prefixesComputing, Step purgeIntermediateTable, Step clusterIndexGeneration) {
        return this.jobBuilderFactory.get("clea-batch-job")
                .incrementer(new RunIdIncrementer())
                .start(purgeIntermediateTable)
                .next(clusterIdentification)
                .next(prefixesComputing)
                .next(clustersIndexation)
                .next(clusterIndexGeneration)
                .build();
    }
}
