package fr.gouv.stopc.robert.crypto.grpc.server.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.exception.NoServerKeyFoundException;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.*;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplImplBase;
import fr.gouv.stopc.robert.crypto.grpc.server.service.ICryptoServerConfigurationService;
import fr.gouv.stopc.robert.crypto.grpc.server.service.IECDHKeyService;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.cryptographic.service.ICryptographicStorageService;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.model.ClientIdentifierBundle;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.service.IClientKeyStorageService;
import fr.gouv.stopc.robert.crypto.grpc.server.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.callable.TupleGenerator;
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.server.crypto.model.EphemeralTuple;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESECB;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoHMACSHA256;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64;
import io.grpc.stub.StreamObserver;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;

import java.security.Key;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CryptoGrpcServiceBaseImpl extends CryptoGrpcServiceImplImplBase {

    private final ICryptoServerConfigurationService serverConfigurationService;

    private final CryptoService cryptoService;

    private final IECDHKeyService keyService;

    private final IClientKeyStorageService clientStorageService;

    private final ICryptographicStorageService cryptographicStorageService;

    private final PropertyLoader propertyLoader;

    @Inject
    public CryptoGrpcServiceBaseImpl(final ICryptoServerConfigurationService serverConfigurationService,
            final CryptoService cryptoService,
            final IECDHKeyService keyService,
            final IClientKeyStorageService clientStorageService,
            final ICryptographicStorageService cryptographicStorageService,
            final PropertyLoader propertyLoader) {

        this.serverConfigurationService = serverConfigurationService;
        this.cryptoService = cryptoService;
        this.keyService = keyService;
        this.clientStorageService = clientStorageService;
        this.cryptographicStorageService = cryptographicStorageService;
        this.propertyLoader = propertyLoader;
    }

    @Override
    public void reloadHSM(ReloadHSMRequest request, StreamObserver<ReloadHSMResponse> responseObserver) {

        boolean success = this.cryptographicStorageService.reloadHSM(
                request.getPin(),
                request.getConfigFileName(),
                propertyLoader.getKeystoreType(),
                propertyLoader.getKeystoreFile()
        );

        responseObserver.onNext(
                ReloadHSMResponse.newBuilder()
                        .setSuccess(success)
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void getHSMCacheStatus(HSMCacheStatusRequest request,
            StreamObserver<HSMCacheStatusResponse> responseObserver) {

        List<String> cachedKeys = this.cryptographicStorageService.getHSMCacheStatus();

        responseObserver.onNext(
                HSMCacheStatusResponse.newBuilder()
                        .addAllAliases(cachedKeys)
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void createRegistration(CreateRegistrationRequest request,
            StreamObserver<CreateRegistrationResponse> responseObserver) {

        try {
            // Derive K_A and K_A_Tuples from client public key for the new registration
            Optional<ClientIdentifierBundle> clientIdentifierBundleWithPublicKey = this.keyService
                    .deriveKeysFromClientPublicKey(request.getClientPublicKey().toByteArray());

            if (!clientIdentifierBundleWithPublicKey.isPresent()) {
                String errorMessage = "Unable to derive keys from provided client public key for client registration";
                log.warn(errorMessage);
                responseObserver.onNext(
                        CreateRegistrationResponse.newBuilder()
                                .setError(
                                        ErrorMessage.newBuilder()
                                                .setCode(400)
                                                .setDescription(errorMessage)
                                                .build()
                                )
                                .build()
                );
                responseObserver.onCompleted();
                return;
            }

            Optional<ClientIdentifierBundle> clientIdentifierBundleFromDb = this.clientStorageService
                    .createClientIdUsingKeys(
                            clientIdentifierBundleWithPublicKey.get().getKeyForMac(),
                            clientIdentifierBundleWithPublicKey.get().getKeyForTuples()
                    );

            if (!clientIdentifierBundleFromDb.isPresent()) {
                String errorMessage = "Unable to create a registration";
                log.warn(errorMessage);
                responseObserver.onNext(
                        CreateRegistrationResponse.newBuilder()
                                .setError(
                                        ErrorMessage.newBuilder()
                                                .setCode(500)
                                                .setDescription(errorMessage)
                                                .build()
                                )
                                .build()
                );
                responseObserver.onCompleted();
                return;
            }

            Optional<TuplesGenerationResult> encryptedTuples = generateEncryptedTuples(
                    clientIdentifierBundleFromDb.get().getKeyForTuples(),
                    clientIdentifierBundleFromDb.get().getId(),
                    request.getFromEpochId(),
                    request.getNumberOfDaysForEpochBundles(),
                    request.getServerCountryCode().byteAt(0)
            );

            if (!encryptedTuples.isPresent()) {
                String errorMessage = "Unhandled exception while creating registration";
                log.warn(errorMessage);
                responseObserver.onNext(
                        CreateRegistrationResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(clientIdentifierBundleFromDb.get().getId()))
                                .setError(
                                        ErrorMessage.newBuilder()
                                                .setCode(500)
                                                .setDescription(errorMessage)
                                                .build()
                                )
                                .build()
                );
                responseObserver.onCompleted();
                return;
            }

            CreateRegistrationResponse response = CreateRegistrationResponse
                    .newBuilder()
                    .setIdA(ByteString.copyFrom(clientIdentifierBundleFromDb.get().getId()))
                    .setTuples(ByteString.copyFrom(encryptedTuples.get().getEncryptedTuples()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (RobertServerCryptoException e) {
            String errorMessage = "Unhandled exception while creating registration";
            log.warn(errorMessage, e);
            responseObserver.onNext(
                    CreateRegistrationResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(500)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }
    }

    @Override
    public void getIdFromAuth(GetIdFromAuthRequest request,
            StreamObserver<GetIdFromAuthResponse> responseObserver) {
        DigestSaltEnum digestSalt = DigestSaltEnum.valueOf((byte) request.getRequestType());

        if (Objects.isNull(digestSalt)) {
            String errorMessage = String.format("Unknown request type %d", request.getRequestType());
            log.warn(errorMessage);
            responseObserver.onNext(
                    GetIdFromAuthResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(400)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        Optional<AuthRequestValidationResult> validationResult;
        try {
            validationResult = validateAuthRequest(
                    request.getEbid().toByteArray(),
                    request.getEpochId(),
                    request.getTime(),
                    request.getMac().toByteArray(),
                    digestSalt
            );
        } catch (NoServerKeyFoundException e) {
            log.warn(e.getMessage());
            responseObserver.onNext(
                    GetIdFromAuthResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(430)
                                            .setDescription(e.getMessage())
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        if (!validationResult.isPresent()) {
            String errorMessage = "Could not validate auth request";
            log.warn(errorMessage);
            responseObserver.onNext(
                    GetIdFromAuthResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(400)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        } else if (Objects.nonNull(validationResult.get().getError())) {
            GetIdFromAuthResponse.Builder responseBuilder = GetIdFromAuthResponse.newBuilder()
                    .setEpochId(validationResult.get().getEpochId() != 0 ? validationResult.get().getEpochId() : 0)
                    .setError(validationResult.get().getError());
            if (Objects.nonNull(validationResult.get().getId())) {
                responseBuilder.setIdA(ByteString.copyFrom(validationResult.get().getId()));
            }
            if (validationResult.get().getEpochId() > 0) {
                responseBuilder.setEpochId(validationResult.get().getEpochId());
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            return;
        }

        responseObserver.onNext(
                GetIdFromAuthResponse.newBuilder()
                        .setIdA(ByteString.copyFrom(validationResult.get().getId()))
                        .setEpochId(validationResult.get().getEpochId())
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void getIdFromStatus(GetIdFromStatusRequest request,
            StreamObserver<GetIdFromStatusResponse> responseObserver) {
        Optional<AuthRequestValidationResult> validationResult = null;

        try {
            validationResult = validateAuthRequest(
                    request.getEbid().toByteArray(),
                    request.getEpochId(),
                    request.getTime(),
                    request.getMac().toByteArray(),
                    DigestSaltEnum.STATUS
            );
        } catch (NoServerKeyFoundException e) {
            log.warn(e.getMessage());
            responseObserver.onNext(
                    GetIdFromStatusResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(430)
                                            .setDescription(e.getMessage())
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }
        if (!validationResult.isPresent()) {
            String errorMessage = "Could not validate auth request";
            log.warn(errorMessage);
            responseObserver.onNext(
                    GetIdFromStatusResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(400)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        } else if (Objects.nonNull(validationResult.get().getError())) {
            GetIdFromStatusResponse.Builder responseBuilder = GetIdFromStatusResponse.newBuilder()
                    .setError(validationResult.get().getError());

            if (Objects.nonNull(validationResult.get().getId())) {
                responseBuilder.setIdA(ByteString.copyFrom(validationResult.get().getId()));
            }
            if (validationResult.get().getEpochId() > 0) {
                responseBuilder.setEpochId(validationResult.get().getEpochId());
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            return;
        }

        Optional<ClientIdentifierBundle> clientIdentifierBundle = this.clientStorageService
                .findKeyById(validationResult.get().getId());
        if (!clientIdentifierBundle.isPresent()) {
            String errorMessage = "Unknown id";
            log.warn(errorMessage);
            responseObserver.onNext(
                    GetIdFromStatusResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(404)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        Optional<TuplesGenerationResult> encryptedTuples = generateEncryptedTuples(
                clientIdentifierBundle.get().getKeyForTuples(),
                clientIdentifierBundle.get().getId(),
                request.getFromEpochId(),
                request.getNumberOfDaysForEpochBundles(),
                request.getServerCountryCode().byteAt(0)
        );

        if (!encryptedTuples.isPresent()) {
            String errorMessage = "Unhandled exception while creating tuples";
            log.warn(errorMessage);
            responseObserver.onNext(
                    GetIdFromStatusResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(500)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        responseObserver.onNext(
                GetIdFromStatusResponse.newBuilder()
                        .setIdA(ByteString.copyFrom(validationResult.get().getId()))
                        .setEpochId(validationResult.get().getEpochId())
                        .setTuples(ByteString.copyFrom(encryptedTuples.get().getEncryptedTuples()))
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void validateContact(ValidateContactRequest request,
            StreamObserver<ValidateContactResponse> responseObserver) {

        byte[] idA;
        int epochId;
        byte[] cc;

        try {
            // Decrypt ECC
            cc = decryptECC(request.getEbid().toByteArray(), request.getEcc().byteAt(0));

            // If country code was decrypted successfully but does not match current server,
            // return directly and forward to appropriate federation server
            if (!Arrays.equals(cc, request.getServerCountryCode().toByteArray())) {
                responseObserver.onNext(
                        ValidateContactResponse.newBuilder()
                                .setCountryCode(ByteString.copyFrom(cc))
                                .build()
                );
                responseObserver.onCompleted();
                return;
            }
        } catch (RobertServerCryptoException e) {
            String errorMessage = "Could not decrypt ECC";
            log.warn(errorMessage);
            responseObserver.onNext(
                    ValidateContactResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(400)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        List<HelloMessageDetail> invalidMessageDetails = new ArrayList<>();
        Optional<EbidContent> ebidContent = Optional.empty();

        for (final var helloMessageDetail : request.getHelloMessageDetailsList()) {

            try {
                // Decrypt EBID
                EbidContent currentEbidContent = decryptEBIDWithTimeReceived(
                        request.getEbid().toByteArray(), helloMessageDetail.getTimeReceived()
                );

                if (Objects.isNull(currentEbidContent)) {
                    invalidMessageDetails.add(helloMessageDetail);
                    break;
                }

                if (ebidContent.isPresent()) {
                    if (!Arrays.equals(currentEbidContent.getIdA(), ebidContent.get().getIdA())) {
                        String errorMessage = "Inconsistent ids across hello message of a contact";
                        log.warn(errorMessage);
                        responseObserver.onNext(
                                ValidateContactResponse.newBuilder()
                                        .setError(
                                                ErrorMessage.newBuilder()
                                                        .setCode(500)
                                                        .setDescription(errorMessage)
                                                        .build()
                                        )
                                        .build()
                        );
                        responseObserver.onCompleted();
                        return;
                    }
                    if (currentEbidContent.getEpochId() != ebidContent.get().getEpochId()) {
                        String errorMessage = "Inconsistent epochs across hello message of a contact";
                        log.warn(errorMessage);
                        responseObserver.onNext(
                                ValidateContactResponse.newBuilder()
                                        .setError(
                                                ErrorMessage.newBuilder()
                                                        .setCode(500)
                                                        .setDescription(errorMessage)
                                                        .build()
                                        )
                                        .build()
                        );
                        responseObserver.onCompleted();
                        return;
                    }
                } else {
                    ebidContent = Optional.of(currentEbidContent);
                }
            } catch (RobertServerCryptoException e) {
                String errorMessage = "Could not decrypt EBID due to internal error";
                log.error(errorMessage);
                invalidMessageDetails.add(helloMessageDetail);
                break;
            } catch (NoServerKeyFoundException e) {
                log.warn(e.getMessage());
                invalidMessageDetails.add(helloMessageDetail);
                break;
            }

        }

        if (ebidContent.isEmpty()) {
            String errorMessage = "Could not decrypt EBID";
            responseObserver.onNext(
                    ValidateContactResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(400)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        Optional<ClientIdentifierBundle> clientIdentifierBundle = this.clientStorageService
                .findKeyById(ebidContent.get().getIdA());
        if (!clientIdentifierBundle.isPresent()) {
            String errorMessage = "Could not find keys for id";
            log.warn(errorMessage);
            responseObserver.onNext(
                    ValidateContactResponse.newBuilder()
                            .setIdA(ByteString.copyFrom(ebidContent.get().getIdA()))
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(404)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        List<HelloMessageDetail> remainingMessages = new ArrayList(request.getHelloMessageDetailsList());
        remainingMessages.removeAll(invalidMessageDetails);

        for (final var helloMessageDetail : remainingMessages) {
            // Check MAC
            try {

                boolean macValid = this.cryptoService.macHelloValidation(
                        new CryptoHMACSHA256(clientIdentifierBundle.get().getKeyForMac()),
                        generateHelloFromHelloMessageDetail(
                                request.getEbid().toByteArray(), request.getEcc().toByteArray(), helloMessageDetail
                        )
                );

                if (!macValid) {
                    String errorMessage = "MAC is invalid";
                    log.warn(errorMessage);
                    invalidMessageDetails.add(helloMessageDetail);
                    break;
                }
            } catch (RobertServerCryptoException e) {
                String errorMessage = "Could not validate MAC";
                log.warn(errorMessage, e);
                invalidMessageDetails.add(helloMessageDetail);
                break;
            }

        }

        responseObserver.onNext(
            ValidateContactResponse.newBuilder()
                .setIdA(ByteString.copyFrom(ebidContent.get().getIdA()))
                .setCountryCode(ByteString.copyFrom(cc))
                .setEpochId(ebidContent.get().getEpochId())
                .addAllInvalidHelloMessageDetails(invalidMessageDetails)
                .build()
            );
        responseObserver.onCompleted();

    }

    @Override
    public void getInfoFromHelloMessage(GetInfoFromHelloMessageRequest request,
            StreamObserver<GetInfoFromHelloMessageResponse> responseObserver) {
        byte[] idA;
        int epochId;
        byte[] cc;

        try {
            // Decrypt ECC
            cc = decryptECC(request.getEbid().toByteArray(), request.getEcc().byteAt(0));

            // If country code was decrypted successfully but does not match current server,
            // return directly and forward to appropriate federation server
            if (!Arrays.equals(cc, request.getServerCountryCode().toByteArray())) {
                responseObserver.onNext(
                        GetInfoFromHelloMessageResponse.newBuilder()
                                .setCountryCode(ByteString.copyFrom(cc))
                                .build()
                );
                responseObserver.onCompleted();
                return;
            }
        } catch (RobertServerCryptoException e) {
            String errorMessage = "Could not decrypt ECC";
            log.warn(errorMessage, e);
            responseObserver.onNext(
                    GetInfoFromHelloMessageResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(400)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        try {
            // Decrypt EBID
            EbidContent ebidContent = decryptEBIDWithTimeReceived(
                    request.getEbid().toByteArray(), request.getTimeReceived()
            );

            if (Objects.isNull(ebidContent)) {
                String errorMessage = "Could not decrypt EBID because the server key was not found";
                responseObserver.onNext(
                        GetInfoFromHelloMessageResponse.newBuilder()
                                .setError(
                                        ErrorMessage.newBuilder()
                                                .setCode(400)
                                                .setDescription(errorMessage)
                                                .build()
                                )
                                .build()
                );
                responseObserver.onCompleted();
                return;
            }

            idA = ebidContent.getIdA();
            epochId = ebidContent.getEpochId();
        } catch (RobertServerCryptoException e) {
            String errorMessage = "Could not decrypt EBID due to internal error";
            log.error(errorMessage, e);
            responseObserver.onNext(
                    GetInfoFromHelloMessageResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(500)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        } catch (NoServerKeyFoundException e) {
            log.warn(e.getMessage());
            responseObserver.onNext(
                    GetInfoFromHelloMessageResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(430)
                                            .setDescription(e.getMessage())
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        // Check MAC
        try {
            Optional<ClientIdentifierBundle> clientIdentifierBundle = this.clientStorageService.findKeyById(idA);
            if (!clientIdentifierBundle.isPresent()) {
                String errorMessage = "Could not find keys for id";
                log.warn(errorMessage);
                responseObserver.onNext(
                        GetInfoFromHelloMessageResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .setError(
                                        ErrorMessage.newBuilder()
                                                .setCode(404)
                                                .setDescription(errorMessage)
                                                .build()
                                )
                                .build()
                );
                responseObserver.onCompleted();
                return;
            }
            boolean macValid = this.cryptoService.macHelloValidation(
                    new CryptoHMACSHA256(clientIdentifierBundle.get().getKeyForMac()),
                    generateHelloFromHelloMessageRequest(request)
            );

            if (!macValid) {
                String errorMessage = "MAC is invalid";
                log.warn(errorMessage);
                responseObserver.onNext(
                        GetInfoFromHelloMessageResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .setError(
                                        ErrorMessage.newBuilder()
                                                .setCode(400)
                                                .setDescription(errorMessage)
                                                .build()
                                )
                                .build()
                );
                responseObserver.onCompleted();
                return;
            }
        } catch (RobertServerCryptoException e) {
            String errorMessage = "Could not validate MAC";
            log.warn(errorMessage, e);
            responseObserver.onNext(
                    GetInfoFromHelloMessageResponse.newBuilder()
                            .setIdA(ByteString.copyFrom(idA))
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(500)
                                            .setDescription(errorMessage)
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }

        GetInfoFromHelloMessageResponse response = GetInfoFromHelloMessageResponse.newBuilder()
                .setIdA(ByteString.copyFrom(idA))
                .setEpochId(epochId)
                .setCountryCode(ByteString.copyFrom(cc))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteId(DeleteIdRequest request,
            StreamObserver<DeleteIdResponse> responseObserver) {
        Optional<AuthRequestValidationResult> validationResult;

        try {
            validationResult = validateAuthRequest(
                    request.getEbid().toByteArray(),
                    request.getEpochId(),
                    request.getTime(),
                    request.getMac().toByteArray(),
                    DigestSaltEnum.UNREGISTER
            );
        } catch (NoServerKeyFoundException e) {
            log.warn(e.getMessage());
            responseObserver.onNext(
                    DeleteIdResponse.newBuilder()
                            .setError(
                                    ErrorMessage.newBuilder()
                                            .setCode(430)
                                            .setDescription(e.getMessage())
                                            .build()
                            )
                            .build()
            );
            responseObserver.onCompleted();
            return;
        }
        if (!validationResult.isPresent()) {
            responseObserver.onError(new RobertServerCryptoException("Could not validate auth request"));
            return;
        } else if (Objects.nonNull(validationResult.get().getError())) {
            DeleteIdResponse.Builder responseBuilder = DeleteIdResponse.newBuilder()
                    .setError(validationResult.get().getError());

            if (Objects.nonNull(validationResult.get().getId())) {
                responseBuilder.setIdA(ByteString.copyFrom(validationResult.get().getId()));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            return;
        }

        this.clientStorageService.deleteClientId(validationResult.get().getId());

        responseObserver.onNext(
                DeleteIdResponse.newBuilder()
                        .setIdA(ByteString.copyFrom(validationResult.get().getId()))
                        .build()
        );
        responseObserver.onCompleted();
    }

    private Optional<AuthRequestValidationResult> validateAuthRequest(byte[] encryptedEbid,
            int epochId,
            long time,
            byte[] mac,
            DigestSaltEnum type) throws NoServerKeyFoundException {
        try {
            EbidContent ebidContent = decryptEBIDAndCheckEpoch(encryptedEbid, epochId);

            if (Objects.isNull(ebidContent)) {
                String message = "Could not decrypt ebid content";
                log.warn(message);
                return Optional.of(
                        AuthRequestValidationResult.builder()
                                .error(
                                        ErrorMessage.newBuilder()
                                                .setCode(400)
                                                .setDescription(message)
                                                .build()
                                )
                                .build()
                );
            }

            Optional<ClientIdentifierBundle> clientIdentifierBundle = this.clientStorageService
                    .findKeyById(ebidContent.getIdA());
            if (!clientIdentifierBundle.isPresent()) {
                String message = "Could not find id";
                log.warn(message);
                return Optional.of(
                        AuthRequestValidationResult.builder()
                                .epochId(ebidContent.getEpochId())
                                .id(ebidContent.getIdA())
                                .error(
                                        ErrorMessage.newBuilder()
                                                .setCode(404)
                                                .setDescription(message)
                                                .build()
                                )
                                .build()
                );
            }
            boolean valid = this.cryptoService.macValidationForType(
                    new CryptoHMACSHA256(clientIdentifierBundle.get().getKeyForMac()),
                    addEbidComponents(encryptedEbid, epochId, time),
                    mac,
                    type
            );
            if (valid) {
                return Optional.of(
                        AuthRequestValidationResult.builder()
                                .epochId(ebidContent.getEpochId())
                                .id(ebidContent.getIdA())
                                .build()
                );
            } else {
                String message = "Invalid MAC";
                log.warn(message);
                return Optional.of(
                        AuthRequestValidationResult.builder()
                                .epochId(ebidContent.getEpochId())
                                .id(ebidContent.getIdA())
                                .error(
                                        ErrorMessage.newBuilder()
                                                .setCode(400)
                                                .setDescription(message)
                                                .build()
                                )
                                .build()
                );
            }
        } catch (RobertServerCryptoException e) {
            String message = "Error validating authenticated request";
            log.error(message, e);
            return Optional.of(
                    AuthRequestValidationResult.builder()
                            .error(
                                    ErrorMessage.newBuilder()
                                            .setCode(500)
                                            .setDescription(message)
                                            .build()
                            )
                            .build()
            );
        }
    }

    @Builder
    @Getter
    @AllArgsConstructor
    private static class AuthRequestValidationResult {

        private byte[] id;

        private int epochId;

        private ErrorMessage error;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    private static class TuplesGenerationResult {

        byte[] encryptedTuples;
    }

    // The two following classes are used to serialize to a JSON string that
    // complies with the API Spec
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class EphemeralTupleJson {

        private int epochId;

        private EphemeralTupleEbidEccJson key;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class EphemeralTupleEbidEccJson {

        private byte[] ebid;

        private byte[] ecc;
    }

    @Builder
    @AllArgsConstructor
    @Getter
    private static class EbidContent {

        byte[] idA;

        int epochId;
    }

    /**
     * Decrypt the provided ebid and check the authRequestEpoch it contains the
     * provided one or the next/previous
     * 
     * @param ebid
     * @param authRequestEpoch
     * @param enableEpochOverlapping authorize the epoch overlapping (ie too close
     *                               epochs => (Math.abs(epoch1 - epoch2) == 1))
     * @param adjacentEpochMatchEnum
     * @return
     * @throws RobertServerCryptoException
     */
    private EbidContent decryptEBIDAndCheckEpoch(byte[] ebid,
            int authRequestEpoch,
            boolean mustCheckWithPreviousDayKey,
            boolean ksAdjustment,
            boolean enableEpochOverlapping,
            AdjacentEpochMatchEnum adjacentEpochMatchEnum)
            throws RobertServerCryptoException, NoServerKeyFoundException {

        byte[] serverKey = this.cryptographicStorageService.getServerKey(
                authRequestEpoch,
                this.serverConfigurationService.getServiceTimeStart(),
                mustCheckWithPreviousDayKey
        );

        if (Objects.isNull(serverKey)) {
            throw new NoServerKeyFoundException("No server key found from cryptographic storage : " + authRequestEpoch);
        }

        byte[] decryptedEbid = this.cryptoService.decryptEBID(new CryptoSkinny64(serverKey), ebid);
        byte[] idA = getIdFromDecryptedEBID(decryptedEbid);
        int ebidEpochId = getEpochIdFromDecryptedEBID(decryptedEbid);

        if (authRequestEpoch != ebidEpochId) {
            log.info(
                    "Epoch from EBID and accompanying authRequestEpoch do not match: ebid epoch = {} vs auth request epoch = {}",
                    ebidEpochId, authRequestEpoch
            );

            if (enableEpochOverlapping && (Math.abs(authRequestEpoch - ebidEpochId) == 1)) {
                return EbidContent.builder().epochId(ebidEpochId).idA(idA).build();
            } else if (ksAdjustment && !mustCheckWithPreviousDayKey) {
                log.warn("Retry to decrypt EBID with previous day key");
                return decryptEBIDAndCheckEpoch(
                        ebid,
                        authRequestEpoch,
                        true,
                        false,
                        enableEpochOverlapping, adjacentEpochMatchEnum
                );
            } else {
                return manageEBIDDecryptRetry(
                        ebid,
                        authRequestEpoch,
                        adjacentEpochMatchEnum
                );
            }
        }

        return EbidContent.builder().epochId(ebidEpochId).idA(idA).build();
    }

    private final static int MAX_EPOCH_DOUBLE_KS_CHECK = 672;

    private boolean isEBIDWithinRange(int epoch) {
        return epoch >= 0 && epoch <= MAX_EPOCH_DOUBLE_KS_CHECK;
    }

    /**
     * Decrypt the provided ebid and check the epoch it contains matches exactly the
     * provided one
     *
     * @param ebid
     * @param epoch
     * @return
     * @throws RobertServerCryptoException
     */
    private EbidContent decryptEBIDAndCheckEpoch(byte[] ebid, int epoch)
            throws RobertServerCryptoException, NoServerKeyFoundException {
        return decryptEBIDAndCheckEpoch(
                ebid,
                epoch,
                false,
                isEBIDWithinRange(epoch),
                false, AdjacentEpochMatchEnum.NONE
        );
    }

    private EbidContent manageEBIDDecryptRetry(byte[] ebid, int authRequestEpoch,
            AdjacentEpochMatchEnum adjacentEpochMatchEnum)
            throws RobertServerCryptoException, NoServerKeyFoundException {
        switch (adjacentEpochMatchEnum) {
            case PREVIOUS:
                log.warn("Retrying ebid decrypt with previous epoch");
                return decryptEBIDAndCheckEpoch(
                        ebid, authRequestEpoch - 1, false, false, false, AdjacentEpochMatchEnum.NONE
                );
            case NEXT:
                log.warn("Retrying ebid decrypt with next epoch");
                return decryptEBIDAndCheckEpoch(
                        ebid, authRequestEpoch + 1, false, false, false, AdjacentEpochMatchEnum.NONE
                );
            case NONE:
            default:
                log.warn(
                        "Epochs do not match for authRequestEpoch = {} and adjacentEpochMatchEnum = {}",
                        authRequestEpoch, adjacentEpochMatchEnum
                );
                return null;
        }
    }

    private EbidContent decryptEBIDWithTimeReceived(byte[] ebid, long timeReceived)
            throws RobertServerCryptoException, NoServerKeyFoundException {
        int epoch = TimeUtils.getNumberOfEpochsBetween(
                this.serverConfigurationService.getServiceTimeStart(),
                timeReceived
        );

        return decryptEBIDAndCheckEpoch(
                ebid,
                epoch,
                false,
                isEBIDWithinRange(epoch),
                true, atStartOrEndOfDay(timeReceived)
        );

    }

    private AdjacentEpochMatchEnum atStartOrEndOfDay(long timeReceived) {
        ZonedDateTime zonedDateTime = Instant
                .ofEpochMilli(TimeUtils.convertNTPSecondsToUnixMillis(timeReceived))
                .atZone(ZoneOffset.UTC);

        int tolerance = this.propertyLoader.getHelloMessageTimeStampTolerance();

        if (zonedDateTime.getHour() == 0
                && (zonedDateTime.getMinute() * 60 + zonedDateTime.getSecond()) < tolerance) {
            return AdjacentEpochMatchEnum.PREVIOUS;
        } else if (zonedDateTime.getHour() == 23
                && (60 * 60 - (zonedDateTime.getMinute() * 60 + zonedDateTime.getSecond())) < tolerance) {
            return AdjacentEpochMatchEnum.NEXT;
        }

        return AdjacentEpochMatchEnum.NONE;
    }

    private enum AdjacentEpochMatchEnum {
        NONE,
        PREVIOUS,
        NEXT
    }

    private byte[] decryptECC(byte[] ebid, byte encryptedCountryCode) throws RobertServerCryptoException {
        return this.cryptoService.decryptCountryCode(
                new CryptoAESECB(this.cryptographicStorageService.getFederationKey()), ebid, encryptedCountryCode
        );
    }

    private byte[] generateHelloFromHelloMessageRequest(GetInfoFromHelloMessageRequest request) {
        byte[] hello = new byte[16];
        byte[] ecc = request.getEcc().toByteArray();
        byte[] ebid = request.getEbid().toByteArray();
        byte[] mac = request.getMac().toByteArray();
        System.arraycopy(ecc, 0, hello, 0, ecc.length);
        System.arraycopy(ebid, 0, hello, ecc.length, ebid.length);
        System.arraycopy(ByteUtils.intToBytes(request.getTimeSent()), 2, hello, ecc.length + ebid.length, 2);
        System.arraycopy(mac, 0, hello, ecc.length + ebid.length + 2, mac.length);
        return hello;
    }

    private byte[] generateHelloFromHelloMessageDetail(byte[] ebid, byte[] ecc, HelloMessageDetail message) {
        byte[] hello = new byte[16];
        byte[] mac = message.getMac().toByteArray();
        System.arraycopy(ecc, 0, hello, 0, ecc.length);
        System.arraycopy(ebid, 0, hello, ecc.length, ebid.length);
        System.arraycopy(ByteUtils.intToBytes(message.getTimeSent()), 2, hello, ecc.length + ebid.length, 2);
        System.arraycopy(mac, 0, hello, ecc.length + ebid.length + 2, mac.length);
        return hello;
    }

    private byte[] addEbidComponents(byte[] encryptedEbid, int epochId, long time) {
        byte[] all = new byte[encryptedEbid.length + Integer.BYTES + Integer.BYTES];
        System.arraycopy(encryptedEbid, 0, all, 0, encryptedEbid.length);
        System.arraycopy(ByteUtils.intToBytes(epochId), 0, all, encryptedEbid.length, Integer.BYTES);
        System.arraycopy(
                ByteUtils.longToBytes(time),
                4,
                all,
                encryptedEbid.length + Integer.BYTES,
                Integer.BYTES
        );
        return all;
    }

    private java.util.List<EphemeralTupleJson> mapEphemeralTuples(Collection<EphemeralTuple> tuples) {
        ArrayList<EphemeralTupleJson> mappedTuples = new ArrayList<>();

        for (EphemeralTuple tuple : tuples) {
            mappedTuples.add(
                    EphemeralTupleJson.builder()
                            .epochId(tuple.getEpochId())
                            .key(
                                    EphemeralTupleEbidEccJson.builder()
                                            .ebid(tuple.getEbid())
                                            .ecc(tuple.getEncryptedCountryCode())
                                            .build()
                            )
                            .build()
            );
        }
        return mappedTuples;
    }

    private Optional<TuplesGenerationResult> generateEncryptedTuples(byte[] tuplesEncryptionKey,
            byte[] id,
            int epochId,
            int nbDays,
            byte serverCountryCode) {

        if (nbDays < 1) {
            log.error("Request number of epochs is invalid for tuple generation");
            return Optional.empty();
        }

        // TODO: limit generation to a max number of days ?

        // Generate tuples
        final byte[][] serverKeys = this.cryptographicStorageService.getServerKeys(
                epochId,
                this.serverConfigurationService.getServiceTimeStart(),
                nbDays
        );

        if (Objects.isNull(serverKeys)) {
            log.warn("Could not retrieve server keys for epoch span starting with: {}", epochId);
            return Optional.empty();
        }
        int[] nbOfEpochsToGeneratePerDay = new int[serverKeys.length];
        nbOfEpochsToGeneratePerDay[0] = TimeUtils.remainingEpochsForToday(epochId);
        for (int i = 1; i < nbOfEpochsToGeneratePerDay.length; i++) {
            nbOfEpochsToGeneratePerDay[i] = TimeUtils.EPOCHS_PER_DAY;
        }

        Collection<EphemeralTuple> ephemeralTuples = new ArrayList<>();
        final Key federationKey = this.cryptographicStorageService.getFederationKey();
        int offset = 0;
        for (int i = 0; i < nbDays; i++) {
            if (serverKeys[i] != null) {
                final TupleGenerator tupleGenerator = new TupleGenerator(serverKeys[i], federationKey);
                try {
                    Collection<EphemeralTuple> tuplesForDay = tupleGenerator.exec(
                            id,
                            epochId + offset,
                            nbOfEpochsToGeneratePerDay[i],
                            serverCountryCode
                    );
                    tupleGenerator.stop();
                    ephemeralTuples.addAll(tuplesForDay);
                } catch (RobertServerCryptoException e) {
                    log.warn("Error generating tuples for day {}", i, e);
                    // return Optional.empty();
                }
            } else {
                log.warn("Cannot generating tuples for day {}, missing key", i);
            }
            offset += nbOfEpochsToGeneratePerDay[i];
        }
        ephemeralTuples = ephemeralTuples.stream()
                .sorted(Comparator.comparingInt(EphemeralTuple::getEpochId))
                .collect(Collectors.toList());

        if (offset != ephemeralTuples.size()) {
            log.warn("Should have generated {} tuples but only returning {} to client", offset, ephemeralTuples.size());
        }

        try {
            if (!CollectionUtils.isEmpty(ephemeralTuples)) {
                ObjectMapper objectMapper = new ObjectMapper();
                byte[] tuplesAsBytes = objectMapper.writeValueAsBytes(mapEphemeralTuples(ephemeralTuples));
                CryptoAESGCM cryptoAESGCM = new CryptoAESGCM(tuplesEncryptionKey);
                return Optional.of(
                        TuplesGenerationResult.builder().encryptedTuples(cryptoAESGCM.encrypt(tuplesAsBytes)).build()
                );
            }
            return Optional.empty();
        } catch (JsonProcessingException | RobertServerCryptoException e) {
            log.warn("Error serializing tuples to encrypted JSON", e);
            return Optional.empty();
        }
    }

    private byte[] getIdFromDecryptedEBID(byte[] ebid) {
        byte[] idA = new byte[5];
        System.arraycopy(ebid, 3, idA, 0, idA.length);
        return idA;
    }

    private int getEpochIdFromDecryptedEBID(byte[] ebid) {
        byte[] epochId = new byte[3];
        System.arraycopy(ebid, 0, epochId, 0, epochId.length);
        return ByteUtils.convertEpoch24bitsToInt(epochId);
    }

}
