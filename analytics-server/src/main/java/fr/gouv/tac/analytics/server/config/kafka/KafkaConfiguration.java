package fr.gouv.tac.analytics.server.config.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringSerializer;

@Configuration
@Slf4j
public class KafkaConfiguration {

    @Value(value = "${spring.kafka.template.default-topic}")
    private String defaultTopic;

    @Bean
    public KafkaTemplate<String, String> analyticsKafkaTemplate(final ObjectMapper objectMapper, final ProducerFactory defaultSpringProducerFactory) {

        log.debug("conf default producer factory : {}", defaultSpringProducerFactory.getConfigurationProperties().toString());

        final ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(defaultSpringProducerFactory.getConfigurationProperties(),
                new StringSerializer(),
                new StringSerializer());

        final KafkaTemplate<String, String> analyticsKafkaTemplate = new KafkaTemplate<>(producerFactory);
        analyticsKafkaTemplate.setDefaultTopic(defaultTopic);
        return analyticsKafkaTemplate;
    }
}
