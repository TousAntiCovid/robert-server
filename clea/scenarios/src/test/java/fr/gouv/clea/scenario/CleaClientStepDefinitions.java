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

        Given("{string} registered on TAC", (String visitorName) -> {
            // TODO Robert registration of the user
            this.scenarioAppContext.getOrCreateVisitor(visitorName);
        });

        Given("{string} has no duplicate verification", (String visitorName) -> {
            // TODO Robert registration of the user
            this.scenarioAppContext.getOrCreateVisitor(visitorName).setDupVerification(false);
        });

        //Risk configuration declaration
        Given("VType of {string}, VCategory1 of {string} and VCategory2 of {int} has risk configuration of \\(Threshold , ExposureTime, Risklevel) for backward \\({int},{int},{float}) and for forward \\({int},{int},{float})",
                (String vtype, String vcategory1, Integer vcategory2, Integer backwardThreshold, Integer backwardExposureTime, Float backwardRisk, Integer forwardThreshold, Integer forwardExposureTime, Float forwardRisk) -> {
            this.scenarioAppContext.updateOrCreateRiskConfig(vtype, vcategory1, vcategory2, backwardThreshold, backwardExposureTime, backwardRisk, forwardThreshold, forwardExposureTime, forwardRisk);
        
        });
        
        //Dynamic Location
        Given("{string} created a dynamic QRCode at {string} with VType as {string} and with VCategory1 as {string} and with VCategory2 as {int} and with a renewal time of \"{int} {word}\" and with a periodDuration of \"{int} hours\"",
                (String locationName, String periodStartTime, String venueType, String venueCategory1, Integer venueCategory2, Integer qrCodeRenewalInterval, String qrCodeRenewalIntervalUnit, Integer periodDuration) -> {
            Instant periodStartTimeInstant = TimeUtils.naturalLanguageDateStringToInstant(periodStartTime);
            Duration qrCodeRenewalIntervalDuration = Duration.of(qrCodeRenewalInterval, ChronoUnit.valueOf(qrCodeRenewalIntervalUnit.toUpperCase()));
            LocationQrCodeGenerator location = this.scenarioAppContext.getOrCreateDynamicLocation(locationName, periodStartTimeInstant, venueType, venueCategory1, venueCategory2, qrCodeRenewalIntervalDuration, periodDuration);
            // TODO: add QR id
        });
        
        //Dynamic Location with default period duration
        Given("{string} created a dynamic QRCode at {string} with VType as {string} and with VCategory1 as {string} and with VCategory2 as {int} and with and with a renewal time of \"{int} {word}\"",
        (String locationName, String periodStartTime, String venueType, String venueCategory1, Integer venueCategory2, Integer qrCodeRenewalInterval, String qrCodeRenewalIntervalUnit) -> {
            Instant periodStartTimeInstant = TimeUtils.naturalLanguageDateStringToInstant(periodStartTime);
            Duration qrCodeRenewalIntervalDuration = Duration.of(qrCodeRenewalInterval, ChronoUnit.valueOf(qrCodeRenewalIntervalUnit.toUpperCase()));
            LocationQrCodeGenerator location = this.scenarioAppContext.getOrCreateDynamicLocation(locationName, periodStartTimeInstant, venueType, venueCategory1, venueCategory2, qrCodeRenewalIntervalDuration);
            // TODO: add QR id
        });

         //Static Location
        Given("{string} created a static QRCode at {string} with VType as {string} and with VCategory1 as {string} and with VCategory2 as {int} and with a periodDuration of \"{int} hours\"",
        (String locationName, String periodStartTime, String venueType,String venueCategory1, Integer venueCategory2, Integer periodDuration) -> {
            Instant periodStartTimeInstant = TimeUtils.naturalLanguageDateStringToInstant(periodStartTime);
            LocationQrCodeGenerator location = this.scenarioAppContext.getOrCreateStaticLocation(locationName, periodStartTimeInstant, venueType, venueCategory1, venueCategory2, periodDuration);
            // TODO: add QR id
        });

        //Static Location with default Period Duration
        Given("{string} created a static QRCode at {string} with VType as {string} and VCategory1 as {string} and with VCategory2 as {int}",
        (String locationName, String periodStartTime, String venueType,String venueCategory1, Integer venueCategory2)-> {
            Instant periodStartTimeInstant = TimeUtils.naturalLanguageDateStringToInstant(periodStartTime);
            LocationQrCodeGenerator location = this.scenarioAppContext.getOrCreateStaticLocation(locationName, periodStartTimeInstant, venueType, venueCategory1, venueCategory2);
            // TODO: add QR id
        });

        //Visitor scan a QR code at given instant
        Given("{string} recorded a visit to {string} at {string}",
                (String visitorName, String locationName, String qrCodeScanTime) -> {
            LocationQrCodeGenerator location = this.scenarioAppContext.getLocation(locationName);
            Instant qrCodeScanTimeInstant = TimeUtils.naturalLanguageDateStringToInstant(qrCodeScanTime);
            QRCode qr = location.getQrCodeAt(TimeUtils.naturalLanguageDateStringToInstant(qrCodeScanTime));
            this.scenarioAppContext.getOrCreateVisitor(visitorName).scanQrCode(qr.getQrCode(), qrCodeScanTimeInstant);
        });
        //Visitor scan a staff QR code at given instant
        Given("{string} recorded a visit to {string} at {string} as a STAFF",
                (String visitorName, String locationName, String qrCodeScanTime) -> {
            LocationQrCodeGenerator location = this.scenarioAppContext.getStaffLocation(locationName);
            Instant qrCodeScanTimeInstant = TimeUtils.naturalLanguageDateStringToInstant(qrCodeScanTime);
            QRCode qr = location.getQrCodeAt(TimeUtils.naturalLanguageDateStringToInstant(qrCodeScanTime));
            this.scenarioAppContext.getOrCreateVisitor(visitorName).scanQrCode(qr.getQrCode(), qrCodeScanTimeInstant);
        });

        //Visitor scan a QR code at a given Instant, but the scanned QR code is valid for another Instant
        Given("{string} recorded a visit to {string} at {string} with a QR code valid for {string}",
                (String visitorName, String locationName, String qrCodeScanTime, String qrCodeValidTime) -> {
            LocationQrCodeGenerator location = this.scenarioAppContext.getLocation(locationName);
            Instant qrCodeScanTimeInstant = TimeUtils.naturalLanguageDateStringToInstant(qrCodeScanTime);
            QRCode qr = location.getQrCodeAt(TimeUtils.naturalLanguageDateStringToInstant(qrCodeValidTime));
            this.scenarioAppContext.getOrCreateVisitor(visitorName).scanQrCode(qr.getQrCode(), qrCodeScanTimeInstant);
        });

        When("^Cluster detection triggered$", () -> {
            this.scenarioAppContext.triggerNewClusterIdenfication();
        });

        When("{string} declares himself/herself sick", (String visitorName) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            visitor.sendReport();
        });

        When("{string} declares himself/herself sick with a {string} pivot date", (String visitorName, String pivotDate) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            System.out.println(pivotDate);
            visitor.sendReport(TimeUtils.naturalLanguageDateStringToInstant(pivotDate));
        });

        When("{string} declares himself/herself sick with a malformed pivot date", (String visitorName) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            visitor.sendMalformedReport(true, false, false);
        });

        When("{string} declares himself/herself sick with a malformed QrCode", (String visitorName) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            visitor.sendMalformedReport(false, true, false);
        });

        When("{string} declares himself/herself sick with a malformed scan time", (String visitorName) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            visitor.sendMalformedReport(false, false, true);
        });

        When("{string} declares himself/herself sick with no scan time", (String visitorName) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            visitor.sendReportWithEmptyField(false, false, true);
        });

        When("{string} declares himself/herself sick with no QrCode", (String visitorName) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            visitor.sendReportWithEmptyField(false, true, false);
        });

        When("{string} asks for exposure status", (String string) -> {
        });

        Then("Exposure status should reports {string} as not being at risk", (String visitorName) -> {
            float riskLevel = this.scenarioAppContext.getOrCreateVisitor(visitorName).getStatus();
            assertThat(riskLevel).isEqualTo(0);
        });

        Then("Exposure status should reports {string} as being at risk of {float}", (String visitorName, Float risk) -> {
            float riskLevel = this.scenarioAppContext.getVisitor(visitorName).getStatus();
            assertThat(riskLevel).isEqualTo(risk);
        });

        Then("Exposure status request for {string} should include only {int} visit\\(s) to {string} at {string}", (String visitorName, Integer nbVisits, String locationName, String qrScanTime) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            assertThat(visitor.getLocalList().size()).isEqualTo(nbVisits);
        });

        Then("{string} has {int} rejected visit(s)", (String visitorName, Integer rejectedVisits) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            System.out.println(visitor.getLastReportResponse());
            assertThat(visitor.getLastReportResponse().getRejectedVisits()).isEqualTo(rejectedVisits);
        });

        Then("{string} cannot send his/her visits", (String visitorName) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            assertThat(visitor.getLastReportSuccess()).isFalse();
        });

        Then("{string} sends his/her visits", (String visitorName) -> {
            CleaClient visitor = this.scenarioAppContext.getVisitor(visitorName);
            assertThat(visitor.getLastReportSuccess()).isTrue();
        });

    }

    protected String toFirstLetterUpperCase(String string) {
        char first = Character.toUpperCase(string.charAt(0));
        return first + string.substring(1);
    }
}
