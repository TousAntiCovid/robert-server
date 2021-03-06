package fr.gouv.stopc.robertserver.ws.controller.impl;

import fr.gouv.stopc.robertserver.ws.dto.VerifyResponseDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerUnauthorizedException;
import fr.gouv.stopc.robertserver.ws.service.IRestApiService;
import fr.gouv.stopc.robertserver.ws.utils.PropertyLoader;
import fr.gouv.stopc.robertserver.ws.vo.ReportBatchRequestVo;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.Optional;

import static java.lang.Boolean.FALSE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportControllerDelegate {

    private final IRestApiService restApiService;

    private final PropertyLoader propertyLoader;

    public boolean isReportRequestValid(ReportBatchRequestVo reportBatchRequestVo) throws RobertServerException {
        if (this.areBothFieldsPresent(reportBatchRequestVo)) {
            log.warn("Contacts and ContactsAsBinary are both present");
            return false;
        } else if (Objects.isNull(reportBatchRequestVo.getContacts())) {
            log.warn("Contacts are null. They could be empty([]) but not null");
            return false;
        } else if (this.areBothFieldsAbsent(reportBatchRequestVo)) {
            log.warn("Contacts and ContactsAsBinary are both absent");
            return false;
        }

        if (FALSE.equals(this.propertyLoader.getDisableCheckToken())) {
            this.checkReportTokenValidity(reportBatchRequestVo.getToken());
        }

        return true;
    }

    private boolean areBothFieldsPresent(ReportBatchRequestVo reportBatchRequestVo) {
        return !CollectionUtils.isEmpty(reportBatchRequestVo.getContacts())
                && StringUtils.isNotEmpty(reportBatchRequestVo.getContactsAsBinary());
    }

    private boolean areBothFieldsAbsent(ReportBatchRequestVo reportBatchRequestVo) {
        return Objects.isNull(reportBatchRequestVo.getContacts())
                && StringUtils.isEmpty(reportBatchRequestVo.getContactsAsBinary());
    }

    private void checkReportTokenValidity(String token) throws RobertServerException {

        Optional<VerifyResponseDto> response = this.restApiService.verifyReportToken(token);

        if (response.isEmpty() || !response.get().isValid()) {
            throw new RobertServerUnauthorizedException("Unrecognized token of length: " + token.length());
        }

        log.info("Verifying the token succeeded");
    }

}
