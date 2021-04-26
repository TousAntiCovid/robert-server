package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.service.IConsumerService;
import fr.gouv.clea.consumer.utils.KafkaSerializer;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class ConsumerServiceTest {

    @Value("${spring.kafka.template.default-topic}")
    private String topicName;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @SpyBean
    private IConsumerService consumerService;

    private Producer<String, DecodedVisit> producer;

    @BeforeEach
    void init() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producer = new DefaultKafkaProducerFactory<>(configs, new StringSerializer(), new KafkaSerializer()).createProducer();
    }

    @AfterEach
    void clean() {
        producer.close();
    }

    @Test
    @DisplayName("test that kafka listener triggers when something is sent to the queue")
    void testCanConsumeMessageSentinDefaultQueue() {
        DecodedVisit decodedVisit = new DecodedVisit(
                Instant.now(),
                EncryptedLocationSpecificPart.builder()
                        .version(RandomUtils.nextInt())
                        .type(RandomUtils.nextInt())
                        .locationTemporaryPublicId(UUID.randomUUID())
                        .encryptedLocationMessage(RandomUtils.nextBytes(20))
                        .build(),
                RandomUtils.nextBoolean()
        );

        producer.send(new ProducerRecord<>(topicName, decodedVisit));
        producer.flush();

        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> verify(consumerService, times(1))
                                .consume(any(DecodedVisit.class))
                );
    }
}