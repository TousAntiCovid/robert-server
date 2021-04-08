package fr.gouv.clea.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CleaBatchJobConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Bean
    public Job cleaBatchJob(Step clusterIdentification, Step clustersIndexation, Step prefixesComputing, Step emptyIntermediateDb) {
        return this.jobBuilderFactory.get("clea-batch-job")
                .incrementer(new RunIdIncrementer())
                .start(emptyIntermediateDb)
                .next(clusterIdentification)
                .next(prefixesComputing)
                .next(clustersIndexation)
                .build();
    }

}
