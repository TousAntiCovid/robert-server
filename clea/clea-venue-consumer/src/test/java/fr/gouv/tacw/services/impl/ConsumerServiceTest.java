package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.CleaVenueConsumerApplication;
import fr.gouv.tacw.dto.DecodedLocationSpecificPart;
import fr.gouv.tacw.services.IConsumerService;
import fr.gouv.tacw.utils.KafkaLSPSerializer;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = CleaVenueConsumerApplication.class)
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@TestPropertySource(properties = {"kafka.bootstrapAddress=localhost:9092"})
class ConsumerServiceTest {

    @Value("${spring.kafka.template.default-topic}")
    private String topicName;

    @SpyBean
    private IConsumerService consumerService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Producer<String, DecodedLocationSpecificPart> producer;

    @BeforeEach
    void init() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producer = new DefaultKafkaProducerFactory<>(configs, new StringSerializer(), new KafkaLSPSerializer()).createProducer();
    }

    @AfterEach
    void clean() {
        producer.close();
    }

    @Test
    @DisplayName("")
    void testSuccessfulConsumeVenue() {
        DecodedLocationSpecificPart decodedLocationSpecificPart = DecodedLocationSpecificPart
                .createDecodedLocationSpecificPart(UUID.randomUUID(), RandomUtils.nextBytes(20), RandomUtils.nextBytes(20));
        producer.send(new ProducerRecord<>(topicName, decodedLocationSpecificPart));
        producer.flush();
        await().atMost(1, TimeUnit.MINUTES)
                .untilAsserted(
                        () -> Mockito.verify(consumerService, Mockito.times(1))
                                .consumeVenue(decodedLocationSpecificPart)
                );
    }
}