package fr.gouv.stopc.submission.code.server.database.service;

import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.entity.Lot;
import fr.gouv.stopc.submission.code.server.database.entity.SubmissionCode;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface ISubmissionCodeService {
    Optional<SubmissionCodeDto> getCodeValidity(String code, CodeTypeEnum type);

    Iterable<SubmissionCode> saveAllCodes(List<SubmissionCodeDto> submissionCodeDtos);
    Iterable<SubmissionCode> saveAllCodes(List<SubmissionCodeDto> submissionCodeDtos, Lot lot);

    Optional<SubmissionCode> saveCode(SubmissionCodeDto submissionCodeDto);

    Optional<SubmissionCode> saveCode(SubmissionCodeDto submissionCodeDto, Lot lot);

    boolean updateCodeUsed(SubmissionCodeDto submissionCodeDto);

    /**
     * Return number of code for the given lot identifier.
     * @param lotIdentifier lot identifier in db
     * @return return number of code with the given lot identifier
     */
    long getNumberOfCodesForLotIdentifier(long lotIdentifier);

        /**
         * Get specific range of code rows
         * @param lotIdentifier lot identifier the codes should be matched
         * @param page page number
         * @param elementsByPage the row page the list ends.
         * @return list of code page row "page" elementsByPage rows "elementsByPage" e.g. : page = 10 and elementsByPage = 12 , size list is 3 and has only row 10, 11, 12
         */
    Page<SubmissionCode> getSubmissionCodesFor(long lotIdentifier, int page, int elementsByPage);

    void removeByLot(Lot lot);
}