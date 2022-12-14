package fr.gouv.stopc.e2e.config;

import fr.gouv.stopc.e2e.mobileapplication.repository.CaptchaRepository;
import fr.gouv.stopc.e2e.mobileapplication.repository.PostgresCaptchaRepository;
import fr.gouv.stopc.e2e.mobileapplication.repository.model.Captcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Slf4j
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

    @Configuration
    @Profile("real-captcha")
    public class RealCaptchaServerConfiguration {

        @Bean
        public DataSource captchaDataSource() {
            return applicationProperties.getCaptchaDatasource()
                    .initializeDataSourceBuilder()
                    .build();
        }

        @Bean
        public CaptchaRepository captchaRepository() {
            return new PostgresCaptchaRepository(captchaDataSource());
        }
    }

    @Configuration
    @Profile("!real-captcha")
    public class MockCaptchaServerConfiguration {

        @Bean
        public CaptchaRepository captchaRepository() {
            return () -> new Captcha("challengeId", "valid");
        }
    }

}
