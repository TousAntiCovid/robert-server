package fr.gouv.stopc.submission.code.server.ws.service.generateservice;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.commun.service.impl.LongCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.commun.service.impl.ShortCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.service.impl.SubmissionCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.service.impl.GenerateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GenerateServiceGetValidityDateLongCodeMethodTest {

    @Mock
    private SubmissionCodeServiceImpl submissionCodeService;

    @Spy
    @InjectMocks
    private GenerateServiceImpl generateService;
    private static final String targetZoneId = "Europe/Paris";

    @BeforeEach
    public void init(){

        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(this.generateService, "targetZoneId", this.targetZoneId);
        ReflectionTestUtils.setField(this.generateService, "numberOfTryInCaseOfError", 0);

        //SET 24 hours of lock security
        ReflectionTestUtils.setField(this.submissionCodeService, "securityTimeBetweenTwoUsagesOfShortCode", 24);
        ReflectionTestUtils.setField(this.generateService, "longCodeService", new LongCodeServiceImpl());
        ReflectionTestUtils.setField(this.generateService, "shortCodeService", new ShortCodeServiceImpl());
    }

    @Test
    void testCheckValidUntilFormat() throws SubmissionCodeServerException {

        final long validityDays = 10;
        ReflectionTestUtils.setField(this.generateService, "timeValidityLongCode", validityDays);

        OffsetDateTime testedValidFrom = OffsetDateTime.now(ZoneId.of(this.targetZoneId));


        testedValidFrom = testedValidFrom.withMonth(01).withDayOfMonth(01).withHour(1).withMinute(12).truncatedTo(ChronoUnit.MINUTES);

        final SubmissionCodeDto submissionCodeDto = this.generateService.preGenerateSubmissionCodeDtoForCodeTypeAndDateValidity(CodeTypeEnum.LONG, testedValidFrom).build();

        assertNotNull(submissionCodeDto);




        final OffsetDateTime validUntil = submissionCodeDto.getDateEndValidity()
                .withOffsetSameInstant(
                        OffsetDateTime.now(ZoneId.of(this.targetZoneId)).getOffset()
                );

        final OffsetDateTime validFrom= submissionCodeDto.getDateAvailable()
                .withOffsetSameInstant(
                        OffsetDateTime.now(ZoneId.of(this.targetZoneId)).getOffset()
                );

        // asserting Hours is 23
        assertEquals(23, validUntil.getHour());

        // asserting Minutes is 59
        assertEquals(59, validUntil.getMinute());

        // asserting truncate to minutes
        assertEquals(00, validUntil.getSecond());


        final long betweenSec = SECONDS.between(validFrom, validUntil);
        final long betweenDays =  betweenSec / 60 / 60 / 24;

        final int deltaMinutes = testedValidFrom.getHour() * 60 + testedValidFrom.getMinute();

        assertEquals((((validityDays + 1 )* 24 * 60) - deltaMinutes- 1 ) * 60 , betweenSec);



    }

}
