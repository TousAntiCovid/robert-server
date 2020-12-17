package fr.gouv.tacw.ws.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tacw.ws.vo.VenueTypeVo;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.properties")
@EnableConfigurationProperties(value = TacWarningWsRestConfiguration.class)
public class TacWarningWsRestConfigurationTest {

    @Autowired
    TacWarningWsRestConfiguration configuration;

    @Test
    public void testCanReadVenueTypePeopleThreshold() {
        assertThat(configuration.getVenueTypePositiveCasesThreshold().size()).isEqualTo(3);
    }

    @Test
    public void testCanReadDefaultVenueTypePeopleThreshold() {
        assertThat(configuration.getVenueTypePositiveCasesThreshold().get("default")).isEqualTo(2);
    }

    @Test
    public void testCanReadSpecificVenueTypePeopleThreshold() {
        assertThat(configuration.getVenueTypePositiveCasesThreshold().get(VenueTypeVo.N.toString())).isEqualTo(1);
    }

    @Test
    public void testCanReadMaxSalt() {
        assertThat(configuration.getMaxSalt()).isEqualTo(1000);
    }

    @Test
    public void testCanReadMaxVisits() {
        assertThat(configuration.getMaxVisits()).isEqualTo(20);
    }

    @Test
    public void testCanReadStartDelta() {
        assertThat(configuration.getStartDelta()).isEqualTo(7200);
    }

    @Test
    public void testCanReadEndDelta() {
        assertThat(configuration.getEndDelta()).isEqualTo(7200);
    }

    @Test
    public void testCanReadDefaultScoreThreshold() {
        assertThat(configuration.getScoreThreshold()).isEqualTo(1000);
    }

    @Test
    public void testCanReadJwtPublicKey() {
        assertThat(configuration.getRobertJwtPublicKey()).isEmpty();
    }

    @Test
    public void testCanReadJwtReportAuthorizationDisabled() {
        assertThat(configuration.isJwtReportAuthorizationDisabled()).isTrue();
    }
}
