package fr.gouv.clea.config;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static fr.gouv.clea.config.BatchConstants.SQL_TRUNCATE_TABLE_CLUSTERPERIODS;

@Configuration
public class PurgeIntermediateTableStepBatchConfig {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Step purgeIntermediateTable(final JdbcTemplate jdbcTemplate) {
        return stepBuilderFactory.get("purgeIntermediateTable")
                .tasklet(clearTable(jdbcTemplate))
                .build();
    }

    @Bean
    public Tasklet clearTable(final JdbcTemplate jdbcTemplate) {
        return (contribution, chunkContext) -> {
            jdbcTemplate.execute(SQL_TRUNCATE_TABLE_CLUSTERPERIODS);
            return RepeatStatus.FINISHED;
        };
    }
}
