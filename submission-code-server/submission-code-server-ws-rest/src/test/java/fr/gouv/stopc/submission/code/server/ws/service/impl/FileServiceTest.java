package fr.gouv.stopc.submission.code.server.ws.service.impl;

import static org.apache.tomcat.util.http.fileupload.FileUtils.deleteDirectory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.internal.util.Assert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.commun.service.impl.LongCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.commun.service.impl.ShortCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.entity.Lot;
import fr.gouv.stopc.submission.code.server.database.entity.SequenceFichier;
import fr.gouv.stopc.submission.code.server.database.service.ISequenceFichierService;
import fr.gouv.stopc.submission.code.server.database.service.impl.SubmissionCodeServiceImpl;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.CodeDetailedDto;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.properties")
class FileServiceTest {

    /**
     * Zone ID to use
     */
    @Value("${stop.covid.qr.code.targetzone}")
    private String targetZoneId;
	
    @Mock
    private GenerateServiceImpl generateService;

    @Mock
    private SFTPServiceImpl sftpService;

    @Mock
    private SubmissionCodeServiceImpl submissionCodeService;

    @Mock
    private ISequenceFichierService sequenceFichierService;

    @Spy
    @InjectMocks
    private FileServiceImpl fileExportService;


    @BeforeEach
    public void init(){

        MockitoAnnotations.initMocks(this);
        
        TimeZone.setDefault(TimeZone.getTimeZone(this.targetZoneId));

        ReflectionTestUtils.setField(this.fileExportService, "qrCodeBaseUrlToBeFormatted", "my%smy%s");
        ReflectionTestUtils.setField(this.fileExportService, "targetZoneId", this.targetZoneId);
        ReflectionTestUtils.setField(this.fileExportService, "csvSeparator", ',');
        ReflectionTestUtils.setField(this.fileExportService, "csvDelimiter", '"');
        ReflectionTestUtils.setField(this.fileExportService, "csvFilenameFormat", "%d%s.csv");
        ReflectionTestUtils.setField(this.fileExportService, "directoryTmpCsv", "tmp");
        ReflectionTestUtils.setField(this.fileExportService, "transferFile", false);
        ReflectionTestUtils.setField(this.generateService, "targetZoneId",this.targetZoneId);
        ReflectionTestUtils.setField(this.generateService, "numberOfTryInCaseOfError",1);
        ReflectionTestUtils.setField(this.generateService, "timeValidityLongCode",2);
        ReflectionTestUtils.setField(this.generateService, "timeValidityShortCode",15);
        ReflectionTestUtils.setField(this.generateService, "submissionCodeService", this.submissionCodeService);
        ReflectionTestUtils.setField(this.generateService, "shortCodeService", new ShortCodeServiceImpl());
        ReflectionTestUtils.setField(this.generateService, "longCodeService", new LongCodeServiceImpl());
        when(this.sequenceFichierService.getSequence(any())).thenReturn(Optional.empty());
    }

