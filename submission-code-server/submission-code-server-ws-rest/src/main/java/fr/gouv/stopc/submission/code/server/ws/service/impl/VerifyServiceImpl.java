package fr.gouv.stopc.submission.code.server.ws.service.impl;

import fr.gouv.stopc.submission.code.server.database.dto.SubmissionCodeDto;
import fr.gouv.stopc.submission.code.server.database.service.ISubmissionCodeService;
import fr.gouv.stopc.submission.code.server.commun.enums.CodeTypeEnum;
import fr.gouv.stopc.submission.code.server.ws.service.IVerifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class VerifyServiceImpl implements IVerifyService {

    private ISubmissionCodeService submissionCodeService;

    /**
     *  Default constructor spring-injecting the needed services.
     * @param submissionCodeService service from the database module permitting to interface with the data base.
     */
    @Inject
    public VerifyServiceImpl (ISubmissionCodeService submissionCodeService){
        this.submissionCodeService = submissionCodeService;
    }

    @Override
    public boolean verifyCode(String code, String type) {
        Optional<CodeTypeEnum> typeToFound = CodeTypeEnum.searchMatchType(type);
        CodeTypeEnum typeFound = null;
        if(typeToFound.isPresent()){
            typeFound = typeToFound.get();
        }

        Optional<SubmissionCodeDto> codeDtoOptional = submissionCodeService.getCodeValidity(code, typeFound);

        if (!codeDtoOptional.isPresent()) {
            log.warn("Code {} ({}) was not found.", code, type);
            return false;
        }

        SubmissionCodeDto codeDto = codeDtoOptional.get();

        if (codeDto.getUsed().equals(Boolean.TRUE) || Objects.nonNull(codeDto.getDateUse())){
            log.warn("Code {} ({}) has already been used.", code, type);
            return false;
        }

        ZoneOffset zoneOffset = codeDto.getDateAvailable().getOffset();
        OffsetDateTime dateNow = LocalDateTime.now().atOffset(zoneOffset);

        if(!validateDate(code, type, dateNow,codeDto.getDateAvailable(),codeDto.getDateEndValidity())){
            log.warn("Code {} ({}) rejected because outside acceptable validity range.", code, type);
            return false;
        }

        codeDto.setUsed(true);
        codeDto.setDateUse(dateNow);
        final boolean isUpdated = submissionCodeService.updateCodeUsed(codeDto);

        if(isUpdated) {
            log.info("Code {} ({}) has been updated successfully.", code, type);
        } else {
            log.error("Code {} ({}) could not be updated.", code, type);
        }
        return isUpdated;
    }

    /**
     * A code cannot be used before he is valid or after it has expired.
     * @param code
     * @param dateNow
     * @param dateAvailable
     * @param dateEndValidity
     * @return
     */
    private boolean validateDate(String code,
                                 String type,
                                 OffsetDateTime dateNow,
                                 OffsetDateTime dateAvailable,
                                 OffsetDateTime dateEndValidity) {
        if(Objects.isNull(dateAvailable) || Objects.isNull(dateEndValidity)){
            log.info("Code {} ({}) does not have a complete validity period (start date or end date missing)",
                    code,
                    type);
            return false;
        }

        if  (dateNow.isBefore(dateAvailable)) {
            log.info("Code {} ({}) being used before validity period start {}.",
                    code,
                    type,
                    dateAvailable);
            return false;
        } else if (dateNow.isAfter(dateEndValidity)) {
            log.info("Code {} ({}) being used after validity period end {}.",
                    code,
                    type,
                    dateEndValidity);
            return false;
        } else {
            return true;
        }
    }
}