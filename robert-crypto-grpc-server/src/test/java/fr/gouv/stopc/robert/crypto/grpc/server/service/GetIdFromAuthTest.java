package fr.gouv.stopc.robert.crypto.grpc.server.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.impl.CryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.test.IntegrationTest;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;

import static fr.gouv.stopc.robert.crypto.grpc.server.test.CryptoManager.createClientIdUsingKeys;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.CryptoManager.generateEbid;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.CryptoManager.generateMac;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.CryptoManager.generateRandomBytesArray;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager.getServerKey;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class GetIdFromAuthTest {

    private final ICryptoServerGrpcClient cryptoServerClient = new CryptoServerGrpcClient("localhost", 9090);

    @Autowired
    private static RobertClock clock;

    // private static GetIdFromAuthRequest.Builder givenValidGetIdFromAuthRequest()
    // {
    //
    // }

    @Test
    void getIdFromAuthRequest_succeed() {

        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().asEpochId();
        final var dateOfTupleEpochId = clock.atEpoch(epochId).asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochId).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue())
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(clientBundle.getId()).isEqualTo(response.getIdA().toByteArray());
        assertThat(epochId).isEqualTo(response.getEpochId());

    }

    @Test
    void getIdFromAuthRequest_with_epochid_in_the_past_succeed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().minus(10, RobertClock.ROBERT_EPOCH).asEpochId();
        final var dateOfTupleEpochId = clock.atEpoch(epochId).asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochId).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(clientBundle.getId()).isEqualTo(response.getIdA().toByteArray());
        assertThat(epochId).isEqualTo(response.getEpochId());
    }

    @Test
    void getIdFromAuthRequest_with_epochid_in_the_future_succeed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().plus(10, RobertClock.ROBERT_EPOCH).asEpochId();
        final var dateOfTupleEpochId = clock.atEpoch(epochId).asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochId).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(clientBundle.getId()).isEqualTo(response.getIdA().toByteArray());
        assertThat(epochId).isEqualTo(response.getEpochId());
    }

    @Test
    void getIdFromAuthRequest_with_incorrect_request_type_failed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().asEpochId();
        final var dateOfTupleEpochId = clock.atEpoch(epochId).asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochId).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(0) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(400);
    }

    @Test
    void getIdFromAuthRequest_with_incorrect_EBID_failed() {
        final var keyForMac = new byte[32];
        final var epochId = clock.now().minus(10, RobertClock.ROBERT_EPOCH).asEpochId();
        final var incorrectEbid = new byte[8];
        new SecureRandom().nextBytes(incorrectEbid);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(incorrectEbid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(incorrectEbid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(400);
    }

    @Test
    void getIdFromAuthRequest_with_different_epochid_in_ebid_and_request_failed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochIdInEbid = clock.now().asEpochId();
        final var epochidInRequest = clock.now().minus(5, ChronoUnit.HOURS).asEpochId();
        final var dateOfTupleEpochId = clock.atEpoch(epochIdInEbid).asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochId).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochIdInEbid, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochIdInEbid, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochidInRequest)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(400);
    }

    @Test
    void getIdFromAuthRequest_with_no_server_key_for_epochid_failed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().minus(1, ChronoUnit.DAYS).asEpochId();
        final var serverKey = new byte[24];
        new SecureRandom().nextBytes(serverKey);
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(430);
    }

    @Test
    void getIdFromAuthRequest_with_unknown_id_failed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        createClientIdUsingKeys(keyForMac, keyForTuples);
        final var idA = generateRandomBytesArray(5);
        final var epochId = clock.now().asEpochId();
        final var dateOfTupleEpochId = clock.atEpoch(epochId).asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochId).getEncoded();
        final var ebid = generateEbid(idA, epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(404);
    }

    @Test
    void getIdFromAuthRequest_with_random_mac_failed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().asEpochId();
        final var dateOfTupleEpochId = clock.atEpoch(epochId).asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochId).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateRandomBytesArray(5);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(400);

    }

    @Test
    void getIdFromAuthRequest_with_incorrect_epochid_in_mac_failed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().asEpochId();
        final var epochIdInTheFuture = clock.now().plus(1, ChronoUnit.HOURS).asEpochId();
        final var dateOfTupleEpochId = clock.atEpoch(epochId).asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochId).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochIdInTheFuture, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(400);
    }

    @Test
    void getIdFromAuthRequest_with_EBID_too_big_failed() {
        final var keyForMac = new byte[32];
        final var epochId = clock.now().minus(10, RobertClock.ROBERT_EPOCH).asEpochId();
        final var incorrectEbid = new byte[64]; // should be 64-bits size
        new SecureRandom().nextBytes(incorrectEbid);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(incorrectEbid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(incorrectEbid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(500);
    }

    @Test
    void getIdFromAuthRequest_with_different_salt_in_mac_and_request_failed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().asEpochId();
        final var dateOfTupleEpochId = clock.atEpoch(epochId).asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochId).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, keyForMac, DigestSaltEnum.UNREGISTER);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(400);
    }

    @Test
    void getIdFromAuthRequest_with_incorrect_time_value_in_request_failed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().asEpochId();
        final var dateOfTupleEpochId = clock.atEpoch(epochId).asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochId).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(0)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(400);
    }

    @Test
    void getIdFromAuthRequest_with_ebid_encoded_with_previous_server_key_failed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().plus(1, ChronoUnit.DAYS).asEpochId();
        final var dateOfTupleEpochIdPreviousDay = clock.atEpoch(clock.now().asEpochId()).asInstant().atOffset(UTC)
                .toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochIdPreviousDay).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(400);
    }

    @Test
    void getIdFromAuthRequest_with_ebid_encoded_with_next_day_server_key_failed() {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var clientBundle = createClientIdUsingKeys(keyForMac, keyForTuples);
        final var epochId = clock.now().asEpochId();
        final var dateOfTupleEpochIdNextDay = clock.atEpoch(clock.now().plus(1, ChronoUnit.DAYS).asEpochId())
                .asInstant().atOffset(UTC).toLocalDate();
        final var serverKey = getServerKey(dateOfTupleEpochIdNextDay).getEncoded();
        final var ebid = generateEbid(clientBundle.getId(), epochId, serverKey);
        final var time = clock.now().asNtpTimestamp();
        final var mac = generateMac(ebid, epochId, time, keyForMac, DigestSaltEnum.DELETE_HISTORY);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(ebid))
                .setEpochId(epochId)
                .setTime(time)
                .setMac(ByteString.copyFrom(mac))
                .setRequestType(DigestSaltEnum.DELETE_HISTORY.getValue()) // Select a request type
                .build();

        final var response = cryptoServerClient.getIdFromAuth(request).orElseThrow();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getCode()).isEqualTo(400);
    }

}
