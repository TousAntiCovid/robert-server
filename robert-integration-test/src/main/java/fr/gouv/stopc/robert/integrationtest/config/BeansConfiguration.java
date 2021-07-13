package fr.gouv.stopc.robert.integrationtest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BeansConfiguration {

    private final ApplicationProperties applicationProperties;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
