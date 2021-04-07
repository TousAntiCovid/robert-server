package fr.gouv.clea.config;

import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class NoPersistenceBatchConfigurer extends DefaultBatchConfigurer {
    @Override
    public void setDataSource(DataSource dataSource) {
        // prevents spring batch from storing JobRepository in database and use in-memory map instead
    }
}
