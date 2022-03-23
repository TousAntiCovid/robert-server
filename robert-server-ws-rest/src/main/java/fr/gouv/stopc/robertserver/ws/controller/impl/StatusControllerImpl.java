package fr.gouv.stopc.robertserver.ws.controller.impl;

import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromStatusResponse;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.ApplicationConfigurationModel;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IApplicationConfigService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.database.service.impl.StatisticsService;
import fr.gouv.stopc.robertserver.ws.config.WsServerConfiguration;
import fr.gouv.stopc.robertserver.ws.controller.IStatusController;
import fr.gouv.stopc.robertserver.ws.dto.*;
import fr.gouv.stopc.robertserver.ws.dto.declaration.GenerateDeclarationTokenRequest;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.service.AuthRequestValidationService;
import fr.gouv.stopc.robertserver.ws.service.DeclarationService;
import fr.gouv.stopc.robertserver.ws.service.IRestApiService;
import fr.gouv.stopc.robertserver.ws.utils.PropertyLoader;
import fr.gouv.stopc.robertserver.ws.vo.StatusVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.internal.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.Instant.*;
import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusControllerImpl implements IStatusController {

    private final IServerConfigurationService serverConfigurationService;

    private final IRegistrationService registrationService;

    private final IApplicationConfigService applicationConfigService;

    private final AuthRequestValidationService authRequestValidationService;

    private final PropertyLoader propertyLoader;

    private final WsServerConfiguration wsServerConfiguration;

    private final IRestApiService restApiService;

    private final DeclarationService declarationService;

    private final StatisticsService statisticsService;

    @Override
    public ResponseEntity<StatusResponseDtoV1ToV4> getStatusV1ToV4(@Valid StatusVo statusVo)
            throws RobertServerException {

        ResponseEntity<StatusResponseDto> statusResponse = this.getStatus(statusVo);

        if (statusResponse.getStatusCodeValue() == 430) {
            log.warn("Status HTTP response code is equal to : {}", statusResponse.getStatusCodeValue());
            return ResponseEntity.status(430).build();
        }
        if (Objects.isNull(statusResponse) || Objects.isNull(statusResponse.getStatusCodeValue())) {
            log.error("The response of the status must not be null");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        if (statusResponse.getStatusCode().isError()) {
            log.warn("Status HTTP response code is equal to : {}", statusResponse.getStatusCode());
            return ResponseEntity.status(statusResponse.getStatusCode()).build();
        }

        StatusResponseDto status = statusResponse.getBody();
        return ResponseEntity.ok(
                StatusResponseDtoV1ToV4.builder()
                        .atRisk(status.getRiskLevel() != RiskLevel.NONE)
                        .config(status.getConfig())
                        .tuples(status.getTuples())
                        .build()
        );
    }

    @Override
    public ResponseEntity<StatusResponseDtoV5> getStatusV5(@Valid StatusVo statusVo) throws RobertServerException {

        ResponseEntity<StatusResponseDto> statusResponse = this.getStatus(statusVo);

        if (statusResponse.getStatusCodeValue() == 430) {
            log.warn("Status HTTP response code is equal to : {}", statusResponse.getStatusCodeValue());
            return ResponseEntity.status(430).build();
        }

        if (Objects.isNull(statusResponse) || Objects.isNull(statusResponse.getStatusCodeValue())) {
            log.error("The response of the status must not be null");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        if (statusResponse.getStatusCode().isError()) {
            log.warn("Status HTTP response code is equal to : {}", statusResponse.getStatusCodeValue());
            return ResponseEntity.status(statusResponse.getStatusCodeValue()).build();
        }

        StatusResponseDto status = statusResponse.getBody();
        return ResponseEntity.ok(
                StatusResponseDtoV5.builder()
                        .riskLevel(status.getRiskLevel())
                        .config(status.getConfig())
                        .tuples(status.getTuples())
                        .declarationToken(status.getDeclarationToken())
                        .lastContactDate(status.getLastContactDate())
                        .lastRiskScoringDate(status.getLastRiskScoringDate())
                        .build()
        );
    }

    @Override
    public ResponseEntity<StatusResponseDto> getStatus(StatusVo statusVo) {
        AuthRequestValidationService.ValidationResult<GetIdFromStatusResponse> validationResult = this.authRequestValidationService
                .validateStatusRequest(statusVo);

        if (Objects.nonNull(validationResult.getResponse()) &&
                validationResult.getResponse().getError().getCode() == 430) {

            return ResponseEntity.status(430).build();
        }

        if (Objects.nonNull(validationResult.getError())) {
            log.info("Status request authentication failed");
            return ResponseEntity.badRequest().build();
        }

        GetIdFromStatusResponse response = validationResult.getResponse();

        if (response.hasError()) {
            this.logErrorInDatabaseIfIdIsProvided(response);
            return ResponseEntity.badRequest().build();
        }

        Optional<Registration> registration = this.registrationService.findById(response.getIdA().toByteArray());
        if (registration.isPresent()) {
            try {
                Optional<ResponseEntity<StatusResponseDto>> responseEntity = this
                        .validate(registration.get(), response.getEpochId(), response.getTuples().toByteArray());

                if (!registration.get().isNotifiedForCurrentRisk() && registration.get().isAtRisk()) {
                    statisticsService.getByDate(now().truncatedTo(DAYS))
                            .ifPresent((statisticsService::incrementNotifiedTotal));
                    registration.get().setNotifiedForCurrentRisk(true);
                    registrationService.saveRegistration(registration.get());
                }

                if (responseEntity.isPresent()) {

                    Optional.ofNullable(statusVo.getPushInfo())
                            .filter(push -> Objects.nonNull(responseEntity.get().getStatusCode()))
                            .filter(push -> responseEntity.get().getStatusCode().equals(HttpStatus.OK))
                            .ifPresent(this.restApiService::registerPushNotif);

                    return responseEntity.get();
                } else {
                    log.info("Status request failed validation");
                    return ResponseEntity.badRequest().build();
                }
            } catch (RobertServerException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            log.info("Discarding status request because id unknown (fake or was deleted)");
            return ResponseEntity.notFound().build();
        }
    }

    protected void logErrorInDatabaseIfIdIsProvided(GetIdFromStatusResponse response) {
        if (Objects.isNull(response.getIdA()))
            return;

        Optional<Registration> record = this.registrationService.findById(response.getIdA().toByteArray());
        if (!record.isPresent())
            return;

        int currentEpoch = TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());
        Registration registration = record.get();
        registration.setLastFailedStatusRequestEpoch(currentEpoch);
        registration.setLastFailedStatusRequestMessage(response.getError().getDescription());
        this.registrationService.saveRegistration(registration);
    }

    public Optional<ResponseEntity<StatusResponseDto>> validate(Registration record, int epoch, byte[] tuples)
            throws RobertServerException {
        if (Objects.isNull(record)) {
            return Optional.empty();
        }

        // Step #6: Check if user was already notified
        // Not applicable anymore (spec update)

        // Step #7: Check that epochs are not too distant
        int currentEpoch = TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());
        int epochDistance = currentEpoch - record.getLastStatusRequestEpoch();
        if (epochDistance < this.wsServerConfiguration.getStatusRequestMinimumEpochGap()
                && this.propertyLoader.getEsrLimit() != 0) {

            String message = "Discarding ESR request because epochs are too close:";
            String errorMessage = String.format(
                    "%s"
                            + " last ESR request epoch %d vs current epoch %d => %d < %d (tolerance)",
                    message,
                    record.getLastStatusRequestEpoch(),
                    currentEpoch,
                    epochDistance,
                    this.wsServerConfiguration.getStatusRequestMinimumEpochGap()
            );

            log.info(
                    "{} {} < {} (tolerance)",
                    message,
                    epochDistance,
                    this.wsServerConfiguration.getStatusRequestMinimumEpochGap()
            );

            record.setLastFailedStatusRequestEpoch(currentEpoch);
            record.setLastFailedStatusRequestMessage(errorMessage);

            this.registrationService.saveRegistration(record);
            return Optional.of(ResponseEntity.badRequest().build());
        }

        if (epochDistance < 0) {
            log.warn(
                    "The ESR request epoch difference is negative {}, "
                            + "because the last ESR request epoch is {} and currentEpoch is {} ",
                    epochDistance,
                    record.getLastStatusRequestEpoch(),
                    currentEpoch
            );
        }

        // Request is valid
        // (now iterating through steps from section "If the ESR_REQUEST_A,i is valid,
        // the server:", p11 of spec)
        // Step #1: Set StatusRequestEpoch with current epoch number
        int previousLastStatusRequestEpoch = record.getLastStatusRequestEpoch();
        record.setLastStatusRequestEpoch(currentEpoch);
        log.info(
                "The registration previous last status epoch request was {} and the next epoch, {}, will be the current epoch {}",
                previousLastStatusRequestEpoch,
                record.getLastStatusRequestEpoch(),
                currentEpoch
        );

        // Step #2: Risk and score were processed during batch, simple lookup
        RiskLevel riskLevel = record.isAtRisk() ? RiskLevel.HIGH : RiskLevel.NONE;

        // Include new EBIDs and ECCs for next M epochs
        StatusResponseDto statusResponse = StatusResponseDto.builder()
                .riskLevel(riskLevel)
                .config(this.getClientConfig())
                .tuples(Base64.encode(tuples))
                .build();

        // Step #3: Set UserNotified to true if at risk
        // If was never notified and batch flagged a risk, notify
        // and remember last exposed epoch as new starting point for subsequent risk
        // notifications
        // The status atRisk will be reinitialized by the batch
        if (riskLevel != RiskLevel.NONE) {
            record.setNotified(true);

            // Include lastContactDate only if any and if user is evaluated at risk
            if (record.getLastContactTimestamp() > 0) {
                statusResponse.setLastContactDate(Long.toString(record.getLastContactTimestamp()));
            }

            // Include lastRiskScoringDate only if any and if user is evaluated at risk
            if (record.getLatestRiskEpoch() > 0) {
                long serviceTimeStart = serverConfigurationService.getServiceTimeStart();
                statusResponse.setLastRiskScoringDate(
                        Long.toString(TimeUtils.getNtpSeconds(record.getLatestRiskEpoch(), serviceTimeStart))
                );
            }
        }

        // Generate declaration token (for CNAM) and atRisk token (for Analytics)
        if (!RiskLevel.NONE.equals(riskLevel) && record.getLastContactTimestamp() > 0) {
            long lastStatusRequestTimestamp = TimeUtils.getNtpSeconds(
                    record.getLastStatusRequestEpoch(),
                    serverConfigurationService.getServiceTimeStart()
            );
            GenerateDeclarationTokenRequest request = GenerateDeclarationTokenRequest.builder()
                    .technicalApplicationIdentifier(Base64.encode(record.getPermanentIdentifier()))
                    .lastContactDateTimestamp(record.getLastContactTimestamp())
                    .riskLevel(riskLevel)
                    .lastStatusRequestTimestamp(lastStatusRequestTimestamp)
                    .latestRiskEpoch(record.getLatestRiskEpoch())
                    .build();
            String declarationToken = declarationService.generateDeclarationToken(request).orElse(null);
            statusResponse.setDeclarationToken(declarationToken);
            log.debug("Declaration token generated : {}", declarationToken);

        }

        String analyticsToken = declarationService.generateAnalyticsToken().orElse(null);
        statusResponse.setAnalyticsToken(analyticsToken);
        log.debug("analytics token generated : {}", analyticsToken);

        // Save changes to the record
        this.registrationService.saveRegistration(record);

        return Optional.of(ResponseEntity.ok(statusResponse));
    }

    private List<ClientConfigDto> getClientConfig() {
        List<ApplicationConfigurationModel> serverConf = this.applicationConfigService.findAll();
        if (CollectionUtils.isEmpty(serverConf)) {
            return Collections.emptyList();
        } else {
            return serverConf
                    .stream()
                    .map(item -> ClientConfigDto.builder().name(item.getName()).value(item.getValue()).build())
                    .collect(Collectors.toList());
        }
    }
}
