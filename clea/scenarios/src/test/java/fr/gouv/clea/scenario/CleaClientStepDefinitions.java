package fr.gouv.clea.scenario;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import fr.gouv.clea.client.service.CleaClient;
import fr.gouv.clea.qr.LocationQrCodeGenerator;
import fr.gouv.clea.qr.model.QRCode;
import io.cucumber.java8.En;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

public class CleaClientStepDefinitions implements En {
    private final ScenarioAppContext scenarioAppContext;

    public CleaClientStepDefinitions(ScenarioAppContext scenarioAppContext) {
        this.scenarioAppContext = Objects.requireNonNull(scenarioAppContext, "scenarioAppContext must not be null");

        Given("^\"([^\"]*)\" registered on TAC$", (String visitorName) -> {
            // TODO Robert registration of the user
            this.scenarioAppContext.getOrCreateVisitor(visitorName);
        });
        Given("^\"([^\"]*)\" created a QRCode \"([^\"]*)\" as a \"([^\"]*)\" at \"([^\"]*)\" with a capacity of (\\d+) and category \"([^\"]*)\" and with a renewal time of (\\d+) \"([^\"]*)\"$",
                (String locationName, String qrCodeId, String venueType, String periodStartTime, Integer venueCapacity, String venueCategory1, Integer qrCodeRenewalInterval, String qrCodeReewalIntervalUnit) -> {
            Instant periodStartTimeInstant = TimeUtils.naturalLanguageDateStringToInstant(periodStartTime);
            Duration qrCodeRenewalIntervalDuration = Duration.of(qrCodeRenewalInterval, ChronoUnit.valueOf(qrCodeReewalIntervalUnit.toUpperCase()));
            LocationQrCodeGenerator location = this.scenarioAppContext.getOrCreateLocation(locationName, periodStartTimeInstant, venueType, venueCategory1, venueCapacity, qrCodeRenewalIntervalDuration);
            // TODO: add QR id
        });
        Given("^\"([^\"]*)\" recorded a visit to \"([^\"]*)\" at \"([^\"]*)\" withQRCode \"([^\"]*)\"$",
                (String visitorName, String locationName, String qrCodeScanTime, String qrCodeId) -> {
            LocationQrCodeGenerator location = this.scenarioAppContext.getLocation(locationName);
            Instant qrCodeScanTimeInstant = TimeUtils.naturalLanguageDateStringToInstant(qrCodeScanTime);
            QRCode qr = location.getQrCodeAt(TimeUtils.naturalLanguageDateStringToInstant(qrCodeScanTime));
            this.scenarioAppContext.getOrCreateVisitor(visitorName).scanQrCode(qr.getQrCode(), qrCodeScanTimeInstant);
        });
        When("^\"([^\"]*)\" asks for exposure status$", (String visitorName) -> {
            // TODO
        });
        Then("^Exposure status should reports \"([^\"]*)\" as not being at risk$", (String visitorName) -> {
            float riskLevel = this.scenarioAppContext.getOrCreateVisitor(visitorName).getStatus();
            assertThat(riskLevel).isEqualTo(0);
        });

        Then("^Exposure status request for \"([^\"]*)\" should include only \"(\\d+)\" visit(s) to \"([^\"]*)\" at \"([^\"]*)\"", (String visitorName, Integer nbVisits, String locationName, String qrScanTime) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            assertThat(visitor.getLocalList().size()).isEqualTo(nbVisits);
        });
    }

    protected String toFirstLetterUpperCase(String string) {
        char first = Character.toUpperCase(string.charAt(0));
        return first + string.substring(1);
    }
}
