package fr.gouv.clea.config;

import fr.gouv.clea.prefixes.ListItemReader;
import fr.gouv.clea.prefixes.PrefixesMemoryWriter;
import fr.gouv.clea.service.PrefixesStorageService;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

import static fr.gouv.clea.config.BatchConstants.SQL_SELECT_DISTINCT_FROM_CLUSTERPERIODS_ORDERBY_LTID;

@Configuration
public class PrefixesStepBatchConfig {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PrefixesStorageService prefixesStorageService;

    @Autowired
    private BatchProperties properties;

    @Bean
    public Step prefixesComputing() {
        return stepBuilderFactory.get("prefixesComputing")
                .<List<String>, List<String>>chunk(properties.getPrefixesComputingStepChunkSize())
                .reader(ltidListDBReader())
                .writer(new PrefixesMemoryWriter(properties, prefixesStorageService))
                .build();
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
        reader.setSql(SQL_SELECT_DISTINCT_FROM_CLUSTERPERIODS_ORDERBY_LTID);
        reader.setRowMapper((rs, i) -> rs.getString(1));
        return new ListItemReader(reader);
    }
}
