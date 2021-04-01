package fr.gouv.tac.analytics.server.config.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.tac.analytics.server.model.kafka.Analytics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringSerializer;

@Configuration
@Slf4j
public class KafkaConfiguration {

    @Value(value = "${spring.kafka.template.default-topic}")
    private String defaultTopic;

    @Bean
    public KafkaTemplate<String, Analytics> analyticsKafkaTemplate(final ObjectMapper objectMapper, final ProducerFactory defaultSpringProducerFactory) {

        log.debug("conf default producer factory : {}", defaultSpringProducerFactory.getConfigurationProperties().toString());

        //a custom producer factory is instantiated in order to take advantage of the spring object mapper on date time management
        final ProducerFactory<String, Analytics> producerFactory = new DefaultKafkaProducerFactory<>(defaultSpringProducerFactory.getConfigurationProperties(),
                new StringSerializer(),
                new JsonSerializer<Analytics>(objectMapper));

        final KafkaTemplate<String, Analytics> analyticsKafkaTemplate = new KafkaTemplate<>(producerFactory);
        analyticsKafkaTemplate.setDefaultTopic(defaultTopic);
        return analyticsKafkaTemplate;
    }
}
