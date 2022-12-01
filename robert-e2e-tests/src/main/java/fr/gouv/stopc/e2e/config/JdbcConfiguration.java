package fr.gouv.stopc.e2e.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class JdbcConfiguration {

    private final ApplicationProperties applicationProperties;

    @Bean
    public DataSource cryptoDataSource() {
        return applicationProperties.getCryptoDatasource()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public JdbcTemplate cryptoJdbcOperations() {
        return new JdbcTemplate(cryptoDataSource());
    }

    @Bean
    @ConditionalOnProperty("robert.captcha-datasource.url")
    public DataSource captchaDataSource() {
        return applicationProperties.getCaptchaDatasource()
                .initializeDataSourceBuilder()
                .build();
    }
}
