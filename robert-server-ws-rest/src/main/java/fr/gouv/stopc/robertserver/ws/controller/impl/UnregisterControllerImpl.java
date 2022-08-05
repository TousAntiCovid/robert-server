package fr.gouv.stopc.robertserver.ws.controller.impl;

import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.ws.controller.IUnregisterController;
import fr.gouv.stopc.robertserver.ws.dto.UnregisterResponseDto;
import fr.gouv.stopc.robertserver.ws.service.AuthRequestValidationService;
import fr.gouv.stopc.robertserver.ws.service.IRestApiService;
import fr.gouv.stopc.robertserver.ws.vo.UnregisterRequestVo;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnregisterControllerImpl implements IUnregisterController {

    private final IRegistrationService registrationService;

    private final AuthRequestValidationService authRequestValidationService;

    private final IRestApiService restApiService;

    @Override
    public ResponseEntity<UnregisterResponseDto> unregister(UnregisterRequestVo unregisterRequestVo) {
        final var validationResult = authRequestValidationService
                .validateRequestForAuth(unregisterRequestVo, DigestSaltEnum.UNREGISTER);

        if (Objects.nonNull(validationResult.getResponse()) &&
                List.of(404, 430).contains(validationResult.getResponse().getError().getCode())) {

            return ResponseEntity.status(430).build();
        }

        if (Objects.nonNull(validationResult.getError()) || validationResult.getResponse().hasError()) {
            return ResponseEntity.badRequest().build();
        }

        final var authResponse = validationResult.getResponse();

        final var registrationRecord = this.registrationService
                .findById(authResponse.getIdA().toByteArray());

        if (registrationRecord.isPresent()) {
            final var record = registrationRecord.get();

            // Unregister by deleting
            this.registrationService.delete(record);
            authRequestValidationService.deleteId(unregisterRequestVo);

            final var response = UnregisterResponseDto.builder().success(true).build();

            if (StringUtils.isNotBlank(unregisterRequestVo.getPushToken())) {
                this.restApiService.unregisterPushNotif(unregisterRequestVo.getPushToken());
            }

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
