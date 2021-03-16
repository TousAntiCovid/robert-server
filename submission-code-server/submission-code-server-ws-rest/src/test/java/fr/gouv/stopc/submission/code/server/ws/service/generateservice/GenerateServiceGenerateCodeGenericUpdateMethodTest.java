package fr.gouv.stopc.submission.code.server.ws.service.generateservice;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.commun.service.IShortCodeService;
import fr.gouv.stopc.submission.code.server.database.entity.SubmissionCode;
import fr.gouv.stopc.submission.code.server.database.repository.SubmissionCodeRepository;
import fr.gouv.stopc.submission.code.server.database.service.impl.SubmissionCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.CodeDetailedDto;
import fr.gouv.stopc.submission.code.server.ws.service.impl.GenerateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenerateServiceGenerateCodeGenericUpdateMethodTest {

    @Mock
    private IShortCodeService shortCodeService;

    @Mock
    private SubmissionCodeRepository submissionCodeRepository;

    @Spy
    @InjectMocks
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
        ReflectionTestUtils.setField(this.generateService, "submissionCodeService", this.submissionCodeService);
    }


    /**
     * Simulate a same code insertion when validity date is not compliant with
     */
    @Test
    void testSameShortCodeAndSecurityDelayNotRespected() {
        // asserting gsi is available
        final long size = Long.parseLong("1");
        final CodeTypeEnum cte = CodeTypeEnum.SHORT;
        final OffsetDateTime validFrom = OffsetDateTime.now();

        Mockito.when(shortCodeService.generateCode())
                .thenReturn("5d98e3");

        final SubmissionCode submissionCode = new SubmissionCode();
        submissionCode.setId(1);
        submissionCode.setCode("5d98e3");

        Mockito.when(this.submissionCodeRepository.save(Mockito.any()))
                .thenThrow(DataIntegrityViolationException.class)
                .thenReturn(null);

        Mockito.when(this.submissionCodeRepository.findByCodeAndTypeAndAndDateEndValidityLessThan(
                "5d98e3", cte.getTypeCode(), validFrom.minusHours(24)
        )).thenReturn(null);

        assertThrows(
                SubmissionCodeServerException.class,
                () -> this.generateService.generateCodeGeneric(
                        size, cte, validFrom, null
                ),
                "Expected generateCodeGeneric() to throw, but it didn't"
        );

    }


    /**
     * Number of tries reach
     */
    @Test
    void testSameShortCodeAndSecurityDelayIsRespected() throws SubmissionCodeServerException {
        // asserting gsi is available
        final long size = Long.parseLong("1");
        final CodeTypeEnum cte = CodeTypeEnum.SHORT;
        final OffsetDateTime validFrom = OffsetDateTime.now();

        Mockito.when(shortCodeService.generateCode())
                .thenReturn("5d98e3");

        final SubmissionCode submissionCode = new SubmissionCode();
        submissionCode.setId(1);
        submissionCode.setCode("5d98e3");

        Mockito.when(this.submissionCodeRepository.save(Mockito.any()))
                .thenThrow(DataIntegrityViolationException.class)
                .thenReturn(submissionCode);

        Mockito.when(this.submissionCodeRepository.findByCodeAndTypeAndAndDateEndValidityLessThan(
                "5d98e3", cte.getTypeCode(), validFrom.minusHours(24)
        )).thenReturn(submissionCode);





        // try once
        final List<CodeDetailedDto> codeDetailedResponseDtoListFirst = this.generateService.generateCodeGeneric(
                size, cte, validFrom, null
        );


        assertEquals(
                codeDetailedResponseDtoListFirst.size(),
                size
        );


    }


}