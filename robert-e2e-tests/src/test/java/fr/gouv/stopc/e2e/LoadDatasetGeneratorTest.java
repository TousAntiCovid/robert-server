package fr.gouv.stopc.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.e2e.external.crypto.CryptoAESGCM;
import fr.gouv.stopc.e2e.external.crypto.model.EphemeralTupleJson;
import fr.gouv.stopc.e2e.mobileapplication.EpochClock;
import fr.gouv.stopc.e2e.mobileapplication.model.ClientKeys;
import fr.gouv.stopc.e2e.mobileapplication.model.ContactTuple;
import fr.gouv.stopc.e2e.mobileapplication.model.HelloMessage;
import fr.gouv.stopc.e2e.mobileapplication.model.RobertRequestBuilder;
import fr.gouv.stopc.robert.client.model.Contact;
import fr.gouv.stopc.robert.client.model.HelloMessageDetail;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.stream.IntStream;

import static fr.gouv.stopc.e2e.external.common.enums.DigestSaltEnum.HELLO;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

class LoadDatasetGeneratorTest {

    private static final String SERVER_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwpDgJdE0aRop5uibRYqCOLK7CnZ+DAyQhGrVD6XYXC/5LeNtLwqSxPAVcswqtiZyPs68h2Y3KEQn2y2bRxRzQg==";
    // dev :
    // "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE7fAxfrLtG5tXyjomCOg9r1wNnr9len4m9sTflJUHDdkwubKyidTtFkVfNLrM91yuffdrpJZe9XOkof8P+zPGDg==";
    // int :
    // "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwpDgJdE0aRop5uibRYqCOLK7CnZ+DAyQhGrVD6XYXC/5LeNtLwqSxPAVcswqtiZyPs68h2Y3KEQn2y2bRxRzQg==";

    private static final String INPUT = "../../load-tests/src/test/resources/robert/register-tuples.csv";

    private static final String OUTPUT = "../../load-tests/src/test/resources/robert/client-auth-hello.csv";

    private static final Random RANDOM = new Random();

    private static final long NTP_SECONDS = Duration.between(
            Instant.parse("1900-01-01T00:00:00Z"),
            Instant.parse("1970-01-01T00:00:00Z")
    ).getSeconds();

    private final EpochClock clock = new EpochClock(
            Instant.parse("2020-06-01T00:00:00Z").plusSeconds(NTP_SECONDS).getEpochSecond()
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Disabled("Used for load test data generation")
    void generate_auth_details_and_hello() throws Exception {
        final var ecKeyFactory = KeyFactory.getInstance("EC");
        try (
                final var reader = new BufferedReader(new InputStreamReader(new FileInputStream(INPUT)));
                final var writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(OUTPUT)));) {
            writer.println(
                    "public,private,epoch_id,ebid,time32,status_mac,deletehistory_mac,unregister_mac,contact_json"
            );

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // lecture du CSV public,private,tuples
                    final var items = line.split(",");
                    final var pub = Base64.getDecoder().decode(items[0].trim());
                    final var priv = Base64.getDecoder().decode(items[1].trim());
                    final var encryptedTuples = Base64.getDecoder().decode(items[2].trim());

                    final var privateKey = ecKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(priv));

                    final var clientKeys = ClientKeys.builder(SERVER_PUBLIC_KEY)
                            .build(new KeyPair(null, privateKey));

                    final var aesGcm = new CryptoAESGCM(clientKeys.getKeyForTuples());
                    final var tuples = objectMapper
                            .readValue(aesGcm.decrypt(encryptedTuples), EphemeralTupleJson[].class);
                    final var tuplesByEpochId = Arrays.stream(tuples)
                            .collect(
                                    toMap(
                                            EphemeralTupleJson::getEpochId,
                                            tuple -> new ContactTuple(tuple.getKey().getEbid(), tuple.getKey().getEcc())
                                    )
                            );

