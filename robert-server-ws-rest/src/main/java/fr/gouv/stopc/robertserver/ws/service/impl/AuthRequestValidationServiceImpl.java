package fr.gouv.stopc.robertserver.ws.service.impl;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.*;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.common.RobertClock;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.ws.config.RobertWsProperties;
import fr.gouv.stopc.robertserver.ws.service.AuthRequestValidationService;
import fr.gouv.stopc.robertserver.ws.vo.AuthRequestVo;
import fr.gouv.stopc.robertserver.ws.vo.StatusVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthRequestValidationServiceImpl implements AuthRequestValidationService {

    @Value("${robert.server.request-time-delta-tolerance:60}")
    private Integer timeDeltaTolerance;

    private final IServerConfigurationService serverConfigurationService;

    private final ICryptoServerGrpcClient cryptoServerClient;

    private final IRegistrationService registrationService;

    private final RobertWsProperties robertWsProperties;

    private final RobertClock clock;

    private ResponseEntity createErrorValidationFailed() {
        log.info("Discarding authenticated request because validation failed");
        return ResponseEntity.badRequest().build();
    }

    private ResponseEntity createErrorTechnicalIssue() {
        log.info("Technical issue managing authenticated request");
        return ResponseEntity.badRequest().build();
    }

    private Optional<ResponseEntity> createErrorBadRequestCustom(String customErrorMessage) {
        log.info(customErrorMessage);
        return Optional.of(ResponseEntity.badRequest().build());
    }

    private Optional<ResponseEntity> validateCommonAuth(AuthRequestVo authRequestVo) {
        // Step #1: Parameter check
        if (Objects.isNull(authRequestVo)) {
            return createErrorBadRequestCustom("Discarding authenticated request because of empty request body");
        }

        byte[] ebid = Base64.getDecoder().decode(authRequestVo.getEbid());
        if (ByteUtils.isEmpty(ebid) || ebid.length != 8) {
            return createErrorBadRequestCustom("Discarding authenticated request because of invalid EBID field size");
        }

        byte[] time = Base64.getDecoder().decode(authRequestVo.getTime());
        if (ByteUtils.isEmpty(time) || time.length != 4) {
            return createErrorBadRequestCustom("Discarding authenticated request because of invalid Time field size");
        }

        byte[] mac = Base64.getDecoder().decode(authRequestVo.getMac());
        if (ByteUtils.isEmpty(mac) || mac.length != 32) {
            return createErrorBadRequestCustom("Discarding authenticated request because of invalid MAC field size");
        }

        // Moved timestamp difference check to after request sent to cryptoserver to be
        // able to store drift in db

        return Optional.empty();
    }

    @Override
    public ValidationResult<GetIdFromAuthResponse> validateRequestForAuth(AuthRequestVo authRequestVo,
            DigestSaltEnum requestType) {
        Optional<ResponseEntity> validationError = validateCommonAuth(authRequestVo);

        if (validationError.isPresent()) {
            return ValidationResult.<GetIdFromAuthResponse>builder().error(validationError.get()).build();
        }

        try {
            GetIdFromAuthRequest request = GetIdFromAuthRequest.newBuilder()
                    .setEbid(ByteString.copyFrom(Base64.getDecoder().decode(authRequestVo.getEbid())))
                    .setEpochId(authRequestVo.getEpochId())
                    .setTime(
                            Integer.toUnsignedLong(
                                    ByteUtils.bytesToInt(Base64.getDecoder().decode(authRequestVo.getTime()))
                            )
                    )
                    .setMac(ByteString.copyFrom(Base64.getDecoder().decode(authRequestVo.getMac())))
                    .setRequestType(requestType.getValue())
                    .build();

            Optional<GetIdFromAuthResponse> response = this.cryptoServerClient.getIdFromAuth(request);

            final var timeValidation = validateTimeDrift(authRequestVo);
            if (response.isPresent()) {
                updateRegistrationTimeDrift(response.get().getIdA(), timeValidation.timeDrift);
                if (timeValidation.error != null) {
                    return ValidationResult.<GetIdFromAuthResponse>builder().error(timeValidation.error).build();
                }
                return ValidationResult.<GetIdFromAuthResponse>builder().response(response.get()).build();
            } else {
                return ValidationResult.<GetIdFromAuthResponse>builder().error(createErrorValidationFailed()).build();
            }
        } catch (Exception e1) {
            return ValidationResult.<GetIdFromAuthResponse>builder().error(createErrorTechnicalIssue()).build();
        }
    }

    @Override
    public ValidationResult<DeleteIdResponse> deleteId(AuthRequestVo authRequestVo) {
        Optional<ResponseEntity> validationError = validateCommonAuth(authRequestVo);

        if (validationError.isPresent()) {
            return ValidationResult.<DeleteIdResponse>builder().error(validationError.get()).build();
        }

        try {
            DeleteIdRequest request = DeleteIdRequest.newBuilder()
                    .setEbid(ByteString.copyFrom(Base64.getDecoder().decode(authRequestVo.getEbid())))
                    .setEpochId(authRequestVo.getEpochId())
                    .setTime(
                            Integer.toUnsignedLong(
                                    ByteUtils.bytesToInt(Base64.getDecoder().decode(authRequestVo.getTime()))
                            )
                    )
                    .setMac(ByteString.copyFrom(Base64.getDecoder().decode(authRequestVo.getMac())))
                    .build();

            Optional<DeleteIdResponse> response = this.cryptoServerClient.deleteId(request);

            final var timeValidation = validateTimeDrift(authRequestVo);
            if (response.isPresent()) {
                updateRegistrationTimeDrift(response.get().getIdA(), timeValidation.timeDrift);
                if (timeValidation.error != null) {
                    return ValidationResult.<DeleteIdResponse>builder().error(timeValidation.error).build();
                }
                return ValidationResult.<DeleteIdResponse>builder().response(response.get()).build();
            } else {
                return ValidationResult.<DeleteIdResponse>builder().error(createErrorValidationFailed()).build();
            }
        } catch (Exception e1) {
            return ValidationResult.<DeleteIdResponse>builder().error(createErrorTechnicalIssue()).build();
        }
    }

    @Override
    public ValidationResult<GetIdFromStatusResponse> validateStatusRequest(StatusVo statusVo) {
        Optional<ResponseEntity> validationError = validateCommonAuth(statusVo);

        if (validationError.isPresent()) {
            return ValidationResult.<GetIdFromStatusResponse>builder().error(validationError.get()).build();
        }

        try {
            GetIdFromStatusRequest request = GetIdFromStatusRequest.newBuilder()
                    .setEbid(ByteString.copyFrom(Base64.getDecoder().decode(statusVo.getEbid())))
                    .setEpochId(statusVo.getEpochId())
                    .setTime(
                            Integer.toUnsignedLong(ByteUtils.bytesToInt(Base64.getDecoder().decode(statusVo.getTime())))
                    )
                    .setMac(ByteString.copyFrom(Base64.getDecoder().decode(statusVo.getMac())))
                    .setFromEpochId(
                            TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart())
                    )
                    .setNumberOfDaysForEpochBundles(robertWsProperties.getEpochBundleDurationInDays())
                    .setServerCountryCode(
                            ByteString.copyFrom(new byte[] { this.serverConfigurationService.getServerCountryCode() })
                    )
                    .build();

            Optional<GetIdFromStatusResponse> response = this.cryptoServerClient.getIdFromStatus(request);

            final var timeValidation = validateTimeDrift(statusVo);
            if (response.isPresent()) {
                updateRegistrationTimeDrift(response.get().getIdA(), timeValidation.timeDrift);
                if (timeValidation.error != null) {
                    return ValidationResult.<GetIdFromStatusResponse>builder().error(timeValidation.error).build();
                }
                return ValidationResult.<GetIdFromStatusResponse>builder().response(response.get()).build();
            } else {
                return ValidationResult.<GetIdFromStatusResponse>builder().error(createErrorValidationFailed()).build();
            }
        } catch (Exception e1) {
            return ValidationResult.<GetIdFromStatusResponse>builder().error(createErrorTechnicalIssue()).build();
        }
    }

    private TimeValidationResult validateTimeDrift(final AuthRequestVo authRequestVo) {
        final var timeAs32bitsTimestamp = Base64.getDecoder().decode(authRequestVo.getTime());
        final var serverTime = clock.now();
        final var clientTime = clock.atTime32(timeAs32bitsTimestamp);
        final var timeDrift = clientTime.until(serverTime);
        if (timeDrift.abs().toSeconds() > this.timeDeltaTolerance) {
            log.warn(
                    "Witnessing abnormal time difference {} between client: {} and server: {}", timeDrift.toSeconds(),
                    clientTime, serverTime
            );
            final var error = createErrorBadRequestCustom(
                    "Discarding authenticated request because provided time is too far from current server time"
            )
                    .orElseThrow();
            return new TimeValidationResult(timeDrift, error);
        }
        return new TimeValidationResult(timeDrift, null);
    }

    private void updateRegistrationTimeDrift(ByteString idA, Duration timeDrift) {
        registrationService.findById(idA.toByteArray())
                .stream()
                .peek(r -> r.setLastTimestampDrift(timeDrift.toSeconds()))
                .forEach(registrationService::saveRegistration);
    }

    @lombok.Value
    private static class TimeValidationResult {

        Duration timeDrift;

        ResponseEntity<?> error;
    }
}
