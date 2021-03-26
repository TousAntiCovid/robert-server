package fr.gouv.clea.scenario;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.Duration;
import java.util.Objects;

import fr.gouv.tacw.qr.LocationQrCodeGenerator;
import fr.gouv.tacw.qr.model.QRCode;
import io.cucumber.java8.En;

public class CleaClientStepDefinitions implements En {
    private final ScenarioAppContext scenarioAppContext;
    public CleaClientStepDefinitions(ScenarioAppContext scenarioAppContext) {
        this.scenarioAppContext = Objects.requireNonNull(scenarioAppContext, "scenarioAppContext must not be null");

        Given("^\"([^\"]*)\" registered on TAC$", (String visitorName) -> {
            // TODO Robert registration of the user
            this.scenarioAppContext.getOrCreateVisitor(visitorName);
        });
        Given("^\"([^\"]*)\" created a QRCode \"([^\"]*)\" as a \"([^\"]*)\" at \"([^\"]*)\" with a capacity of (\\d+) and category \"([^\"]*)\" and with a renewal time of \"([^\"]*)\"$",
                (String locationName, String qrCodeId, String venueType, String periodStartTime, Integer venueCapacity, String venueCategory1, String qrCodeRenewalInterval) -> {
            Instant periodStartTimeInstant = Instant.parse(periodStartTime);
            LocationQrCodeGenerator location = this.scenarioAppContext.getOrCreateLocation(locationName, periodStartTimeInstant, venueType, venueCategory1, venueCapacity, qrCodeRenewalInterval);
            // TODO: add QR id
            location.startNewPeriod();
        });
        Given("^\"([^\"]*)\" recorded a visit to \"([^\"]*)\" at \"([^\"]*)\" withQRCode \"([^\"]*)\"$",
                (String visitorName, String locationName, String qrCodeScanTime, String qrCodeId) -> {
            LocationQrCodeGenerator location = this.scenarioAppContext.getLocation(locationName);
            Instant qrCodeScanTimeInstant = Instant.parse(qrCodeScanTime);
            QRCode qr = location.getQrCodeAt(Instant.parse(qrCodeScanTime));
            this.scenarioAppContext.getOrCreateVisitor(visitorName).scanQrCode(qr.getQrCode(), qrCodeScanTimeInstant);
        });
        When("^\"([^\"]*)\" asks for exposure status$", (String visitorName) -> {
            // TODO
        });
        Then("^Exposure status should reports \"([^\"]*)\" as not being at risk$", (String visitorName) -> {
            float riskLevel = this.scenarioAppContext.getOrCreateVisitor(visitorName).getStatus();
            assertThat(riskLevel).isEqualTo(0);
        });
    }
}
