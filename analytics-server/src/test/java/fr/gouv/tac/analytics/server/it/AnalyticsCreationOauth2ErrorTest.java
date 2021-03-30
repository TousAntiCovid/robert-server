package fr.gouv.tac.analytics.server.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator.JtiCanOnlyBeUsedOnceOAuth2TokenValidator;
import fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator.JtiPresenceOAuth2TokenValidator;
import fr.gouv.tac.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tac.analytics.server.controller.vo.ErrorVo;
import fr.gouv.tac.analytics.server.model.kafka.Analytics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ActiveProfiles(value = "test")
@SpringBootTest(classes = AnalyticsServerApplication.class)
@AutoConfigureMockMvc
public class AnalyticsCreationOauth2ErrorTest {

    @MockBean
    private KafkaTemplate<String, Analytics> kafkaTemplate;

    @Mock
    private ListenableFuture<SendResult<String, Analytics>> listenableFutureMock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${analyticsserver.controller.analytics.path}")
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

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
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

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));

    }

    @Test
    public void itShouldRejectTokenWithAnAlreadyUsedJTI() throws Exception {
        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final String jti = UUID.randomUUID().toString();
        jwtTokenHelper.withJti(jti);
        jwtTokenHelper.withIssueTime(ZonedDateTime.now());
        jwtTokenHelper.withExpirationDate(ZonedDateTime.now().plusMinutes(10));
        final String authorizationHeader1 = jwtTokenHelper.generateAuthorizationHeader();

        when(kafkaTemplate.sendDefault(any(Analytics.class))).thenReturn(listenableFutureMock);

        mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isOk())
                .andExpect(content().string(is(emptyString())));

        verify(kafkaTemplate, times(1)).sendDefault(any(Analytics.class));

        jwtTokenHelper.withIssueTime(ZonedDateTime.now());
        final String authorizationHeader2 = jwtTokenHelper.generateAuthorizationHeader();


        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isUnauthorized())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        assertThat(errorVo.getMessage()).contains(JtiCanOnlyBeUsedOnceOAuth2TokenValidator.ERR_MESSAGE);
        assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());


        verify(kafkaTemplate, times(1)).sendDefault(any(Analytics.class));

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

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
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

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    private AnalyticsVo buildAnalyticsVo() {
        return AnalyticsVo.builder()
                .installationUuid("some installation uuid")
                .build();
    }

}
