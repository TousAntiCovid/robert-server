package fr.gouv.stopc.submission.code.server.ws.service.generateservice;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.commun.service.impl.LongCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.database.entity.Lot;
import fr.gouv.stopc.submission.code.server.database.entity.SubmissionCode;
import fr.gouv.stopc.submission.code.server.database.service.impl.SubmissionCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.service.impl.GenerateServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
class GenerateServiceGenerateLongCodesMethodTest {
    @Mock
    private LongCodeServiceImpl longCodeService;

    @Mock
    private SubmissionCodeServiceImpl submissionCodeService;

    @Spy
    @InjectMocks
    private GenerateServiceImpl generateService;

    @BeforeEach
    public void init(){

        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(this.generateService, "targetZoneId", "Europe/Paris");
        ReflectionTestUtils.setField(this.generateService, "numberOfTryInCaseOfError", 0);

        //SET 24 hours of lock security
        ReflectionTestUtils.setField(this.submissionCodeService, "securityTimeBetweenTwoUsagesOfShortCode", 24);
    }
    /**
     * Calling generateCodeGeneric and assert that it returns the right size and the right elements
     */
    @Test
    void testSizeLongCodeGenerateResponseDtoList() throws SubmissionCodeServerException {
        int size = 12;

        Mockito.when(this.submissionCodeService.saveCode(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new SubmissionCode()));

        this.generateService.generateCodeGeneric(size, CodeTypeEnum.LONG, OffsetDateTime.now(), new Lot());

        verify(longCodeService, times(12)).generateCode();

    }

    @Test
    void testGenerateLongCode() {
        LongCodeServiceImpl longCodeService = new LongCodeServiceImpl();
        log.info("{}", longCodeService.generateCode());
    }

}
