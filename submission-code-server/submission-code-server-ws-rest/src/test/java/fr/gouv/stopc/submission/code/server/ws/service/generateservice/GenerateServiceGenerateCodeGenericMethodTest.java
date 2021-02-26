package fr.gouv.stopc.submission.code.server.ws.service.generateservice;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.commun.service.impl.LongCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.commun.service.impl.ShortCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.entity.Lot;
import fr.gouv.stopc.submission.code.server.database.entity.SubmissionCode;
import fr.gouv.stopc.submission.code.server.database.service.impl.SubmissionCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.CodeDetailedDto;
import fr.gouv.stopc.submission.code.server.ws.service.impl.GenerateServiceImpl;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;


class GenerateServiceGenerateCodeGenericMethodTest {

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
        ReflectionTestUtils.setField(this.generateService, "longCodeService", new LongCodeServiceImpl());
        ReflectionTestUtils.setField(this.generateService, "shortCodeService", new ShortCodeServiceImpl());
    }

    /**
     * List is returning desired number of codes
     */
    @Test
    void testSizeOfGenerateResponseDtoList()
            throws SubmissionCodeServerException
    {
        // asserting generateService is available
        final long size = Long.parseLong("10");
        final CodeTypeEnum cte = CodeTypeEnum.LONG;
        final OffsetDateTime validFrom = OffsetDateTime.now();
        final Lot lot = new Lot();

        Mockito.when(this.submissionCodeService.saveCode(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(new SubmissionCode()));

        final List<CodeDetailedDto> codeDetailedResponseDtoList = this.generateService.generateCodeGeneric(
                size, cte, validFrom, lot
        );
        //list should not be null
        assertNotNull(codeDetailedResponseDtoList);
        // list should be at size
        assertEquals(size, codeDetailedResponseDtoList.size());
    }

    /**
     * Check elements in list have each a code.
     */
    @Test
    void testCodeNotBlank()
            throws SubmissionCodeServerException
    {
        // asserting generateService is available
        final CodeTypeEnum cte = CodeTypeEnum.LONG;
        final OffsetDateTime validFrom = OffsetDateTime.now();


        final SubmissionCode submissionCode = new SubmissionCode();
        submissionCode.setCode("TOTO");

        Mockito.when(this.submissionCodeService.saveCode(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(submissionCode));

        final SubmissionCodeDto submissionCodeDto = this.generateService
                .preGenerateSubmissionCodeDtoForCodeTypeAndDateValidity(
                        cte, validFrom
                ).build();

        //list should not be null
        assertNotNull(submissionCodeDto);

        // asserting that generated is not blank
        final String code = submissionCodeDto.getCode();
        assertTrue(Strings.isNotBlank(code));


    }

    /**
     * Check elements in list have each a code.
     */
    @Test
    void testCodeWithLongCodePattern()
            throws SubmissionCodeServerException
    {
        // asserting generateService is available
        final CodeTypeEnum cte = CodeTypeEnum.LONG;
        final OffsetDateTime validFrom = OffsetDateTime.now();

        final SubmissionCodeDto submissionCodeDto = this.generateService
                .preGenerateSubmissionCodeDtoForCodeTypeAndDateValidity(
                        cte, validFrom
                ).build();

        //list should not be null
        assertNotNull(submissionCodeDto);

        Pattern p = Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");

        // asserting that code is formatted as UUIDv4 standard
        final String code = submissionCodeDto.getCode();
        Matcher m = p.matcher(code);
        assertTrue(m.matches());
    }

    /**
     * Check elements in list have each a code.
     */
    @Test
    void testCodeWithShortCodePattern()
            throws SubmissionCodeServerException
    {
        // asserting generateService is available
        final CodeTypeEnum cte = CodeTypeEnum.SHORT;
        final OffsetDateTime validFrom = OffsetDateTime.now();

        final SubmissionCodeDto submissionCodeDto = this.generateService
                .preGenerateSubmissionCodeDtoForCodeTypeAndDateValidity(
                        cte, validFrom
                ).build();


        //list should not be null
        assertNotNull(submissionCodeDto);

        Pattern p = Pattern.compile("([A-Z0-9]{6})");

        // asserting that code is formatted as UUIDv4 standard
        final String code = submissionCodeDto.getCode();
        Matcher m = p.matcher(code);
        assertTrue(m.matches());


    }

    /**
     * Check elements in list have the right code type
     */
    @Test
    void testCodeType()
            throws SubmissionCodeServerException
    {
        // asserting generateService is available
        final CodeTypeEnum cte = CodeTypeEnum.LONG;
        final OffsetDateTime validFrom = OffsetDateTime.now();

        final SubmissionCodeDto submissionCodeDto = this.generateService
                .preGenerateSubmissionCodeDtoForCodeTypeAndDateValidity(
                        cte, validFrom
                ).build();

        //list should not be null
        assertNotNull(submissionCodeDto);

        // assert the returning code corresponding to the given CodeTypeEnum in parameter
        assertEquals(CodeTypeEnum.LONG.getTypeCode(), submissionCodeDto.getType());
    }

    /**
     * Check elements in list have the right validUntil and validFrom format
     */
    @Test
    void testValidUntilAndValidFromFormat()
            throws SubmissionCodeServerException
    {
        // asserting generateService is available
        final long size = Long.parseLong("1");
        final CodeTypeEnum cte = CodeTypeEnum.LONG;
        final OffsetDateTime validFrom = OffsetDateTime.now();
        final Lot lot = new Lot();

        final SubmissionCode submissionCode = new SubmissionCode();
        submissionCode.setDateEndValidity(OffsetDateTime.now());
        submissionCode.setDateAvailable(OffsetDateTime.now());


        Mockito.when(this.submissionCodeService.saveCode(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(submissionCode));


        final List<CodeDetailedDto> codeDetailedResponseDtoList = this.generateService.generateCodeGeneric(
                size, cte, validFrom, lot
        );


        //list should not be null
        assertNotNull(codeDetailedResponseDtoList);
        // list should be at size
        assertEquals(size, codeDetailedResponseDtoList.size());

        Pattern p = Pattern.compile("^(-?(?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])" +
                "T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\\.[0-9]+)?" +
                "(Z|[+-](?:2[0-3]|[01][0-9]):[0-5][0-9])?$");

        codeDetailedResponseDtoList.forEach(grDto -> {

            // asserting that date is at the right format
            final String validFrom1 = grDto.getValidFrom();
            final String validUntil1 = grDto.getValidUntil();

            Matcher m = p.matcher(validFrom1);
            assertTrue(m.matches());

            m = p.matcher(validUntil1);
            assertTrue(m.matches());
        });
    }

    /**
     * Number of tries reach
     */
    @Test
    void testReachNumberOfTries()
    {
        // asserting generateService is available
        final long size = Long.parseLong("10");
        final CodeTypeEnum cte = CodeTypeEnum.LONG;
        final OffsetDateTime validFrom = OffsetDateTime.now();
        final Lot lot = new Lot();

        Mockito.when(submissionCodeService.saveCode(Mockito.any(SubmissionCodeDto.class), Mockito.any(Lot.class)))
                .thenThrow(DataIntegrityViolationException.class);


        ReflectionTestUtils.setField(generateService, "targetZoneId", "Europe/Paris");
        ReflectionTestUtils.setField(generateService, "numberOfTryInCaseOfError", 0);


        assertThrows(
                SubmissionCodeServerException.class,
                () -> this.generateService.generateCodeGeneric(
                        size, cte, validFrom, lot
                ),
                "Expected generateCodeGeneric() to throw, but it didn't"
        );
    }

    /**
     * Number of tries reach
     */
    @Test
    void testReachNumberOfTriesWithoutLotParameter()
    {
        // asserting generateService is available
        final long size = Long.parseLong("10");
        final CodeTypeEnum cte = CodeTypeEnum.LONG;
        final OffsetDateTime validFrom = OffsetDateTime.now();
        final Lot lot = new Lot();

        Mockito.when(submissionCodeService.saveCode(Mockito.any(SubmissionCodeDto.class), Mockito.any(Lot.class)))
                .thenThrow(DataIntegrityViolationException.class);


        ReflectionTestUtils.setField(this.generateService, "targetZoneId", "Europe/Paris");
        ReflectionTestUtils.setField(this.generateService, "numberOfTryInCaseOfError", 0);

        assertThrows(
                SubmissionCodeServerException.class,
                () -> this.generateService.generateCodeGeneric(
                        size, cte, validFrom, new Lot()
                ),
                "Expected generateCodeGeneric() to throw, but it didn't"
        );
    }

}