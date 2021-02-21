package fr.gouv.stopc.submission.code.server.ws.service.verifyservice;


import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.service.impl.SubmissionCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.service.impl.VerifyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class VerifyServiceVerifyCodeMethodTest {

    private static final String FALSE_CODE = "FALSE_CODE";

    @Mock
    SubmissionCodeServiceImpl submissionCodeService;

    @Spy
    @InjectMocks
    private VerifyServiceImpl verifyService;

    @BeforeEach
    public void init(){
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Code does not exists
     */
    @Test
    void testCodeNotExist() throws SubmissionCodeServerException {

        when(this.submissionCodeService.getCodeValidity(FALSE_CODE, CodeTypeEnum.SHORT))
                .thenReturn(Optional.empty());

        final boolean isPresent = this.verifyService.verifyCode(FALSE_CODE, CodeTypeEnum.SHORT.getType());
        assertFalse(isPresent);

    }

    /**
     * Code exists for given CodeType
     */
    @Test
    void testCodeExistForGivenCodeType() throws SubmissionCodeServerException {

        when(this.submissionCodeService.getCodeValidity(FALSE_CODE, CodeTypeEnum.SHORT))
                .thenReturn(Optional.of(
                        SubmissionCodeDto.builder()
                                .code(FALSE_CODE)
                                .type(CodeTypeEnum.SHORT.getTypeCode())
                                .used(false)
                                .dateAvailable(OffsetDateTime.now().minusDays(11))
                                .dateEndValidity(OffsetDateTime.now().plusDays(12))
                                .build()
                ));

        when(this.submissionCodeService.updateCodeUsed(any()))
                .thenReturn(true);

        final boolean isPresent = this.verifyService.verifyCode(FALSE_CODE, CodeTypeEnum.SHORT.getTypeCode());
        assertTrue(isPresent);
    }

    /**
     * Code exists but not for given CodeType
     */
    @Test
    void testCodeNotExistForGivenCodeType() throws SubmissionCodeServerException {

        when(this.submissionCodeService.getCodeValidity(FALSE_CODE, CodeTypeEnum.SHORT))
                .thenReturn(Optional.empty());


        final boolean isPresent = this.verifyService.verifyCode(FALSE_CODE, CodeTypeEnum.LONG.getTypeCode());
        assertFalse(isPresent);
    }

    /**
     * Code was already verified
     */
    @Test
    void testCodeAlreadyVerify() throws SubmissionCodeServerException {

         when(this.submissionCodeService.getCodeValidity(FALSE_CODE, CodeTypeEnum.SHORT))
                .thenReturn(Optional.of(
                        SubmissionCodeDto.builder()
                                .code(FALSE_CODE)
                                .type(CodeTypeEnum.SHORT.getTypeCode())
                                .used(true)
                                .dateAvailable(OffsetDateTime.now().minusDays(11))
                                .dateEndValidity(OffsetDateTime.now().plusDays(12))
                                .build()
                ));

        final boolean isPresent2 = this.verifyService.verifyCode(FALSE_CODE, CodeTypeEnum.SHORT.getTypeCode());
        assertFalse(isPresent2);
    }

    /**
     * Code has expired
     */
    @Test
    void testExpiredCode() throws SubmissionCodeServerException {

        when(this.submissionCodeService.getCodeValidity(FALSE_CODE, CodeTypeEnum.SHORT))
                .thenReturn(Optional.of(
                        SubmissionCodeDto.builder()
                                .code(FALSE_CODE)
                                .type(CodeTypeEnum.SHORT.getTypeCode())
                                .used(false)
                                .dateAvailable(OffsetDateTime.now().minusDays(11))
                                .dateEndValidity(OffsetDateTime.now().minusDays(10))
                                .build()
                ));


        final boolean isPresent = this.verifyService.verifyCode(FALSE_CODE, CodeTypeEnum.SHORT.getTypeCode());
        assertFalse(isPresent);
    }
}
