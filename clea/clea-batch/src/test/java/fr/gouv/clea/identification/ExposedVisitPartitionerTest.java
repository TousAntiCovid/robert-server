package fr.gouv.clea.identification;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static fr.gouv.clea.config.BatchConstants.EXPOSED_VISITS_TABLE;
import static fr.gouv.clea.config.BatchConstants.LTID_COLUMN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ExposedVisitPartitionerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcOperations jdbcTemplate;

    @InjectMocks
    private ExposedVisitPartitioner partitioner;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(partitioner, "jdbcTemplate", jdbcTemplate);
    }

    @Test
    void partition_returns_map_with_as_much_execution_contexts_as_LTids_returned_by_db() {

        final String expectedRequest = "SELECT DISTINCT " + LTID_COLUMN + " FROM " + EXPOSED_VISITS_TABLE;
        when(jdbcTemplate.queryForList(eq(expectedRequest), eq(String.class))).thenReturn(List.of("lTid1", "lTid1", "lTid2", "lTid3", "lTid3", "lTid3"));

        final Map<String, ExecutionContext> result = partitioner.partition(4);

        log.info(result.toString());
        final int differentIdNumbers = 3;
        assertThat(result).hasSize(differentIdNumbers);

    }
}
