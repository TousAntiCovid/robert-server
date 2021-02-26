package fr.gouv.stopc.submission.code.server.ws.service.impl;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.entity.Lot;
import fr.gouv.stopc.submission.code.server.database.entity.SequenceFichier;
import fr.gouv.stopc.submission.code.server.database.service.ISequenceFichierService;
import fr.gouv.stopc.submission.code.server.database.service.ISubmissionCodeService;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.CodeDetailedDto;
import fr.gouv.stopc.submission.code.server.ws.dto.SubmissionCodeCsvDto;
import fr.gouv.stopc.submission.code.server.ws.service.IFileService;
import fr.gouv.stopc.submission.code.server.ws.service.IGenerateService;
import fr.gouv.stopc.submission.code.server.ws.service.ISFTPService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static org.apache.tomcat.util.http.fileupload.FileUtils.deleteDirectory;


@Slf4j
@Service
@Transactional
public class FileServiceImpl implements IFileService {
    private String HEADER_CSV = "code_pour_qr%s code_brut%s validite_debut%s validite_fin\n";
    private ISubmissionCodeService submissionCodeService;
    private IGenerateService generateService;
    private ISFTPService sftpService;
    private ISequenceFichierService sequenceFichierService;

    @Value("${stop.covid.qr.code.url}")
    private String qrCodeBaseUrlToBeFormatted;

    /**TargetZoneId is the time zone id (in the java.time.ZoneId way) on which the submission code server should deliver the codes.
     * eg.: for France is "Europe/Paris"*/
    @Value("${stop.covid.qr.code.targetzone}")
    private String targetZoneId;

    /**The separator is the character uses to separate the columns.*/
    @Value("${csv.separator}")
    private Character csvSeparator;

    /**The delimiter is the character uses to enclose the strings.*/
    @Value("${csv.delimiter}")
    private Character csvDelimiter;

    @Value("${csv.filename.formatter}")
    private String csvFilenameFormat;

    @Value("${submission.code.server.sftp.enableautotransfer}")
    private boolean transferFile;

    @Value("${csv.directory.tmp}")
    private String directoryTmpCsv;



    @Inject
    public FileServiceImpl(ISubmissionCodeService submissionCodeService, IGenerateService generateService, ISFTPService sftpService,
    		ISequenceFichierService sequenceFichierService) {
        this.submissionCodeService = submissionCodeService;
        this.generateService=generateService;
        this.sftpService=sftpService;
        this.sequenceFichierService = sequenceFichierService;
    }

    @Async
    @Override
    public Optional<ByteArrayOutputStream> zipExportAsync(Long numberCodeDay, Lot lotObject, String dateFrom, String dateTo)
            throws SubmissionCodeServerException {
        final OffsetDateTime start = OffsetDateTime.now();

        log.info("Generating long codes with archive method asynchronously");

        final Optional<ByteArrayOutputStream> byteArrayOutputStream = this.zipExport(numberCodeDay, lotObject, dateFrom, dateTo);

        log.info("It took {} seconds to generate {} codes", ChronoUnit.SECONDS.between(start, OffsetDateTime.now()), (ChronoUnit.DAYS.between(OffsetDateTime.parse(dateFrom), OffsetDateTime.parse(dateTo)) + 1) * numberCodeDay);

        return byteArrayOutputStream;
    }

