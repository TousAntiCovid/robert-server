package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdResponse;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.ws.dto.UnregisterResponseDto;
import fr.gouv.stopc.robertserver.ws.service.AuthRequestValidationService;
import fr.gouv.stopc.robertserver.ws.service.IRestApiService;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.UnregisterRequestVo;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class UnregisterController {

    private final IRegistrationService registrationService;

    private final AuthRequestValidationService authRequestValidationService;

    private final IRestApiService restApiService;

    @PostMapping(value = UriConstants.UNREGISTER)
    public ResponseEntity<UnregisterResponseDto> unregister(UnregisterRequestVo unregisterRequestVo) {
        AuthRequestValidationService.ValidationResult<DeleteIdResponse> validationResult = authRequestValidationService
                .validateRequestForUnregister(unregisterRequestVo);

        if (Objects.nonNull(validationResult.getResponse()) &&
                validationResult.getResponse().getError().getCode() == 430) {

            return ResponseEntity.status(430).build();
        }

        if (Objects.nonNull(validationResult.getError()) || validationResult.getResponse().hasError()) {
            return ResponseEntity.badRequest().build();
        }

        DeleteIdResponse authResponse = validationResult.getResponse();

        Optional<Registration> registrationRecord = this.registrationService
                .findById(authResponse.getIdA().toByteArray());

        if (registrationRecord.isPresent()) {
            Registration record = registrationRecord.get();

            // Unregister by deleting
            this.registrationService.delete(record);

            UnregisterResponseDto response = UnregisterResponseDto.builder().success(true).build();

            if (StringUtils.isNotBlank(unregisterRequestVo.getPushToken())) {
                this.restApiService.unregisterPushNotif(unregisterRequestVo.getPushToken());
            }

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
