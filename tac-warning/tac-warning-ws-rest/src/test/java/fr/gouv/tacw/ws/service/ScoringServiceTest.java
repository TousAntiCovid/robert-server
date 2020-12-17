package fr.gouv.tacw.ws.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tacw.service.ScoringService;
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
    public void testGetDefaultScoreWhenVenuTypeNotSpecified() {
        assertThat(scoringService.getScoreIncrement(VenueTypeVo.M)).isEqualTo(50);
    }
    
    @Test
    public void testCanGetScoreOfAGivenVenueTypeWhenSpecified() {
        assertThat(scoringService.getScoreIncrement(VenueTypeVo.N)).isEqualTo(100);
    }
}