    @Test
    public void testCreateZipComplete() throws IOException, SubmissionCodeServerException {
        // String numberCodeDay, String lot, String dateFrom, String dateTo
        final CodeDetailedDto sc = CodeDetailedDto.builder()
                .typeAsString(CodeTypeEnum.LONG.getTypeCode())
                .validUntil(OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .validFrom(OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .dateGenerate(OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .code("3d27eeb8-956c-4660-bc04-8612a4c0a7f1")
                .build();

        OffsetDateTime startDate = OffsetDateTime.now();
        String nowDay = startDate.format(DateTimeFormatter.ISO_DATE_TIME);
        String endDay = OffsetDateTime.now().plusDays(4L).format(DateTimeFormatter.ISO_DATE_TIME);

        Lot lot= new Lot();
        lot.setId(1L);

        OffsetDateTime date= OffsetDateTime.now().plusDays(1L);
        List<OffsetDateTime> dates = new ArrayList<>();
        dates.add(date);

        Mockito.when(generateService.generateLongCodesWithBulkMethod(date,10, lot, date)).thenReturn(Arrays.asList(sc));
        Mockito.when(generateService.getListOfValidDatesFor(5,startDate)).thenReturn(dates);
        Optional<ByteArrayOutputStream> result = Optional.empty();


        result = fileExportService.zipExport(10L, lot, nowDay, endDay);

        Assert.notNull(result.get());


    }

    @Test
    public void testCreateZipCompleteOneDay() throws Exception {
        // String numberCodeDay, String lot, String dateFrom, String dateTo
        Optional<ByteArrayOutputStream> result;

        final CodeDetailedDto sc = CodeDetailedDto.builder()
                .typeAsString(CodeTypeEnum.LONG.getTypeCode())
                .validUntil(OffsetDateTime.now().toString())
                .validFrom(OffsetDateTime.now().toString())
                .dateGenerate(OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .code("3d27eeb8-956c-4660-bc04-8612a4c0a7f1")
                .build();

        OffsetDateTime nowDay = OffsetDateTime.now();
        String nowDayString = nowDay.format(DateTimeFormatter.ISO_DATE_TIME);
        String endDay = nowDayString;

        Lot lot= new Lot();
        lot.setId(1L);

        Mockito.when(generateService.generateLongCodesWithBulkMethod(nowDay, 10, lot, nowDay)).thenReturn(Arrays.asList(sc));
        List<OffsetDateTime> dates = new ArrayList<>();
        dates.add(nowDay);
        Mockito.when(generateService.getListOfValidDatesFor(1,nowDay)).thenReturn(dates);

        result = fileExportService.zipExport(10L, lot, nowDayString, endDay);

        Assert.notNull(result.get());

    }

    @Test
    public void testCheckDatesValidation(){

        OffsetDateTime startDay = OffsetDateTime.now().minusDays(1l);
        OffsetDateTime endDay = OffsetDateTime.now().plusDays(4L);
        Assert.isTrue(!fileExportService.isDateValid(startDay, endDay));

    }

    @Test
    public void testSerializeCodesToCsv()  {
        File tmpDirectory = new  File("test");
        tmpDirectory.mkdir();
        List<SubmissionCodeDto> submissionCodeDtos = new ArrayList<>();
        OffsetDateTime nowDay = OffsetDateTime.now();
        SubmissionCodeDto submissionCodeDto =  SubmissionCodeDto.builder().code("test").dateAvailable(nowDay).dateEndValidity(nowDay.plusDays(1L))
                .dateGeneration(nowDay).lot(1L).used(false).type("test").build();

        submissionCodeDtos.add(submissionCodeDto);
        List<OffsetDateTime> dates = new ArrayList<>();
        dates.add(nowDay);
        try {
            fileExportService.serializeCodesToCsv(submissionCodeDtos, dates, tmpDirectory);
        } catch (SubmissionCodeServerException e) {
            e.printStackTrace();
        }

        Assert.notNull(tmpDirectory.list());
        try {
            deleteDirectory(tmpDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPackageCsvDataToZipFile(){
        File tmpDirectory = new  File("test");
        tmpDirectory.mkdir();
        List<SubmissionCodeDto> submissionCodeDtos = new ArrayList<>();
        OffsetDateTime nowDay = OffsetDateTime.now();
        SubmissionCodeDto submissionCodeDto =  SubmissionCodeDto.builder().code("test").dateAvailable(nowDay).dateEndValidity(nowDay.plusDays(1L))
                .dateGeneration(nowDay).lot(1L).used(false).type("test").build();

        submissionCodeDtos.add(submissionCodeDto);
        List<OffsetDateTime> dates = new ArrayList<>();
        dates.add(nowDay);


        when(sequenceFichierService.getSequence(nowDay)).thenReturn(Optional.of(new SequenceFichier(1L, nowDay.getYear(), nowDay.getMonthValue(), nowDay.getDayOfMonth(), nowDay.getYear() % 100 + 1)));
        //create csv in directory
        try {
            fileExportService.serializeCodesToCsv(submissionCodeDtos, dates, tmpDirectory);
        } catch (SubmissionCodeServerException e) {
            e.printStackTrace();
        }
        List<String> datesZip= new ArrayList<>();
        datesZip.add(String.format("%s.csv",nowDay.plus(1, ChronoUnit.YEARS).format(DateTimeFormatter.ofPattern("yyyy")).substring(2)
                + nowDay.format(DateTimeFormatter.ofPattern("yyMMdd"))));
        ByteArrayOutputStream result= null;
        //call package zip
        try {
           result = fileExportService.packageCsvDataToZipFile(datesZip, tmpDirectory);
        } catch (SubmissionCodeServerException | IOException e) {
            e.printStackTrace();
        }

        Assert.notNull(result);
        //remove resources
        try {
            deleteDirectory(tmpDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
