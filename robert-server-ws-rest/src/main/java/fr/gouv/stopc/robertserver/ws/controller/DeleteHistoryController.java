package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthResponse;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.ws.dto.DeleteHistoryResponseDto;
import fr.gouv.stopc.robertserver.ws.service.AuthRequestValidationService;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.DeleteHistoryRequestVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = {"${controller.path.prefix}" + UriConstants.API_V2,
        "${controller.path.prefix}" + UriConstants.API_V3, "${controller.path.prefix}" + UriConstants.API_V4,
        "${controller.path.prefix}" + UriConstants.API_V5, "${controller.path.prefix}" + UriConstants.API_V6 })
@Consumes(MediaType.APPLICATION_JSON_VALUE)
@Produces(MediaType.APPLICATION_JSON_VALUE)
public class DeleteHistoryController {

    private final IRegistrationService registrationService;

    private final AuthRequestValidationService authRequestValidationService;

    @PostMapping(value = UriConstants.DELETE_HISTORY)
    public ResponseEntity<DeleteHistoryResponseDto> deleteHistory(DeleteHistoryRequestVo deleteHistoryRequestVo) {
        log.info("Receiving delete exposure history request");

        AuthRequestValidationService.ValidationResult<GetIdFromAuthResponse> validationResult = this.authRequestValidationService
                .validateRequestForAuth(deleteHistoryRequestVo, DigestSaltEnum.DELETE_HISTORY);

        if (Objects.nonNull(validationResult.getResponse()) &&
                validationResult.getResponse().getError().getCode() == 430) {

            return ResponseEntity.status(430).build();
        }

        if (Objects.nonNull(validationResult.getError()) || validationResult.getResponse().hasError()) {
            log.info("Delete exposure history request authentication failed");
            return ResponseEntity.badRequest().build();
        }
        log.info("Delete exposure history request authentication passed");

        GetIdFromAuthResponse authResponse = validationResult.getResponse();
        Optional<Registration> registrationRecord = this.registrationService
                .findById(authResponse.getIdA().toByteArray());

        if (registrationRecord.isPresent()) {
            Registration record = registrationRecord.get();

            // Clear ExposedEpoch list then save the updated registration
            if (!CollectionUtils.isEmpty(record.getExposedEpochs())) {
                record.getExposedEpochs().clear();
                registrationService.saveRegistration(record);
            }

            log.info("Delete exposure history request successful");
            return ResponseEntity.ok(DeleteHistoryResponseDto.builder().success(true).build());
        } else {
            log.info("Discarding delete exposure history request because id unknown (fake or was deleted)");
            return ResponseEntity.notFound().build();
        }
    }
}
