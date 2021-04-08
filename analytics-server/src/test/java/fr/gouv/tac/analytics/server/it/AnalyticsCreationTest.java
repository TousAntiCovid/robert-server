package fr.gouv.tac.analytics.server.it;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.tac.analytics.server.AnalyticsServerApplication;
import fr.gouv.tac.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tac.analytics.server.controller.vo.TimestampedEventVo;
import fr.gouv.tac.analytics.server.model.kafka.Analytics;
import fr.gouv.tac.analytics.server.model.kafka.TimestampedEvent;
import fr.gouv.tac.analytics.server.utils.TestUtils;
import fr.gouv.tac.analytics.server.utils.UriConstants;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ActiveProfiles(value = "test")
@SpringBootTest(classes = AnalyticsServerApplication.class)
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9094", "port=9094"}, topics = "topicNameForTest")
public class AnalyticsCreationTest {

    private static final int QUEUE_READ_TIMEOUT = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Value("${analyticsserver.controller.path.prefix}"+ UriConstants.API_V1 + UriConstants.ANALYTICS)
    private String analyticsControllerPath;

    private KafkaMessageListenerContainer<String, Analytics> container;

    private final List<Analytics> records = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        records.clear();
        final Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(kafkaProperties.getConsumer().getGroupId(), "false", embeddedKafkaBroker);
        final DefaultKafkaConsumerFactory<String, Analytics> defaultKafkaConsumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new JsonDeserializer<>(Analytics.class, objectMapper));
        final ContainerProperties containerProperties = new ContainerProperties(kafkaProperties.getTemplate().getDefaultTopic());
        container = new KafkaMessageListenerContainer<>(defaultKafkaConsumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, Analytics>) message -> records.add(message.value()));
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    public void tearDown() {
        container.stop();
    }

    @Test
    @WithMockUser
    public void itShouldStoreValidAnalytics() throws Exception {
        final AnalyticsVo analyticsVo = buildAnalyticsVo();

        final List<TimestampedEvent> expectedEvents = analyticsVo.getEvents().stream()
                .map(TestUtils::convertTimestampedEvent)
                .collect(Collectors.toList());

        final List<TimestampedEvent> expectedErrors = analyticsVo.getErrors().stream()
                .map(TestUtils::convertTimestampedEvent)
                .collect(Collectors.toList());

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isOk())
                .andExpect(content().string(is(emptyString())));


        await().atMost(QUEUE_READ_TIMEOUT, SECONDS).untilAsserted(() -> assertThat(records).isNotEmpty());

        assertThat(records).hasSize(1);
        final Analytics analyticsResult = records.get(0);

        assertThat(analyticsResult.getCreationDate()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        assertThat(analyticsResult.getInstallationUuid()).isEqualTo(analyticsVo.getInstallationUuid());
        assertThat(analyticsResult.getInfos()).containsExactlyInAnyOrderEntriesOf(analyticsVo.getInfos());
        assertThat(analyticsResult.getEvents()).containsExactlyInAnyOrderElementsOf(expectedEvents);
        assertThat(analyticsResult.getErrors()).containsExactlyInAnyOrderElementsOf(expectedErrors);

    }


    private AnalyticsVo buildAnalyticsVo() {
        final Map<String, String> infos = Map.of("info1", "info1Value", "info2", "info2value");

        final ZonedDateTime timestamp = ZonedDateTime.parse("2020-12-17T10:59:17.123Z[UTC]");

        final TimestampedEventVo event1 = TimestampedEventVo.builder().name("eventName1").timestamp(timestamp).desc("event1 description").build();
        final TimestampedEventVo event2 = TimestampedEventVo.builder().name("eventName2").timestamp(timestamp).build();

        final TimestampedEventVo error1 = TimestampedEventVo.builder().name("errorName1").timestamp(timestamp).build();
        final TimestampedEventVo error2 = TimestampedEventVo.builder().name("errorName2").timestamp(timestamp).desc("error2 description").build();

        return AnalyticsVo.builder()
                .installationUuid("some installation uuid")
                .infos(infos)
                .events(Arrays.asList(event1, event2))
                .errors(Arrays.asList(error1, error2))
                .build();
    }


}

