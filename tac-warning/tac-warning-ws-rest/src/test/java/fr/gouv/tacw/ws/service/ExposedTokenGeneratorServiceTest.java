package fr.gouv.tacw.ws.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.service.impl.ExposedTokenGeneratorServiceImpl;
import fr.gouv.tacw.ws.service.impl.ScoringServiceImpl;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitVo;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ExposedTokenGeneratorServiceImpl.class, ScoringServiceImpl.class })
@EnableConfigurationProperties(value = TacWarningWsRestConfiguration.class)
@TestPropertySource("classpath:application.properties")
public class ExposedTokenGeneratorServiceTest {
    @Autowired
    private ExposedTokenGeneratorService exposedTokenGeneratorService;
    
    @Test
    public void testWhenCallingRegisterAllExposedStaticTokensWithDifferentVisitsThenReturnDifferentTokens() {
        VisitVo visit1 = new VisitVo("1000", 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID1"));
        VisitVo visit2 = new VisitVo("1000", 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID2"));
        
        List<ExposedStaticVisitEntity> visit1Tokens = exposedTokenGeneratorService.generateAllExposedTokens(visit1).collect(Collectors.toList());
        List<ExposedStaticVisitEntity> visit2Tokens = exposedTokenGeneratorService.generateAllExposedTokens(visit2).collect(Collectors.toList());
        
        assertThat(visit1Tokens).doesNotContainAnyElementsOf(visit2Tokens);
    }
}
