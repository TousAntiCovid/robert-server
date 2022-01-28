package fr.gouv.stopc.e2e.mobileapplication;

import com.mongodb.MongoClient;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.mobileapplication.timemachine.repository.ClientIdentifierRepository;
import fr.gouv.stopc.e2e.mobileapplication.timemachine.repository.RegistrationRepository;
import fr.gouv.stopc.robert.client.api.CaptchaApi;
import fr.gouv.stopc.robert.client.api.DefaultApi;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpHeaders.DATE;

@Service
@AllArgsConstructor
public class MobilePhonesEmulator {

    private final ApplicationProperties applicationProperties;

    private final DefaultApi robertApi;

    private final CaptchaApi captchaApi;

    private ClientIdentifierRepository clientIdentifierRepository;

    private final RegistrationRepository registrationRepository;

    private final MongoClient mongoClient;

    final Map<String, MobileApplication> mobileApplications = new HashMap<>();

    public MobileApplication getMobileApplication(final String userName) {
        return mobileApplications.get(userName);
    }

    public void createMobileApplication(final String userName) {
        final var mobileApplication = new MobileApplication(
                userName, applicationProperties, captchaApi, robertApi, clientIdentifierRepository,
                registrationRepository
        );
        mobileApplications.put(userName, mobileApplication);
    }

    public void exchangeHelloMessagesBetween(final List<String> users,
            final Instant startInstant,
            final Duration exchangeDuration) {
        final var endDate = startInstant.plus(exchangeDuration);

        for (final String user : users) {
            final var mobileApplication = mobileApplications.get(user);
            Stream.iterate(startInstant, d -> d.isBefore(endDate), d -> d.plusSeconds(10))
                    .map(mobileApplication::produceHelloMessage)
                    .forEach(
                            hello -> users.stream()
                                    .filter(otherUser -> !otherUser.equals(user))
                                    .map(mobileApplications::get)
                                    .forEach(mobileApp -> mobileApp.receiveHelloMessage(hello))
                    );
        }

    }

    public String getPostgresqlServiceDate() {
        return clientIdentifierRepository.getDate();
    }

    // public String getMongoDBContainerDate() {
    // final var database = mongoClient.getDatabase("protectedRobertDB");
    // final var document = database.runCommand(new Document("$eval", "new
    // Date()"));
    // final var date = (Date) document.get("retval");
    // return date.toString();
    // }

    public String getBatchServiceDate() throws IOException {

        final var builder = new ProcessBuilder(applicationProperties.getBatchCommand().split(" "));
        builder.directory(Path.of(".").toFile());
        final var process = builder.start();

        final var date = process.info().startInstant().get().toString();
        process.destroy();
        return date;
    }

    public String getWSRestServiceDate() {

        final var redirectResponse = given()
                .contentType(JSON)
                .baseUri(applicationProperties.getWsRestBaseUrl().toString())
                .expect()
                .statusCode(400)

                .when()
                .post("/api/v6/status");

        final var headerDateValue = redirectResponse.then().extract().header(DATE);
        return headerDateValue;
    }

    public String getCryptoServerContainerDate() throws IOException {

        final var processBuilder = new ProcessBuilder("docker", "exec", "robert-server_crypto-server_1", "date");
        processBuilder.directory(Path.of(".").toFile());
        final var process = processBuilder.start();

        final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final var sj = new StringJoiner(System.getProperty("line.separator"));
        reader.lines().iterator().forEachRemaining(sj::add);
        final var result = sj.toString();

        process.destroy();
        return result;

    }

}
