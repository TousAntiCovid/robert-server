package fr.gouv.stopc.submission.code.server.ws.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Async;

import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.entity.Lot;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.CodeDetailedDto;

public interface IFileService {

    @Async
    Optional<ByteArrayOutputStream> zipExportAsync(Long numberCodeDay, Lot lotObject, String dateFrom, String dateTo)
            throws SubmissionCodeServerException;

    /**
     * Method:
     * 1)generate long codes between dateFrom to dateTo
     * 2) export from database
     * 3) create one csv file each day between dateFrom to dateTo
     * 4) create archive with csv files
     * @param numberCodeDay
     * @param lotObject
     * @param dateFrom
     * @param dateTo
     * @return
     */
    @Async
     Optional<ByteArrayOutputStream> zipExport(Long numberCodeDay, Lot lotObject, String dateFrom, String dateTo)
             throws SubmissionCodeServerException;

    /**
     * STEP - 1 [ PERSISTING ]
     * @param codePerDays code per days to be generated
     * @param lotObject lot identifier that the series should take
     * @param from start date of the series of days code generation
     * @param to end date of the series of days code generation
     * @throws SubmissionCodeServerException
     * @return
     */

     List<CodeDetailedDto> persistLongCodes(Long codePerDays, Lot lotObject, OffsetDateTime from, OffsetDateTime to)
             throws SubmissionCodeServerException;

    /**
     * STEP 2 - [ PARSING DATA TO CSV Data ]
     * @param submissionCodeDtos
     * @param dates
     * @param tmpDirectory
     * @return List of csv dataByFilename
     */
    List<String> serializeCodesToCsv(List<SubmissionCodeDto> submissionCodeDtos, List<OffsetDateTime> dates, File tmpDirectory)
            throws SubmissionCodeServerException;


    /**
     * STEP 3 - [ Packaging dataByFilename in a Zip ]
     * @param dataByFilename csv data to be zipped.
     * @return ZipOutputStream instance containing csv data.
     */
    ByteArrayOutputStream packageCsvDataToZipFile(List<String> dataByFilename, File tmpDirectory)
            throws SubmissionCodeServerException, IOException;

    }
