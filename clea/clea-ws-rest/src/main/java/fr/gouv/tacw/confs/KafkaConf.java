package fr.gouv.tacw.confs;

import com.fasterxml.jackson.databind.JsonSerializer;
import fr.gouv.tacw.data.DecodedLocationSpecificPart;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

// @Configuration
public class KafkaConf {

    private final String bootstrapAddress;

    private final String topicName;

    private final int numPartitions;

    private final int replicationFactor;

    @Autowired
    public KafkaConf(
            @Value("${kafka.bootstrapAddress}") String bootstrapAddress,
            @Value("${kafka.topic.name}") String topicName,
            @Value("${kafka.topic.numPartitions}") int numPartitions,
            @Value("${kafka.topic.replicationFactor}") int replicationFactor
    ) {
        this.bootstrapAddress = bootstrapAddress;
        this.topicName = topicName;
        this.numPartitions = numPartitions;
        this.replicationFactor = replicationFactor;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic topic() {
        return new NewTopic(topicName, numPartitions, (short) replicationFactor);
    }

    @Bean
    public ProducerFactory<String, DecodedLocationSpecificPart> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapAddress
        );
        configProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        configProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, DecodedLocationSpecificPart> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
