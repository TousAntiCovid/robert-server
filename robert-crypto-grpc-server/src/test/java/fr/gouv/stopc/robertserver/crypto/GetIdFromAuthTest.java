package fr.gouv.stopc.robertserver.crypto;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.impl.CryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.model.ClientIdentifierBundle;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest;
import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;

import static fr.gouv.stopc.robertserver.crypto.test.ClockManager.clock;
import static fr.gouv.stopc.robertserver.crypto.test.KeystoreManager.*;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.GrpcResponseMatcher.grpcErrorResponse;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.GrpcResponseMatcher.noGrpcError;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class GetIdFromAuthTest {

    private final ICryptoServerGrpcClient robertCryptoClient = new CryptoServerGrpcClient("localhost", 9090);

    private static final RobertClock.RobertInstant now = clock().now();

    @Value
    @Builder
    static class AuthRequestBundle {

        byte[] ebid;

        int epochId;

        long time;

        byte[] mac;

        DigestSaltEnum requestType;

        byte[] idA;

        byte[] serverKey;

        ClientIdentifierBundle clientIdentifierBundle;
    }

    private static AuthRequestBundle.AuthRequestBundleBuilder givenValidAuthRequestBundle() {
        final var clientBundle = createClientId();
        final var epochId = now.asEpochId();
        final var serverKey = getServerKey(epochId).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = now.asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, clientBundle.getKeyForMac(), DigestSaltEnum.DELETE_HISTORY);

        return AuthRequestBundle.builder()
                .ebid(ebid)
                .epochId(epochId)
                .time(time)
                .mac(mac)
                .serverKey(serverKey)
                .clientIdentifierBundle(clientBundle)
                .requestType(DigestSaltEnum.DELETE_HISTORY)
                .idA(clientBundle.getId());

    }

    @Test
    void getIdFromAuthRequest_succeed() {
        final var authRequestBundle = givenValidAuthRequestBundle().build();

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response).has(noGrpcError());
        assertThat(authRequestBundle.getIdA()).isEqualTo(response.getIdA().toByteArray());
        assertThat(authRequestBundle.getEpochId()).isEqualTo(response.getEpochId());

    }

    @Test
    void getIdFromAuthRequest_with_epochid_in_the_past_succeed() {
        final var authRequestBundle = givenValidAuthRequestBundle()
                .epochId(now.minus(10, RobertClock.ROBERT_EPOCH).asEpochId())
                .build();
        final var ebid = generateEbid(
                authRequestBundle.getIdA(), authRequestBundle.getEpochId(), authRequestBundle.getServerKey()
        );
        final var mac = generateMac(
                ebid, authRequestBundle.getEpochId(), authRequestBundle.getTime(),
                authRequestBundle.getClientIdentifierBundle().getKeyForMac(), DigestSaltEnum.DELETE_HISTORY
        );

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response).has(noGrpcError());
        assertThat(authRequestBundle.getIdA()).isEqualTo(response.getIdA().toByteArray());
        assertThat(authRequestBundle.getEpochId()).isEqualTo(response.getEpochId());
    }

    @Test
    void getIdFromAuthRequest_with_epochid_in_the_future_succeed() {
        final var authRequestBundle = givenValidAuthRequestBundle()
                .epochId(now.plus(10, RobertClock.ROBERT_EPOCH).asEpochId())
                .build();
        final var ebid = generateEbid(
                authRequestBundle.getIdA(), authRequestBundle.getEpochId(), authRequestBundle.getServerKey()
        );
        final var mac = generateMac(
                ebid, authRequestBundle.getEpochId(), authRequestBundle.getTime(),
                authRequestBundle.getClientIdentifierBundle().getKeyForMac(), DigestSaltEnum.DELETE_HISTORY
        );

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response).has(noGrpcError());
        assertThat(authRequestBundle.getIdA()).isEqualTo(response.getIdA().toByteArray());
        assertThat(authRequestBundle.getEpochId()).isEqualTo(response.getEpochId());
    }

    @Test
    void getIdFromAuthRequest_with_incorrect_request_type_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle().build();

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(0)
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Unknown request type 0"));
    }

    @Test
    void getIdFromAuthRequest_with_malformed_EBID_failed() {
        final var malformedEbid = new byte[8];
        new SecureRandom().nextBytes(malformedEbid);
        final var authRequestBundle = givenValidAuthRequestBundle()
                .ebid(malformedEbid)
                .build();

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Could not decrypt ebid content"));
    }

    @Test
    void getIdFromAuthRequest_with_different_epochid_in_ebid_and_request_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle().build();
        final var epochidInRequest = now.minus(5, ChronoUnit.HOURS).asEpochId();

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(epochidInRequest)
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Could not decrypt ebid content"));
    }

    @Test
    void getIdFromAuthRequest_with_no_server_key_for_epochid_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle()
                .epochId(now.minus(1, ChronoUnit.DAYS).asEpochId())
                .build();

        final var serverKey = new byte[24];
        new SecureRandom().nextBytes(serverKey);
        final var ebid = generateEbid(authRequestBundle.getIdA(), authRequestBundle.getEpochId(), serverKey);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(
                        grpcErrorResponse(
                                430, "No server key found from cryptographic storage : %s",
                                authRequestBundle.getEpochId()
                        )
                );
    }

    @Test
    void getIdFromAuthRequest_with_unknown_id_failed() {
        createClientId();
        final var authRequestBundle = givenValidAuthRequestBundle().build();
        final var idA = new byte[5];
        new SecureRandom().nextBytes(idA);
        final var ebid = generateEbid(idA, authRequestBundle.getEpochId(), authRequestBundle.getServerKey());

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(404, "Could not find id"));
    }

    @Test
    void getIdFromAuthRequest_with_random_mac_failed() {
        final var mac = new byte[5];
        new SecureRandom().nextBytes(mac);
        final var authRequestBundle = givenValidAuthRequestBundle()
                .mac(mac)
                .build();

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Invalid MAC"));

    }

    @Test
    void getIdFromAuthRequest_with_different_epochId_in_mac_failed() {

        final var authRequestBundle = givenValidAuthRequestBundle().build();
        final var macWithDifferentEpochId = generateMac(
                authRequestBundle.getEbid(),
                now.plus(1, ChronoUnit.HOURS).asEpochId(),
                authRequestBundle.getTime(),
                authRequestBundle.getClientIdentifierBundle().getKeyForMac(),
                DigestSaltEnum.DELETE_HISTORY
        );

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(macWithDifferentEpochId))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Invalid MAC"));
    }

    @Test
    void getIdFromAuthRequest_with_different_ebid_in_mac_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle().build();
        final var randomEbid = new byte[8];
        new SecureRandom().nextBytes(randomEbid);

        final var macWithDifferentEbid = generateMac(
                randomEbid,
                authRequestBundle.getEpochId(),
                authRequestBundle.getTime(),
                authRequestBundle.getClientIdentifierBundle().getKeyForMac(),
                DigestSaltEnum.DELETE_HISTORY
        );

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(macWithDifferentEbid))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Invalid MAC"));
    }

    @Test
    void getIdFromAuthRequest_with_different_time_in_mac_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle().build();

        final var macWithDifferentTime = generateMac(
                authRequestBundle.getEbid(),
                authRequestBundle.getEpochId(),
                0,
                authRequestBundle.getClientIdentifierBundle().getKeyForMac(),
                DigestSaltEnum.DELETE_HISTORY
        );

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(macWithDifferentTime))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Invalid MAC"));
    }

    @Test
    void getIdFromAuthRequest_mac_encrypted_with_incorrect_key_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle().build();
        byte[] differentKeyForMac = new byte[32];
        new SecureRandom().nextBytes(differentKeyForMac);

        final var macEncryptedWithDifferentKey = generateMac(
                authRequestBundle.getEbid(),
                authRequestBundle.getEpochId(),
                authRequestBundle.getTime(),
                differentKeyForMac,
                DigestSaltEnum.DELETE_HISTORY
        );

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(macEncryptedWithDifferentKey))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Invalid MAC"));
    }

    @Test
    void getIdFromAuthRequest_with_EBID_too_big_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle().build();
        final var incorrectEbid = new byte[64]; // should be 64-bits size
        new SecureRandom().nextBytes(incorrectEbid);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(incorrectEbid))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(500, "Error validating authenticated request"));
    }

    @Test
    void getIdFromAuthRequest_with_different_salt_in_mac_and_request_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle().build();
        final var macWithDifferentSalt = generateMac(
                authRequestBundle.getEbid(),
                authRequestBundle.getEpochId(),
                authRequestBundle.getTime(),
                authRequestBundle.getClientIdentifierBundle().getKeyForMac(),
                DigestSaltEnum.UNREGISTER
        );

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(macWithDifferentSalt))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Invalid MAC"));
    }

    @Test
    void getIdFromAuthRequest_with_incorrect_time_value_in_request_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle().build();

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(authRequestBundle.getEbid()))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(0)
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Invalid MAC"));
    }

    @Test
    void getIdFromAuthRequest_with_ebid_encoded_with_previous_server_key_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle()
                .epochId(now.plus(1, ChronoUnit.DAYS).asEpochId())
                .build();

        final var serverKeyFromPreviousDay = getServerKey(now.asEpochId()).getEncoded();
        final var ebid = generateEbid(
                authRequestBundle.getIdA(), authRequestBundle.getEpochId(), serverKeyFromPreviousDay
        );

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Could not decrypt ebid content"));
    }

    @Test
    void getIdFromAuthRequest_with_ebid_encoded_with_next_day_server_key_failed() {
        final var authRequestBundle = givenValidAuthRequestBundle().build();

        final var serverKey = getServerKey(now.plus(1, ChronoUnit.DAYS).asEpochId()).getEncoded();
        final var ebid = generateEbid(authRequestBundle.getIdA(), authRequestBundle.getEpochId(), serverKey);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(authRequestBundle.getEpochId())
                .setTime(authRequestBundle.getTime())
                .setMac(ByteString.copyFrom(authRequestBundle.getMac()))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Could not decrypt ebid content"));
    }

}
