package fr.gouv.stopc.robertserver.ws.controller.impl;

import fr.gouv.stopc.robertserver.ws.dto.VerifyResponseDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerBadRequestException;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerUnauthorizedException;
import fr.gouv.stopc.robertserver.ws.service.IRestApiService;
import fr.gouv.stopc.robertserver.ws.utils.MessageConstants;
import fr.gouv.stopc.robertserver.ws.utils.PropertyLoader;
import fr.gouv.stopc.robertserver.ws.vo.ReportBatchRequestVo;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class ReportControllerDelegate {
    private IRestApiService restApiService;
    private PropertyLoader propertyLoader;

    @Inject
    public ReportControllerDelegate(final IRestApiService restApiService,
		                    final PropertyLoader propertyLoader) {
        this.restApiService = restApiService;
	this.propertyLoader = propertyLoader;
    }

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

        if (!this.propertyLoader.getDisableCheckToken()) {
            this.checkValidityToken(reportBatchRequestVo.getToken());
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

    private void checkValidityToken(String token) throws RobertServerException {
        if (StringUtils.isEmpty(token)) {
            log.warn("No token provided");
            throw new RobertServerBadRequestException(MessageConstants.INVALID_DATA.getValue());
        }

        Optional<VerifyResponseDto> response = this.restApiService.verifyReportToken(token);

        if (!response.isPresent() ||  !response.get().isValid()) {
            log.warn("Verifying the token failed");
            throw new RobertServerUnauthorizedException(MessageConstants.INVALID_AUTHENTICATION.getValue());
        }

        log.info("Verifying the token succeeded");
    }

}
