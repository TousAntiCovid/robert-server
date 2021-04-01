package fr.gouv.tac.analytics.server.it;

import static fr.gouv.tac.analytics.server.config.validation.validator.AnalyticsVoInfoSizeValidator.*;
import static fr.gouv.tac.analytics.server.config.validation.validator.TimestampedEventCollectionValidator.DESCRIPTION_TOO_LONG_ERROR_MESSAGE;
import static fr.gouv.tac.analytics.server.config.validation.validator.TimestampedEventCollectionValidator.NAME_TOO_LONG_ERROR_MESSAGE;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.concurrent.ListenableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.tac.analytics.server.AnalyticsServerApplication;
import fr.gouv.tac.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tac.analytics.server.controller.vo.ErrorVo;
import fr.gouv.tac.analytics.server.controller.vo.TimestampedEventVo;
import fr.gouv.tac.analytics.server.model.kafka.Analytics;
import fr.gouv.tac.analytics.server.utils.UriConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ActiveProfiles(value = "test")
@SpringBootTest(classes = AnalyticsServerApplication.class)
@AutoConfigureMockMvc
public class AnalyticsCreationValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${analyticsserver.controller.path.prefix}"+ UriConstants.API_V1 + UriConstants.ANALYTICS)
    private String analyticsControllerPath;

    @MockBean
    private KafkaTemplate<String, Analytics> kafkaTemplate;

    @Mock
    private ListenableFuture<SendResult<String, Analytics>> listenableFutureMock;

    /****************
     * ROOT
     ******/

    @Test
    @WithMockUser
    public void itShouldAcceptValidAnalytics() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        when(kafkaTemplate.sendDefault(any(Analytics.class))).thenReturn(listenableFutureMock);

        mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isOk())
                .andExpect(content().string(is(emptyString())));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithoutInstallationUuid() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.setInstallationUuid(null);

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains("'analyticsVo' on field 'installationUuid': rejected value [null]");
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithEmptyInstallationUuid() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.setInstallationUuid("");

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains("'analyticsVo' on field 'installationUuid': rejected value []");
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithTooLongInstallationUuid() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.setInstallationUuid(RandomStringUtils.random(65));

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains("Field error in object 'analyticsVo' on field 'installationUuid': rejected value");
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    /****************
     * INFO
     ******/

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithTooManyInfo() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.setInfos(Map.of("info1", "info1Value", "info2", "info2value", "info3", "info3value"));

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains(String.format(TOO_MANY_INFO_ERROR_MESSAGE, 3, 2));
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithInfoKeyTooLong() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.setInfos(Map.of("abcdefghijkl", "info1Value"));

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains(String.format(KEY_TOO_LONG_ERROR_MESSAGE, 10, 12));
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithInfoValueTooLong() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.setInfos(Map.of("info1", "info1ValueTooLong"));

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains(String.format(VALUE_TOO_LONG_ERROR_MESSAGE, 12, 17));
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    /****************
     * EVENT
     ******/

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithEmptyEventName() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.getEvents().get(0).setName("");

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains("Field error in object 'analyticsVo' on field 'events[0].name': rejected value []");
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithEventNameTooLong() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.getEvents().get(0).setName("Even name too long");

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains(String.format(NAME_TOO_LONG_ERROR_MESSAGE, "EVENT", 10, 18));
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithoutEventTimeStamp() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.getEvents().get(0).setTimestamp(null);

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains("Field error in object 'analyticsVo' on field 'events[0].timestamp': rejected value [null]");
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithEvenDescriptionTooLong() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.getEvents().get(0).setDescription("Event description too long");

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains(String.format(DESCRIPTION_TOO_LONG_ERROR_MESSAGE, "EVENT", 20, 26));
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    /****************
     * ERROR
     ******/


    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithEmptyErrorName() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.getErrors().get(0).setName("");

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains("Field error in object 'analyticsVo' on field 'errors[0].name': rejected value []");
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithErrorNameTooLong() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.getErrors().get(0).setName("Error name too long");

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains(String.format(NAME_TOO_LONG_ERROR_MESSAGE, "ERROR", 10, 19));
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithoutErrorTimeStamp() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.getErrors().get(0).setTimestamp(null);

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains("Field error in object 'analyticsVo' on field 'errors[0].timestamp': rejected value [null]");
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    @Test
    @WithMockUser
    public void itShouldRejectAnalyticsWithErrorDescriptionTooLong() throws Exception {

        final AnalyticsVo analyticsVo = buildAnalyticsVo();
        analyticsVo.getErrors().get(0).setDescription("Error description too long");

        final String analyticsAsJson = objectMapper.writeValueAsString(analyticsVo);

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(analyticsControllerPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyticsAsJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        final ErrorVo errorVo = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorVo.class);
        Assertions.assertThat(errorVo.getMessage()).contains(String.format(DESCRIPTION_TOO_LONG_ERROR_MESSAGE, "ERROR", 20, 26));
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    private AnalyticsVo buildAnalyticsVo() {
        final Map<String, String> infos = Map.of("info1", "info1Value", "info2", "info2value");

        final ZonedDateTime timestamp = ZonedDateTime.parse("2020-12-17T10:59:17.123Z");

        final TimestampedEventVo event1 = TimestampedEventVo.builder().name("eventName1").timestamp(timestamp).description("event1 description").build();
        final TimestampedEventVo event2 = TimestampedEventVo.builder().name("eventName2").timestamp(timestamp).build();

        final TimestampedEventVo error1 = TimestampedEventVo.builder().name("errorName1").timestamp(timestamp).build();
        final TimestampedEventVo error2 = TimestampedEventVo.builder().name("errorName2").timestamp(timestamp).description("error2 description").build();

        return AnalyticsVo.builder()
                .installationUuid("some installation uuid")
                .infos(infos)
                .events(Arrays.asList(event1, event2))
                .errors(Arrays.asList(error1, error2))
                .build();
    }


}