                    final var epochId = tuplesByEpochId.keySet().stream()
                            .findAny()
                            .orElseThrow();
                    final var someTuple = tuplesByEpochId.get(epochId);
                    final var requestTime = clock.atEpoch(epochId);
                    final var authRequestBuilder = RobertRequestBuilder.withMacKey(clientKeys.getKeyForMac());
                    final var statusAuth = authRequestBuilder.exposureStatusRequest(someTuple.getEbid(), requestTime)
                            .build();
                    final var deleteHistoryAuth = authRequestBuilder
                            .deleteExposureHistory(someTuple.getEbid(), requestTime)
                            .build();
                    final var unregisterAuth = authRequestBuilder.unregisterRequest(someTuple.getEbid(), requestTime)
                            .build();

                    // generate randomly 0 to 6 contacts
                    final var hellos = IntStream.range(0, RANDOM.nextInt(6))
                            .mapToObj(i -> requestTime.asEpochId() + i)
                            .map(helloEpochId -> {
                                final var tuple = tuplesByEpochId.get(helloEpochId);
                                final var helloFrequency = 1 + RANDOM.nextInt(10);
                                return Contact.builder()
                                        .ebid(tuple.getEbid())
                                        .ecc(tuple.getEcc())
                                        .ids(
                                                // for each minute in the epoch
                                                IntStream.range(0, 15)
                                                        .boxed()
                                                        .flatMap(
                                                                // for each second in the epoch's minute
                                                                minuteInEpoch -> IntStream.range(0, RANDOM.nextInt(60))
                                                                        // consider only seconds for the given
                                                                        // helloFrequency
                                                                        .filter(
                                                                                secondInMinute -> secondInMinute
                                                                                        % helloFrequency == 0
                                                                        )
                                                                        .mapToObj(
                                                                                secondInMinute -> generateHello(
                                                                                        clientKeys,
                                                                                        helloEpochId, tuple,
                                                                                        minuteInEpoch,
                                                                                        secondInMinute
                                                                                )
                                                                        )
                                                        )
                                                        .collect(toList())
                                        )
                                        .build();
                            })
                            .collect(toList());
                    final var helloList = objectMapper.writeValueAsString(hellos)
                            .replaceAll("^\\[", "")
                            .replaceAll("]$", "");

                    writer.printf(
                            "%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                            items[0],
                            items[1],
                            statusAuth.getEpochId(),
                            Base64.getEncoder().encodeToString(statusAuth.getEbid()),
                            Base64.getEncoder().encodeToString(statusAuth.getTime()),
                            Base64.getEncoder().encodeToString(statusAuth.getMac()),
                            Base64.getEncoder().encodeToString(deleteHistoryAuth.getMac()),
                            Base64.getEncoder().encodeToString(unregisterAuth.getMac()),
                            Base64.getEncoder().encodeToString(helloList.getBytes())
                    );
                } catch (Exception e) {
                    System.err.printf("Ignoring line '%s': %s\n", line, e.getMessage());
                }
            }
        }
    }

    private HelloMessageDetail generateHello(ClientKeys clientKeys, Integer epochId, ContactTuple tuple,
            Integer minuteInEpoch, int secondInMinute) {
        final var helloTime = clock.atEpoch(epochId)
                .getTime()
                .plus(minuteInEpoch, MINUTES)
                .plusSeconds(secondInMinute);
        final var helloNtpTimeStamp = clock
                .at(helloTime).asNtpTimestamp();
        final var time32 = ByteBuffer
                .allocate(Integer.BYTES)
                .putInt((int) clock.at(helloTime).asNtpTimestamp())
                .put(0, (byte) 0)
                .put(1, (byte) 0)
                .rewind()
                .getInt();
        final var h = HelloMessage
                .builder(HELLO, clientKeys.getKeyForMac())
                .ebid(tuple.getEbid())
                .ecc(tuple.getEcc())
                .time(helloTime)
                .build();
        return HelloMessageDetail.builder()
                .timeFromHelloMessage(time32)
                .mac(h.getMac())
                .timeCollectedOnDevice(helloNtpTimeStamp)
                .rssiCalibrated(-20 - RANDOM.nextInt(50))
                .build();
    }

}
