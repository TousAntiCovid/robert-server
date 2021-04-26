package fr.gouv.tac.analytics.server.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.concurrent.ListenableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.tac.analytics.server.AnalyticsServerApplication;
import fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator.ExpirationTokenPresenceOAuth2TokenValidator;
import fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator.JtiPresenceOAuth2TokenValidator;
import fr.gouv.tac.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tac.analytics.server.controller.vo.ErrorVo;
import fr.gouv.tac.analytics.server.utils.UriConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ActiveProfiles(value = "test")
@SpringBootTest(classes = AnalyticsServerApplication.class)
@AutoConfigureMockMvc
public class AnalyticsCreationOauth2ErrorTest {

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ListenableFuture<SendResult<String, String>> listenableFutureMock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${analyticsserver.controller.path.prefix}"+ UriConstants.API_V1 + UriConstants.ANALYTICS)
    private String analyticsControllerPath;

    @Value("${analyticsserver.robert_jwt_analyticsprivatekey}")
    private String jwtPrivateKey;

    private JwtTokenHelper jwtTokenHelper;

    @BeforeEach
    public void setUp() throws InvalidKeySpecException, NoSuchAlgorithmException {
        jwtTokenHelper = new JwtTokenHelper(jwtPrivateKey);
    }


    @Test
    public void itShouldRejectWhenThereIsNoAuthenticationHeader() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isUnauthorized())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        assertThat(errorVo.getMessage()).isEqualTo("Full authentication is required to access this resource");
        assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(String.class));
    }

    @Test
    public void itShouldRejectTokenWithoutJTI() throws Exception {
        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        jwtTokenHelper.withIssueTime(ZonedDateTime.now());
        jwtTokenHelper.withExpirationDate(ZonedDateTime.now().plusMinutes(5));
        final String authorizationHeader = jwtTokenHelper.generateAuthorizationHeader();

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isUnauthorized())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        assertThat(errorVo.getMessage()).contains(JtiPresenceOAuth2TokenValidator.ERR_MESSAGE);
        assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(String.class));

    }

   @Test
    public void itShouldRejectTokenWithoutTokenExpiration() throws Exception {
        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        jwtTokenHelper.withJti(UUID.randomUUID().toString());
        jwtTokenHelper.withIssueTime(ZonedDateTime.now().minusMinutes(10));

        final String authorizationHeader = jwtTokenHelper.generateAuthorizationHeader();


        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isUnauthorized())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        assertThat(errorVo.getMessage()).contains(ExpirationTokenPresenceOAuth2TokenValidator.ERR_MESSAGE);
        assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(String.class));
    }


    @Test
    public void itShouldRejectExpiredToken() throws Exception {
        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        jwtTokenHelper.withJti(UUID.randomUUID().toString());
        jwtTokenHelper.withIssueTime(ZonedDateTime.now().minusMinutes(10));
        jwtTokenHelper.withExpirationDate(ZonedDateTime.now().minusMinutes(2));
        final String authorizationHeader = jwtTokenHelper.generateAuthorizationHeader();


        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isUnauthorized())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        assertThat(errorVo.getMessage()).startsWith("An error occurred while attempting to decode the Jwt: Jwt expired at");
        assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(String.class));
    }

    private AnalyticsVo buildAnalyticsVo() {
        return AnalyticsVo.builder()
                .installationUuid("some installation uuid")
                .build();
    }

}
