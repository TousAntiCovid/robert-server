package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.dtos.DecodedLocationSpecificPart;
import fr.gouv.tacw.services.IProducerService;
import fr.gouv.tacw.utils.KafkaLSPDeserializer;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@TestPropertySource(properties = {"kafka.bootstrapAddress=localhost:9092"})
class ProducerServiceTest {

    @Autowired
    private IProducerService processService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, DecodedLocationSpecificPart> consumer;

    @Value("${spring.kafka.template.default-topic}")
    private String defaultTopic;

    @BeforeEach
    void init() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("consumer", "false", embeddedKafkaBroker));
        consumer = new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), new KafkaLSPDeserializer()).createConsumer();
        consumer.subscribe(Collections.singleton(defaultTopic));
    }

    @Test
    @DisplayName("test that produce send decoded lsps to kafka and that we can read them back")
    void testProduce() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        byte[] locationTemporarySecretKey1 = RandomUtils.nextBytes(18);
        byte[] locationTemporarySecretKey2 = RandomUtils.nextBytes(19);
        byte[] locationTemporarySecretKey3 = RandomUtils.nextBytes(20);

        byte[] encryptedLocationContactMessage1 = RandomUtils.nextBytes(21);
        byte[] encryptedLocationContactMessage2 = RandomUtils.nextBytes(22);
        byte[] encryptedLocationContactMessage3 = RandomUtils.nextBytes(23);

        List<DecodedLocationSpecificPart> decoded = List.of(
                DecodedLocationSpecificPart.createDecodedLocationSpecificPart("qr1", uuid1, locationTemporarySecretKey1, encryptedLocationContactMessage1),
                DecodedLocationSpecificPart.createDecodedLocationSpecificPart("qr2", uuid2, locationTemporarySecretKey2, encryptedLocationContactMessage2),
                DecodedLocationSpecificPart.createDecodedLocationSpecificPart("qr3", uuid3, locationTemporarySecretKey3, encryptedLocationContactMessage3)
        );

        processService.produce(decoded);

        ConsumerRecords<String, DecodedLocationSpecificPart> records = KafkaTestUtils.getRecords(consumer);
        assertThat(records.count()).isEqualTo(3);

        List<DecodedLocationSpecificPart> extracted = StreamSupport
                .stream(records.spliterator(), true)
                .map(ConsumerRecord::value)
                .collect(Collectors.toList());
        assertThat(extracted.size()).isEqualTo(3);

        DecodedLocationSpecificPart dlsp1 = extracted.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findFirst().orElse(null);
        assertThat(dlsp1).isNotNull();
        assertThat(dlsp1.getQrCode()).isNull();
        assertThat(dlsp1.getLocationTemporaryPublicId()).isEqualTo(uuid1);
        assertThat(dlsp1.getLocationTemporarySecretKey()).isEqualTo(locationTemporarySecretKey1);
        assertThat(dlsp1.getEncryptedLocationContactMessage()).isEqualTo(encryptedLocationContactMessage1);

        DecodedLocationSpecificPart dlsp2 = extracted.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findFirst().orElse(null);
        assertThat(dlsp2).isNotNull();
        assertThat(dlsp2.getQrCode()).isNull();
        assertThat(dlsp2.getLocationTemporaryPublicId()).isEqualTo(uuid2);
        assertThat(dlsp2.getLocationTemporarySecretKey()).isEqualTo(locationTemporarySecretKey2);
        assertThat(dlsp2.getEncryptedLocationContactMessage()).isEqualTo(encryptedLocationContactMessage2);

        DecodedLocationSpecificPart dlsp3 = extracted.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findFirst().orElse(null);
        assertThat(dlsp3).isNotNull();
        assertThat(dlsp3.getQrCode()).isNull();
        assertThat(dlsp3.getLocationTemporaryPublicId()).isEqualTo(uuid3);
        assertThat(dlsp3.getLocationTemporarySecretKey()).isEqualTo(locationTemporarySecretKey3);
        assertThat(dlsp3.getEncryptedLocationContactMessage()).isEqualTo(encryptedLocationContactMessage3);
    }

}