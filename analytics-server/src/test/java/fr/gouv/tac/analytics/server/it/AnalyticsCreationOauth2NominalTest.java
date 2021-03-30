package fr.gouv.tac.analytics.server.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.tac.analytics.server.AnalyticsServerApplication;
import fr.gouv.tac.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tac.analytics.server.model.kafka.Analytics;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles(value = "test")
@SpringBootTest(classes = AnalyticsServerApplication.class)
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9094", "port=9094"}, topics = "topicNameForTest")
public class AnalyticsCreationOauth2NominalTest {

    private static final int QUEUE_READ_TIMEOUT = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Value("${analyticsserver.controller.analytics.path}")
    private String analyticsControllerPath;

    @Value("${analyticsserver.robert_jwt_analyticsprivatekey}")
    private String jwtPrivateKey;

    private JwtTokenHelper jwtTokenHelper;

    private KafkaMessageListenerContainer<String, Analytics> container;

    private final List<Analytics> records = new ArrayList<>();

    @BeforeEach
    public void setUp() throws InvalidKeySpecException, NoSuchAlgorithmException {
        records.clear();
        jwtTokenHelper = new JwtTokenHelper(jwtPrivateKey);
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
    public void itShouldAcceptValidToken() throws Exception {
        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        jwtTokenHelper.withIssueTime(ZonedDateTime.now());
        jwtTokenHelper.withExpirationDate(ZonedDateTime.now().plusMinutes(5));
        jwtTokenHelper.withJti(UUID.randomUUID().toString());
        final String authorizationHeader = jwtTokenHelper.generateAuthorizationHeader();

        mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isOk())
                .andExpect(content().string(is(emptyString())));

        await().atMost(QUEUE_READ_TIMEOUT, SECONDS).untilAsserted(() -> assertThat(records).hasSize(1));
    }


    @Test
    public void itShouldAcceptNonExpiredToken() throws Exception {
        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        jwtTokenHelper.withJti(UUID.randomUUID().toString());
        jwtTokenHelper.withIssueTime(ZonedDateTime.now().minusMinutes(10));
        jwtTokenHelper.withExpirationDate(ZonedDateTime.now().plusMinutes(2));
        final String authorizationHeader = jwtTokenHelper.generateAuthorizationHeader();


        mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isOk())
                .andExpect(content().string(is(emptyString())));

        await().atMost(QUEUE_READ_TIMEOUT, SECONDS).untilAsserted(() -> assertThat(records).hasSize(1));

    }


    private AnalyticsVo buildAnalyticsVo() {
        return AnalyticsVo.builder()
                .installationUuid("some installation uuid")
                .build();
    }

}
