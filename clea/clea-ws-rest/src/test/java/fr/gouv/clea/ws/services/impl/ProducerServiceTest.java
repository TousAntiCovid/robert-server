package fr.gouv.clea.ws.services.impl;

import fr.gouv.clea.ws.model.SerializableDecodedVisit;
import fr.gouv.clea.ws.service.IProducerService;
import fr.gouv.clea.ws.utils.KafkaLSPDeserializer;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
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
    private IProducerService producerService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, SerializableDecodedVisit> consumer;

    @Value("${spring.kafka.template.default-topic}")
    private String defaultTopic;

    private static SerializableDecodedVisit createSerializableDecodedVisit(Long qrCodeScanTime, Long pivotDate, UUID locationTemporaryPublicId, byte[] encryptedLocationMessage) {
        return new SerializableDecodedVisit(
                qrCodeScanTime,
                EncryptedLocationSpecificPart.builder()
                        .version(RandomUtils.nextInt())
                        .type(RandomUtils.nextInt())
                        .locationTemporaryPublicId(locationTemporaryPublicId)
                        .encryptedLocationMessage(encryptedLocationMessage)
                        .build(),
                pivotDate
        );
    }

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

        byte[] encryptedLocationMessage1 = RandomUtils.nextBytes(21);
        byte[] encryptedLocationMessage2 = RandomUtils.nextBytes(22);
        byte[] encryptedLocationMessage3 = RandomUtils.nextBytes(23);

        Long pivotDate1 = RandomUtils.nextLong();
        Long pivotDate2 = RandomUtils.nextLong();
        Long pivotDate3 = RandomUtils.nextLong();

        Long qrCodeScanTime1 = RandomUtils.nextLong();
        Long qrCodeScanTime2 = RandomUtils.nextLong();
        Long qrCodeScanTime3 = RandomUtils.nextLong();

        List<SerializableDecodedVisit> decoded = List.of(
                createSerializableDecodedVisit(qrCodeScanTime1, pivotDate1, uuid1, encryptedLocationMessage1),
                createSerializableDecodedVisit(qrCodeScanTime2, pivotDate2, uuid2, encryptedLocationMessage2),
                createSerializableDecodedVisit(qrCodeScanTime3, pivotDate3, uuid3, encryptedLocationMessage3)
        );

        producerService.produce(decoded);

        ConsumerRecords<String, SerializableDecodedVisit> records = KafkaTestUtils.getRecords(consumer);
        assertThat(records.count()).isEqualTo(3);

        List<SerializableDecodedVisit> extracted = StreamSupport
                .stream(records.spliterator(), true)
                .map(ConsumerRecord::value)
                .collect(Collectors.toList());
        assertThat(extracted.size()).isEqualTo(3);

        SerializableDecodedVisit visit1 = extracted.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findFirst().orElse(null);
        assertThat(visit1).isNotNull();
        assertThat(visit1.getLocationTemporaryPublicId()).isEqualTo(uuid1);
        assertThat(visit1.getEncryptedLocationSpecificPart().getEncryptedLocationMessage()).isEqualTo(encryptedLocationMessage1);
        assertThat(visit1.getQrCodeScanTime()).isEqualTo(qrCodeScanTime1);
        assertThat(visit1.getPivotDate()).isEqualTo(pivotDate1);

        SerializableDecodedVisit visit2 = extracted.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findFirst().orElse(null);
        assertThat(visit2).isNotNull();
        assertThat(visit2.getLocationTemporaryPublicId()).isEqualTo(uuid2);
        assertThat(visit2.getEncryptedLocationSpecificPart().getEncryptedLocationMessage()).isEqualTo(encryptedLocationMessage2);
        assertThat(visit2.getQrCodeScanTime()).isEqualTo(qrCodeScanTime2);
        assertThat(visit2.getPivotDate()).isEqualTo(pivotDate2);

        SerializableDecodedVisit visit3 = extracted.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findFirst().orElse(null);
        assertThat(visit3).isNotNull();
        assertThat(visit3.getLocationTemporaryPublicId()).isEqualTo(uuid3);
        assertThat(visit3.getEncryptedLocationSpecificPart().getEncryptedLocationMessage()).isEqualTo(encryptedLocationMessage3);
        assertThat(visit3.getQrCodeScanTime()).isEqualTo(qrCodeScanTime3);
        assertThat(visit3.getPivotDate()).isEqualTo(pivotDate3);
    }

}