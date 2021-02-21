package fr.gouv.stopc.submission.code.server.ws.service;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.database.entity.Lot;
import fr.gouv.stopc.submission.code.server.database.entity.SubmissionCode;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import fr.gouv.stopc.submission.code.server.ws.dto.CodeDetailedDto;
import fr.gouv.stopc.submission.code.server.ws.dto.CodeSimpleDto;

import java.time.OffsetDateTime;
import java.util.List;

public interface IGenerateService {

    CodeSimpleDto generateShortCode()
            throws SubmissionCodeServerException;

    /**
     * Method used to sequentially generate codes of codeType in parameter
     * @param size the desired number of code to be generated
     * @param cte the code type desired
     * @param validFrom date from the code should be valid.
     * @return list of unique persisted codes
     */
    List<CodeDetailedDto> generateCodeGeneric(final long size,
                                              final CodeTypeEnum cte,
                                              final OffsetDateTime validFrom,
                                              final Lot lotObject
    ) throws SubmissionCodeServerException;

    /**
     * Method return List of OffsetDateTime increment by day and truncate to day
     * @param size give size of the list to be returned included validFromFirstValue
     * @param validFromFirstValue seed time from the list should be generated from.
     * @return List of OffsetDateTime increment by day and truncate to day.
     */
    List<OffsetDateTime> getListOfValidDatesFor(int size, OffsetDateTime validFromFirstValue);

    List<CodeDetailedDto> generateLongCodesWithBulkMethod(
            final OffsetDateTime validFrom,
            final long dailyAmount,
            final Lot lot,
            final OffsetDateTime validGenDate
    ) throws SubmissionCodeServerException;

    List<CodeDetailedDto> submissionCodeListToCodeDetailDtoList(Iterable<SubmissionCode> submissionCodes);

    }
