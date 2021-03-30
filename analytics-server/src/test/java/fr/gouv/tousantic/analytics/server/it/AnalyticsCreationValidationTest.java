package fr.gouv.tousantic.analytics.server.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.tousantic.analytics.server.AnalyticsServerApplication;
import fr.gouv.tousantic.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tousantic.analytics.server.controller.vo.ErrorVo;
import fr.gouv.tousantic.analytics.server.controller.vo.TimestampedEventVo;
import fr.gouv.tousantic.analytics.server.model.kafka.Analytics;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles(value = "test")
@SpringBootTest(classes = AnalyticsServerApplication.class)
@AutoConfigureMockMvc
public class AnalyticsCreationValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${analyticsserver.controller.analytics.path}")
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
        Assertions.assertThat(errorVo.getMessage()).contains("Too many info, 3 found, whereas the maximum allowed is 2");
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
        Assertions.assertThat(errorVo.getMessage()).contains("Key with more than 10 characters is not allowed, found 12 characters");
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
        Assertions.assertThat(errorVo.getMessage()).contains("Parameter value with more than 12 characters is not allowed, found 17 characters");
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
        Assertions.assertThat(errorVo.getMessage()).contains("For EVENT, name with more than 10 characters is not allowed, found 18 characters");
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
        Assertions.assertThat(errorVo.getMessage()).contains("For EVENT, description with more than 20 characters is not allowed, found 26 characters");
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
        Assertions.assertThat(errorVo.getMessage()).contains("For ERROR, name with more than 10 characters is not allowed, found 19 characters");
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
        Assertions.assertThat(errorVo.getMessage()).contains("For ERROR, description with more than 20 characters is not allowed, found 26 characters");
        Assertions.assertThat(errorVo.getTimestamp()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        verify(kafkaTemplate, never()).sendDefault(any(Analytics.class));
    }

    private AnalyticsVo buildAnalyticsVo() {
        final Map<String, String> infos = Map.of("info1", "info1Value", "info2", "info2value");

        final ZonedDateTime timestamp = ZonedDateTime.parse("2020-12-17T10:59:17.123Z");

        final TimestampedEventVo event1 = TimestampedEventVo.builder().name("eventName1").timestamp(timestamp).description("event1 description").build();
        final TimestampedEventVo event2 = TimestampedEventVo.builder().name("eventName2").timestamp(timestamp).build();

        final TimestampedEventVo error1 = TimestampedEventVo.builder().name("errorName1").timestamp(timestamp).build();
        final TimestampedEventVo error2 = TimestampedEventVo.builder().name("errorName2").timestamp(timestamp).description("error2 descirption").build();

        return AnalyticsVo.builder()
                .installationUuid("some installation uuid")
                .infos(infos)
                .events(Arrays.asList(event1, event2))
                .errors(Arrays.asList(error1, error2))
                .build();
    }


}

