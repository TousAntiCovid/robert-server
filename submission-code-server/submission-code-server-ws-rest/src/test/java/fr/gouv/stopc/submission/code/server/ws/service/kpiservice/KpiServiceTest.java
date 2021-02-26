package fr.gouv.stopc.submission.code.server.ws.service.kpiservice;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.database.repository.SequenceFichierRepository;
import fr.gouv.stopc.submission.code.server.database.repository.SubmissionCodeRepository;
import fr.gouv.stopc.submission.code.server.ws.SubmissionCodeServerClientApiWsRestApplication;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.service.impl.KpiServiceImpl;
import fr.gouv.stopc.submission.code.server.ws.utils.FormatDatesKPI;
import fr.gouv.stopc.submission.code.server.ws.vo.SubmissionCodeServerKpi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test class for the Kpi generation service
 * 
 * @author plant-stopcovid
 *
 */
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@SpringBootTest(classes = {
        SubmissionCodeServerClientApiWsRestApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application.properties")
public class KpiServiceTest {

    private static final String SHOULD_NOT_FAIL_MESSAGE = "This should not fail.";

    /**
     * Zone ID to use
     */
    @Value("${stop.covid.qr.code.targetzone}")
    private String targetZoneId;

    @MockBean
    private SubmissionCodeRepository submissionCodeRepositoryMock;

    @MockBean
    private SequenceFichierRepository sequenceFichierRepositoryMock;

    /**
     * The service to test
     */
    @Autowired
    private KpiServiceImpl kpiService;

    private LocalDate startDate;

    private LocalDate endDate;

    private OffsetDateTime startDateTime;

    private OffsetDateTime endDateTime;

    @BeforeEach
    public void init() {
        this.startDate = LocalDate.now();
        this.endDate = this.startDate.plusDays(1);

        this.startDateTime = FormatDatesKPI.normaliseDateFrom(this.startDate, this.targetZoneId);
        this.endDateTime = FormatDatesKPI.normaliseDateTo(this.startDate,this.targetZoneId);
    }

    @Test
    public void getCodeLongUsedForDay() {
        LocalDate fromDate = OffsetDateTime.now().toLocalDate();
        LocalDate toDate = OffsetDateTime.now().toLocalDate();

        when(this.submissionCodeRepositoryMock.countSubmissionCodeUsedByDate(
                FormatDatesKPI.normaliseDateFrom(fromDate, this.targetZoneId),
                FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "1")).thenReturn(2L);
        when(this.submissionCodeRepositoryMock.countSubmissionCodeUsedByDate(
                FormatDatesKPI.normaliseDateFrom(fromDate, this.targetZoneId),
                FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "2")).thenReturn(0L);
        when(this.submissionCodeRepositoryMock
                .countSubmissionCodeExpiredDate(FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "1"))
                        .thenReturn(0L);
        when(this.submissionCodeRepositoryMock
                .countSubmissionCodeExpiredDate(FormatDatesKPI.normaliseDateTo(toDate, this.targetZoneId), "2"))
                        .thenReturn(0L);
        List<SubmissionCodeServerKpi> result = new ArrayList<>();
        try {
            result = this.kpiService.generateKPI(fromDate, toDate);
        } catch (SubmissionCodeServerException s) {
            fail(SHOULD_NOT_FAIL_MESSAGE);
        }
        assertTrue(result.get(0).getNbLongCodesUsed() != 0);
        assertTrue(result.size() == 1);
    }

    @Test
    public void getCodeShortUsedForDay() {
        LocalDate fromDate = OffsetDateTime.now().toLocalDate();
        LocalDate toDate = OffsetDateTime.now().toLocalDate();

        when(this.submissionCodeRepositoryMock.countSubmissionCodeUsedByDate(
                FormatDatesKPI.normaliseDateFrom(fromDate, this.targetZoneId),
                FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "1")).thenReturn(0L);
        when(this.submissionCodeRepositoryMock.countSubmissionCodeUsedByDate(
                FormatDatesKPI.normaliseDateFrom(fromDate, this.targetZoneId),
                FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "2")).thenReturn(2L);
        when(this.submissionCodeRepositoryMock
                .countSubmissionCodeExpiredDate(FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "1"))
                        .thenReturn(0L);
        when(this.submissionCodeRepositoryMock
                .countSubmissionCodeExpiredDate(FormatDatesKPI.normaliseDateTo(toDate, this.targetZoneId), "2"))
                        .thenReturn(0L);
        List<SubmissionCodeServerKpi> result = new ArrayList<>();
        try {
            result = this.kpiService.generateKPI(fromDate, toDate);
        } catch (SubmissionCodeServerException s) {
            fail(SHOULD_NOT_FAIL_MESSAGE);
        }
        assertTrue(result.get(0).getNbShortCodesUsed() != 0);
        assertTrue(result.size() == 1);
    }

    @Test
    public void getCodeShortExpiredForDay() {
        LocalDate fromDate = OffsetDateTime.now().toLocalDate();
        LocalDate toDate = OffsetDateTime.now().toLocalDate();

        when(this.submissionCodeRepositoryMock.countSubmissionCodeUsedByDate(
                FormatDatesKPI.normaliseDateFrom(fromDate, this.targetZoneId),
                FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "1")).thenReturn(0L);
        when(this.submissionCodeRepositoryMock.countSubmissionCodeUsedByDate(
                FormatDatesKPI.normaliseDateFrom(fromDate, this.targetZoneId),
                FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "2")).thenReturn(0L);
        when(this.submissionCodeRepositoryMock
                .countSubmissionCodeExpiredDate(FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "1"))
                        .thenReturn(0L);
        when(this.submissionCodeRepositoryMock
                .countSubmissionCodeExpiredDate(FormatDatesKPI.normaliseDateTo(toDate, this.targetZoneId), "2"))
                        .thenReturn(1L);
        List<SubmissionCodeServerKpi> result = new ArrayList<>();
        try {
            result = this.kpiService.generateKPI(fromDate, toDate);
        } catch (SubmissionCodeServerException s) {
            fail(SHOULD_NOT_FAIL_MESSAGE);
        }
        assertTrue(result.get(0).getNbShortExpiredCodes() != 0);
        assertTrue(result.size() == 1);

    }

    @Test
    public void getCodeLongExpiredForDay() {
        LocalDate fromDate = OffsetDateTime.now().toLocalDate();
        LocalDate toDate = OffsetDateTime.now().toLocalDate();

        when(this.submissionCodeRepositoryMock.countSubmissionCodeUsedByDate(
                FormatDatesKPI.normaliseDateFrom(fromDate, this.targetZoneId),
                FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "1")).thenReturn(0L);
        when(this.submissionCodeRepositoryMock.countSubmissionCodeUsedByDate(
                FormatDatesKPI.normaliseDateFrom(fromDate, this.targetZoneId),
                FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "2")).thenReturn(0L);
        when(this.submissionCodeRepositoryMock
                .countSubmissionCodeExpiredDate(FormatDatesKPI.normaliseDateTo(fromDate, this.targetZoneId), "1"))
                        .thenReturn(1L);
        when(this.submissionCodeRepositoryMock
                .countSubmissionCodeExpiredDate(FormatDatesKPI.normaliseDateTo(toDate, this.targetZoneId), "2"))
                        .thenReturn(0L);
        List<SubmissionCodeServerKpi> result = new ArrayList<>();
        try {
            result = this.kpiService.generateKPI(fromDate, toDate);
        } catch (SubmissionCodeServerException s) {
            fail(SHOULD_NOT_FAIL_MESSAGE);
        }
        assertTrue(result.get(0).getNbLongExpiredCodes() != 0);
        assertTrue(result.size() == 1);

    }

    @Test
    public void validationDate() {
        LocalDate fromDate = OffsetDateTime.now().toLocalDate();
        LocalDate toDate = OffsetDateTime.now().toLocalDate().minusDays(1L);
        assertThrows(SubmissionCodeServerException.class, () -> {
            this.kpiService.generateKPI(fromDate, toDate);
        });
    }

    /**
     * Check if the service KPI create 7 KPI for a week.
     */
    @Test
    public void getKPIForWeek() {
        LocalDate fromDate = OffsetDateTime.now().toLocalDate();
        LocalDate toDate = OffsetDateTime.now().toLocalDate().plusDays(6L);
        int countLongUsed = 1;
        int countShortUsed = 3;

        int i = 1;
        for (LocalDate loopDate = fromDate; loopDate.isBefore(toDate)
                || loopDate.isEqual(toDate); loopDate = loopDate.plusDays(1L)) {
            when(this.submissionCodeRepositoryMock.countSubmissionCodeUsedByDate(
                    FormatDatesKPI.normaliseDateFrom(loopDate, this.targetZoneId),
                    FormatDatesKPI.normaliseDateTo(loopDate, this.targetZoneId), "1"))
                            .thenReturn(Long.valueOf(countLongUsed * i));
            when(this.submissionCodeRepositoryMock.countSubmissionCodeUsedByDate(
                    FormatDatesKPI.normaliseDateFrom(loopDate, this.targetZoneId),
                    FormatDatesKPI.normaliseDateTo(loopDate, this.targetZoneId), "2"))
                            .thenReturn(Long.valueOf(countShortUsed * i));
            when(this.submissionCodeRepositoryMock
                    .countSubmissionCodeExpiredDate(FormatDatesKPI.normaliseDateTo(loopDate, this.targetZoneId), "1"))
                            .thenReturn(Long.valueOf(i * (i + 1)));
            when(this.submissionCodeRepositoryMock
                    .countSubmissionCodeExpiredDate(FormatDatesKPI.normaliseDateTo(loopDate, this.targetZoneId), "2"))
                            .thenReturn(Long.valueOf(i * (i + 2)));
            i = i + 1;
        }
        List<SubmissionCodeServerKpi> result = new ArrayList<>();
        try {
            result = this.kpiService.generateKPI(fromDate, toDate);
        } catch (SubmissionCodeServerException s) {
            fail(SHOULD_NOT_FAIL_MESSAGE);
        }
        assertTrue(result.size() == 7);
    }

    @Test
    public void testCountShortCodesGenerated() {

        // Given
        final long nbGeneratedCodes = 1L;
        when(this.submissionCodeRepositoryMock.countGeneratedCodes(this.startDateTime, this.endDateTime, CodeTypeEnum.SHORT.getTypeCode())).thenReturn(nbGeneratedCodes);

        // When
        List<SubmissionCodeServerKpi> submissionCodeServerKpis = null;
        try {
            submissionCodeServerKpis = this.kpiService.generateKPI(this.startDate, this.endDate);
        } catch (SubmissionCodeServerException e) {
            fail(SHOULD_NOT_FAIL_MESSAGE);
        }

        // Then
        assertTrue(submissionCodeServerKpis.size() == 2);
        assertEquals(nbGeneratedCodes, submissionCodeServerKpis.get(0).getNbShortCodesGenerated());
    }

}
