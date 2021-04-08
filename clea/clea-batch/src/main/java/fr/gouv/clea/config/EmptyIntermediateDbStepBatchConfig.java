package fr.gouv.clea.config;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static fr.gouv.clea.config.BatchConstants.SINGLE_PLACE_CLUSTER_PERIOD_TABLE;

@Configuration
public class EmptyIntermediateDbStepBatchConfig {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private DataSource dataSource;

    @Bean
    public Step emptyIntermediateDb() {
        return stepBuilderFactory.get("emptyIntermediateDb")
                .tasklet(emptyDb())
                .build();
    }

    @Bean
    public Tasklet emptyDb() {
        return (contribution, chunkContext) -> {
            JdbcOperations jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute("truncate " + SINGLE_PLACE_CLUSTER_PERIOD_TABLE + ";");
            return RepeatStatus.FINISHED;
        };
    }
}
