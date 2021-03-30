package fr.gouv.tousantic.analytics.server.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.tousantic.analytics.server.model.kafka.Analytics;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaConfiguration {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Value(value = "${spring.kafka.template.default-topic}")
    private String defaultTopic;

    @Bean
    public KafkaTemplate<String, Analytics> analyticsKafkaTemplate(final ObjectMapper objectMapper) {
        final Map<String, Object> props = Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);

        final ProducerFactory<String, Analytics> producerFactory = new DefaultKafkaProducerFactory<>(props,
                new StringSerializer(),
                new JsonSerializer<Analytics>(objectMapper));

        final KafkaTemplate<String, Analytics> analyticsKafkaTemplate = new KafkaTemplate<>(producerFactory);
        analyticsKafkaTemplate.setDefaultTopic(defaultTopic);
        return analyticsKafkaTemplate;
    }
}
