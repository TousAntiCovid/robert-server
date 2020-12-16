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
    public void testCanReadScoringProperties() {
        assertThat(configuration.getExposureCountIncrements().size()).isEqualTo(20);
        assertThat(configuration.getExposureCountIncrements().get(VenueTypeVo.J.toString())).isEqualTo(10);
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
        assertThat(configuration.getScoreThreshold()).isEqualTo(10);
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
