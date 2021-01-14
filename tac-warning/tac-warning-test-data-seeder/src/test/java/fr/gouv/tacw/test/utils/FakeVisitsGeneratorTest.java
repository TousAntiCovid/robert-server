package fr.gouv.tacw.test.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

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
    
    @Test
    void testGeneratorCallNbVisitorsTimeWReport() throws Exception {
        new FakeVisitsGenerator(warningService).run(null);
        
        verify(warningService, times(FakeVisitsGenerator.NB_VISITORS)).reportVisitsWhenInfected(anyList());
    }

    @Test
    void testFakeVisitorGeneratesTheExpectecNumberOfVisitsPerDay() {
        List<VisitVo> visits = new FakeVisitor().generateVisits();
        
        assertThat(visits.size()).isEqualTo(FakeVisitsGenerator.RETENTION_DAYS * FakeVisitsGenerator.VISITS_PER_VISITOR_PER_DAY);
    }

}
