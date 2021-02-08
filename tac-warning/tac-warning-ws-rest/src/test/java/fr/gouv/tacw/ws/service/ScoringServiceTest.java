package fr.gouv.tacw.ws.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tacw.database.model.RiskLevel;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.service.impl.ScoringServiceImpl;
import fr.gouv.tacw.ws.vo.VenueTypeVo;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ScoringServiceImpl.class})
@EnableConfigurationProperties(value = TacWarningWsRestConfiguration.class)
@TestPropertySource("classpath:application.properties")
public class ScoringServiceTest {
    @Autowired
    private ScoringService scoringService;

    @Test
    public void testGetDefaultScoreWhenVenueTypeNotSpecified() {
        assertThat(scoringService.getScoreIncrement(VenueTypeVo.M)).isEqualTo(500);
    }

    @Test
    public void testCanGetScoreOfAGivenVenueTypeWhenSpecified() {
        assertThat(scoringService.getScoreIncrement(VenueTypeVo.N)).isEqualTo(1000);
    }

    @Test
    public void testWhenSpecificVenueTypeConfigurationSpecifiedButNoSpecificVenueTypeScoreThresholdThenGetDefaultScoreThreshold() {
        assertThat(scoringService.getScoreIncrement(VenueTypeVo.P)).isEqualTo(500);
    }

    @Test
    public void testCanScoreIsRoundedUpToTheNextInteger() {
        assertThat(scoringService.getScoreIncrement(VenueTypeVo.L)).isEqualTo(334);
    }
    
    @Test
    public void testCanGetRiskLevelWhenVenueTypeNotSpecified() {
        assertThat(scoringService.getVenueRiskLevel(VenueTypeVo.M)).isEqualTo(RiskLevel.LOW);
    }

    @Test
    public void testCanGetRiskLevelOfAGivenVenueTypeWhenSpecified() {
        assertThat(scoringService.getVenueRiskLevel(VenueTypeVo.L)).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    public void testWhenSpecificVenueTypeConfigurationSpecifiedButNoSpecificVenueTypeRiskLevelThenGetDefaultRiskLevel() {
        assertThat(scoringService.getVenueRiskLevel(VenueTypeVo.N)).isEqualTo(RiskLevel.LOW);
    }
}