    @Override
    public Optional<ByteArrayOutputStream> zipExport(Long numberCodeDay, Lot lotObject, String dateFrom, String dateTo)
            throws SubmissionCodeServerException {
        log.info("Generate {} codes per day from {} to {}", numberCodeDay, dateFrom, dateTo);
        OffsetDateTime dateTimeFrom;
        OffsetDateTime dateTimeTo;

        try {
            dateTimeFrom = OffsetDateTime.parse(dateFrom, DateTimeFormatter.ISO_DATE_TIME);
            dateTimeTo = OffsetDateTime.parse(dateTo, DateTimeFormatter.ISO_DATE_TIME);
        } catch (RuntimeException e) {
            log.error(SubmissionCodeServerException.ExceptionEnum.PARSE_STR_DATE_ERROR.getMessage());
            throw new SubmissionCodeServerException(
                    SubmissionCodeServerException.ExceptionEnum.PARSE_STR_DATE_ERROR
            );
        }

        if(!isDateValid(dateTimeFrom,dateTimeTo)) {
            log.error(SubmissionCodeServerException.ExceptionEnum.INVALID_DATE.getMessage());
            throw new SubmissionCodeServerException(
                    SubmissionCodeServerException.ExceptionEnum.INVALID_DATE
            );
        }


        // STEP 1 - create codes
        // STEP 2 parsing codes to csv dataByFilename
        File tmpDirectory = new File(System.getProperty("java.io.tmpdir")+directoryTmpCsv);
        tmpDirectory.mkdir();
        log.info("Create directory {} and Start generation codes bulk method", tmpDirectory.getAbsolutePath());
        List<String> dataByFilename = this.persistLongCodesQuiet(numberCodeDay, lotObject, dateTimeFrom, dateTimeTo, tmpDirectory);

        log.info("End generation codes");
        // STEP 3 packaging csv data
        ByteArrayOutputStream zipOutputStream = null;
        try {
            zipOutputStream = packageCsvDataToZipFile(dataByFilename, tmpDirectory);
        } catch (IOException e) {
            log.error(SubmissionCodeServerException.ExceptionEnum.PACKAGING_CSV_FILE_ERROR.getMessage(), e);
            throw new SubmissionCodeServerException(
                    SubmissionCodeServerException.ExceptionEnum.PACKAGING_CSV_FILE_ERROR,
                    e
            );
        }
        if(transferFile){
            // async method is called here.
            log.info("SFTP transfer is about to be submitted.");
            sftpService.transferFileSFTP(zipOutputStream);

            log.info("SFTP transfer have been submitted.");
        } else {
            log.info("No SFTP transfer have been submitted.");
        }

        //delete directory
        try {
            log.info("Delete directory {}",tmpDirectory.getAbsolutePath());
            deleteDirectory(tmpDirectory);
        } catch (IOException e) {
            log.error("Delete directory is not good");
        }

        return  Optional.of(zipOutputStream);
    }


    @Override
    public List<CodeDetailedDto> persistLongCodes(Long codePerDays, Lot lotObject, OffsetDateTime from, OffsetDateTime to)
            throws SubmissionCodeServerException {
        List<CodeDetailedDto> listCodeDetailedDto = new ArrayList<>();
        OffsetDateTime fromWithoutHours = from.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime toWithoutHours = to.truncatedTo(ChronoUnit.DAYS);

        OffsetDateTime validGenDate= OffsetDateTime.now();

        long diffDays= ChronoUnit.DAYS.between(fromWithoutHours, toWithoutHours) + 1;
        int diff = Integer.parseInt(Long.toString(diffDays));
        List<OffsetDateTime> datesFromList = generateService.getListOfValidDatesFor(diff, from);
        int i = 0;
        for(OffsetDateTime dateFromDay: datesFromList) {
            i++;
            log.info("Generate code for start validity date : {} [{}/{}]", dateFromDay, i, datesFromList.size());
            List<CodeDetailedDto> codeSaves = this.generateService.generateLongCodesWithBulkMethod(
                    dateFromDay,
                    codePerDays,
                    lotObject,
                    validGenDate
            );

            if(CollectionUtils.isNotEmpty(codeSaves)){
                listCodeDetailedDto.addAll(codeSaves);
            }
        }
        return listCodeDetailedDto;
    }

    public List<String> persistLongCodesQuiet(Long codePerDays, Lot lotObject, OffsetDateTime from, OffsetDateTime to, File tmpDirectory)
            throws SubmissionCodeServerException {
        List<String> listFile = new ArrayList<>();

        OffsetDateTime fromWithoutHours = from.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime toWithoutHours = to.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime validGenDate= OffsetDateTime.now();

        long diffDays= ChronoUnit.DAYS.between(fromWithoutHours, toWithoutHours) + 1;
        int diff = Integer.parseInt(Long.toString(diffDays));
        List<OffsetDateTime> datesFromList = generateService.getListOfValidDatesFor(diff, from);
        for(OffsetDateTime dateFromDay: datesFromList) {
            List<CodeDetailedDto> codeSaves = this.generateService.generateLongCodesWithBulkMethod(
                    dateFromDay,
                    codePerDays,
                    lotObject,
                    validGenDate
            );

            if(CollectionUtils.isNotEmpty(codeSaves)){
                final List<SubmissionCodeDto> collect = codeSaves.stream()
                        .map(codeDetailedDto -> mapToSubmissionCodeDto(codeDetailedDto, lotObject.getId()))
                        .collect(Collectors.toList());

                listFile.addAll((this.serializeCodesToCsv(collect, Arrays.asList(dateFromDay),tmpDirectory)));
            }
        }
        return listFile;
    }

