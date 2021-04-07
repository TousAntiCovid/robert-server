package fr.gouv.clea.init;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static fr.gouv.clea.config.BatchConstants.SINGLE_PLACE_CLUSTER_PERIOD_TABLE;

/**
 * This batch uses an intermediate table to store some information. This tasklet role is to empty the table before the job
 * runs to avoid taking into account precedent executions data.
 */
public class EmptyIntermediateDBTasklet implements Tasklet {

    private final JdbcOperations jdbcTemplate;

    public EmptyIntermediateDBTasklet(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        jdbcTemplate.execute("truncate " + SINGLE_PLACE_CLUSTER_PERIOD_TABLE + ";");
        return RepeatStatus.FINISHED;
    }
}
