package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.data.DecodedLocationSpecificPart;
import fr.gouv.tacw.services.IProducerService;
import fr.gouv.tacw.utils.KafkaLSPDeserializer;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
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
@TestPropertySource(properties = {"kafka.bootstrapAddress=localhost:9092", "kafka.consumer.group.id:group1"})
@Import(ProcessServiceTestConfiguration.class)
class ProducerServiceTest {

    @Autowired
    private IProducerService processService;

    @Autowired
    private Consumer<String, DecodedLocationSpecificPart> consumer;

    private static DecodedLocationSpecificPart createDecodedLocationSpecificPart(
            String qrCode,
            UUID locationTemporaryPublicId,
            byte[] locationTemporarySecretKey,
            byte[] encryptedLocationContactMessage
    ) {
        return new DecodedLocationSpecificPart(
                0,
                0,
                0,
                false,
                locationTemporaryPublicId,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                locationTemporarySecretKey,
                encryptedLocationContactMessage,
                0,
                qrCode
        );
    }

    @BeforeEach
    void init() {
        assertThat(processService).isNotNull();
        assertThat(consumer).isNotNull();
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
                createDecodedLocationSpecificPart("qr1", uuid1, locationTemporarySecretKey1, encryptedLocationContactMessage1),
                createDecodedLocationSpecificPart("qr2", uuid2, locationTemporarySecretKey2, encryptedLocationContactMessage2),
                createDecodedLocationSpecificPart("qr3", uuid3, locationTemporarySecretKey3, encryptedLocationContactMessage3)
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

@TestConfiguration
class ProcessServiceTestConfiguration {

    private final String bootstrapAddress;

    private final String groupId;

    @Autowired
    public ProcessServiceTestConfiguration(
            @Value("${kafka.bootstrapAddress}") String bootstrapAddress,
            @Value("${kafka.consumer.group.id}") String groupId
    ) {
        this.bootstrapAddress = bootstrapAddress;
        this.groupId = groupId;
    }

    @Bean
    public Consumer<String, DecodedLocationSpecificPart> consumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaLSPDeserializer.class);
        Consumer<String, DecodedLocationSpecificPart> consumer = new DefaultKafkaConsumerFactory<String, DecodedLocationSpecificPart>(props).createConsumer();
        consumer.subscribe(Collections.singleton("qrCodes"));
        consumer.poll(0);
        return consumer;
    }
}