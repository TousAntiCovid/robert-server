package fr.gouv.tacw.confs;

import fr.gouv.tacw.dto.DecodedLocationSpecificPart;
import fr.gouv.tacw.utils.KafkaLSPDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

// @Configuration
// @EnableKafka
public class KafkaConf {

    private final String bootstrapAddress;

    private final String groupId;

    @Autowired
    public KafkaConf(
            @Value("${kafka.bootstrapAddress}") String bootstrapAddress,
            @Value("${kafka.group.id}") String groupId
    ) {
        this.bootstrapAddress = bootstrapAddress;
        this.groupId = groupId;
    }

    @Bean
    public ConsumerFactory<String, DecodedLocationSpecificPart> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaLSPDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DecodedLocationSpecificPart> listenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DecodedLocationSpecificPart> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(this.consumerFactory());
        return factory;
    }
}