    @Override
    public List<String> serializeCodesToCsv(
            List<SubmissionCodeDto> submissionCodeDtos,
            List<OffsetDateTime> dates,
            File tmpDirectory)
            throws SubmissionCodeServerException {
        List<String> dataByFilename = new ArrayList<>();

        for(OffsetDateTime dateTime : dates){
            List<SubmissionCodeDto> listForDay= submissionCodeDtos
                    .stream().filter(tmp-> dateTime.isEqual(tmp.getDateAvailable()))
                    .collect(Collectors.toList());
            
            OffsetDateTime date  = dateTime.withOffsetSameInstant(OffsetDateTime.now(ZoneId.of(this.targetZoneId)).getOffset());
            Optional<SequenceFichier> seqFichier = this.sequenceFichierService.getSequence(date);
            int sequence = LocalDate.now().getYear() % 100;
            if(seqFichier.isPresent()) {
            	sequence = seqFichier.get().getSequence();
            }

            if(CollectionUtils.isNotEmpty(listForDay)) {
                byte[] fileByte = createCSV(listForDay, dateTime);
                String filename = this.getCsvFilename(dateTime, sequence);
                log.info("About to create the file : {}", filename);
                dataByFilename.add(filename);
                OutputStream os = null;
                try {
                    os = new FileOutputStream(tmpDirectory.getAbsolutePath()+ File.separator+ filename);
                    os.write(fileByte);
                    os.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return dataByFilename;
    }

    @Override
    public ByteArrayOutputStream packageCsvDataToZipFile(List<String> dataByFilename, File tmpDirectory)
            throws SubmissionCodeServerException, IOException {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = null;
        TarArchiveOutputStream tarArchiveOutputStream = null;

        try {
            gzipOutputStream = new GZIPOutputStream(byteOutputStream);
            tarArchiveOutputStream = new TarArchiveOutputStream(gzipOutputStream);

            for (String filename: dataByFilename){
                TarArchiveEntry entry = new TarArchiveEntry(filename);
                File file = new File(tmpDirectory.getAbsolutePath() + File.separator+ filename);
                FileInputStream fileInputStream = new FileInputStream(tmpDirectory.getAbsolutePath() + File.separator+ filename);
                byte [] data = new byte [(int) file.length()];
                fileInputStream.read(data);
                entry.setSize(file.length());
                tarArchiveOutputStream.putArchiveEntry(entry);
                final ByteArrayInputStream inputByteArray = new ByteArrayInputStream(data);
                IOUtils.copy(inputByteArray, tarArchiveOutputStream);
                inputByteArray.close();
                fileInputStream.close();
                tarArchiveOutputStream.closeArchiveEntry();
            }

        } catch (IOException ioe) {
            log.error(SubmissionCodeServerException.ExceptionEnum.PACKAGING_CSV_FILE_ERROR.getMessage(), ioe);
            throw new SubmissionCodeServerException(
                    SubmissionCodeServerException.ExceptionEnum.PACKAGING_CSV_FILE_ERROR,
                    ioe
            );
        } finally {
            {
                if(byteOutputStream!= null){
                    byteOutputStream.close();
                }
                if(tarArchiveOutputStream!=null){
                    tarArchiveOutputStream.close();
                }
                if(gzipOutputStream!= null){
                    gzipOutputStream.close();
                }
            }
        }
        return byteOutputStream;
    }

    /**
     * Create one file csv for the list of submissionCodes with date of available equal to dateTimeFrom
     * @param submissionCodeDtoList list of submissionCodes for a day specific
     * @param date the date of available of submissionCode
     * @return submissionCodeDtoList parsed into a csv file
     */
    private byte[] createCSV(List<SubmissionCodeDto> submissionCodeDtoList, OffsetDateTime date)
            throws SubmissionCodeServerException {
        String header= HEADER_CSV.replaceAll("%s",Character.toString(csvSeparator));
        // converting list SubmissionCodeDto to SubmissionCodeCsvDto to be proceeded in csv generator
        final List<SubmissionCodeCsvDto> submissionCodeCsvDtos = convert(submissionCodeDtoList);

        StringWriter fileWriter = new StringWriter();
        fileWriter.append(header);

        ColumnPositionMappingStrategy mappingStrategy = new ColumnPositionMappingStrategy();
        mappingStrategy.setType(SubmissionCodeCsvDto.class);

        String[] columns = new String[]{"qrcode", "code", "dateAvailable", "dateEndValidity"};
        mappingStrategy.setColumnMapping(columns);
        StatefulBeanToCsvBuilder<SubmissionCodeDto> builder = new StatefulBeanToCsvBuilder<SubmissionCodeDto>(fileWriter)
                .withSeparator(csvSeparator).withQuotechar(csvDelimiter);

        StatefulBeanToCsv statefulBeanToCsv = builder.withMappingStrategy(mappingStrategy).build();

        try {
            statefulBeanToCsv.write(submissionCodeCsvDtos);

            return fileWriter.toString().getBytes(StandardCharsets.UTF_8);

        } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            log.error(SubmissionCodeServerException.ExceptionEnum.CODE_TO_CSV_PARSING_ERROR.getMessage(), e);
            throw new SubmissionCodeServerException(
                    SubmissionCodeServerException.ExceptionEnum.CODE_TO_CSV_PARSING_ERROR,
                    e
            );
        }
    }

    /**
     *
     * @param from start date
     * @param to end date
     * @throws DateTimeException when end date is less than start date. or if start date is inferior to current time.
     */

    protected Boolean isDateValid(OffsetDateTime from, OffsetDateTime to)
            throws DateTimeException {
        return !(OffsetDateTime.now().toLocalDate().compareTo(from.toLocalDate()) > 0 || from.isAfter(to));
    }



    /**
     * Method convert list of dao SubmissionCodeDto to csv data SubmissionCodeCsvDto
     * @param listForDay list provided by dao service to be converted to a list of csv DTO
     * @return list of SubmissionCodeCsvDto
     */
    private List<SubmissionCodeCsvDto> convert(List<SubmissionCodeDto> listForDay) throws SubmissionCodeServerException {
        final ModelMapper modelMapper = new ModelMapper();

        final List<SubmissionCodeCsvDto> submissionCodeCsvDtos = listForDay.stream().map(s -> {
            final SubmissionCodeCsvDto csvDto = modelMapper.map(s, SubmissionCodeCsvDto.class);
            try {
                csvDto.setQrcode(String.format(
                        this.qrCodeBaseUrlToBeFormatted,
                        URLEncoder.encode(csvDto.getCode(), "UTF-8"),
                        URLEncoder.encode(csvDto.getType(), "UTF-8")
                ));
                return csvDto;
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }).collect(Collectors.toList());

        if(submissionCodeCsvDtos.size() != listForDay.size()) {
            log.error(SubmissionCodeServerException.ExceptionEnum.MAPPING_CODE_FOR_CSV_FILE_ERROR.getMessage());
            throw new SubmissionCodeServerException(
                    SubmissionCodeServerException.ExceptionEnum.MAPPING_CODE_FOR_CSV_FILE_ERROR
            );
        }

        return submissionCodeCsvDtos;
    }

    /**
     * Formats csv file name from date and pattern set in application.properties.
     * @param date date of the file to generate
     * @return  formatted csv file name from date and pattern set in application.properties.
     */
    private String getCsvFilename(OffsetDateTime date, int sequence) {
    	date = date.withOffsetSameInstant(OffsetDateTime.now(ZoneId.of(this.targetZoneId)).getOffset());
    	if(sequence > 0) {
    		return String.format(csvFilenameFormat, sequence,
    				date.format(DateTimeFormatter.ofPattern("yyMMdd")));
    	}
    	
    	return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private SubmissionCodeDto mapToSubmissionCodeDto(CodeDetailedDto codeDetailedDto, Long idLot) {
        SubmissionCodeDto submissionCodeDto = new SubmissionCodeDto();
        submissionCodeDto.setLot(idLot);
        submissionCodeDto.setUsed(false);
        submissionCodeDto.setType(CodeTypeEnum.LONG.getTypeCode());
        submissionCodeDto.setDateGeneration(OffsetDateTime.parse(codeDetailedDto.getDateGenerate(), DateTimeFormatter.ISO_DATE_TIME));
        submissionCodeDto.setDateEndValidity(OffsetDateTime.parse(codeDetailedDto.getValidUntil(), DateTimeFormatter.ISO_DATE_TIME));
        submissionCodeDto.setCode(codeDetailedDto.getCode());
        submissionCodeDto.setDateAvailable(OffsetDateTime.parse(codeDetailedDto.getValidFrom(), DateTimeFormatter.ISO_DATE_TIME));
        return submissionCodeDto;
    }

}
