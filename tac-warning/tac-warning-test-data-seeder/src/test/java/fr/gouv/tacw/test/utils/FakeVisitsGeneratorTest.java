package fr.gouv.tacw.test.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.vo.VisitVo;

@ExtendWith(SpringExtension.class)
class FakeVisitsGeneratorTest {
    @MockBean
    protected WarningService warningService;
    
    @MockBean
    protected FakeVisitsGeneratorConfiguration configuration;
    
    @BeforeEach
    public void setUp() {
        when(configuration.getNbVenues()).thenReturn(100);
    }
    
    @Test
    public void testGeneratorCallNbVisitorsTimeWReport() throws Exception {
        int nbVisits = 200;
        when(configuration.getNbVisitors()).thenReturn(nbVisits);
        
        new FakeVisitsGenerator(configuration, warningService).run(null);
        
        verify(warningService, times(nbVisits)).reportVisitsWhenInfected(anyList());
    }

    @Test
    public void testFakeVisitorGeneratesTheExpectecNumberOfVisitsPerDay() {
        int nbRetentionDays = 14;
        int nbVisitsPerVistorPerDay = 20;
        List<UUID> venues = Stream.generate(UUID::randomUUID).limit(configuration.getNbVenues()).collect(Collectors.toList());
        when(configuration.getRetentionDays()).thenReturn(nbRetentionDays);
        when(configuration.getNbVisitsPerVisitorPerDay()).thenReturn(nbVisitsPerVistorPerDay);
        when(configuration.getNbHangouts()).thenReturn(10);

        List<VisitVo> visits = new FakeVisitor(configuration, venues).generateVisits();
        
        assertThat(visits.size()).isEqualTo(nbRetentionDays * nbVisitsPerVistorPerDay);
    }

}
