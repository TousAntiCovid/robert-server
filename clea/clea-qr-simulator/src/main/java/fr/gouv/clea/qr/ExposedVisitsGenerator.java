package fr.gouv.clea.qr;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.clea.qr.model.QRCode;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExposedVisitsGenerator {
    private static final int RETENTION_DAYS = 14;
    private static final Instant NOW = Instant.now();
    private static final Instant OLDEST_VALID_VISIT_TIME = NOW.minus(RETENTION_DAYS, ChronoUnit.DAYS);
    private static final int NB_LOCATIONS = 10;
    private static final int NB_INFECTED_VISITS = 50 * 100 * RETENTION_DAYS;
    private ExposedVisitsGeneratorConfiguration configuration;
    private List<LocationQrCodeGenerator> locations;
    private List<QRCode> qrCodes;

    public ExposedVisitsGenerator(ExposedVisitsGeneratorConfiguration exposedVisitsGeneratorConfiguration) {
        this.configuration = exposedVisitsGeneratorConfiguration;
        qrCodes = new ArrayList<QRCode>(NB_INFECTED_VISITS);
    }

    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
        new ExposedVisitsGenerator(new ExposedVisitsGeneratorConfiguration()).run();
    }

    public void run() throws JsonGenerationException, JsonMappingException, IOException {
        this.locations = this.generateLocations();
        this.generateQrCodes();
        this.dumpQrCodes(new File("./qrcodes.json"));
    }

    protected void dumpQrCodes(File file) throws JsonGenerationException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(file, this.qrCodes);
    }

    protected void generateQrCodes() {
        int nbVisitsPerLocation = NB_INFECTED_VISITS / NB_LOCATIONS;
        log.info("generating {} visits per location.", nbVisitsPerLocation);
        this.locations.forEach(location -> this.generateLocationQrCodes(location, nbVisitsPerLocation));
    }

    protected List<QRCode> generateLocationQrCodes(LocationQrCodeGenerator location, int nbVisits) {
        List<QRCode> qrCodes = IntStream.rangeClosed(1, nbVisits)
                .mapToObj(i -> getQrCodeAtRandomInstant(location))
                .collect(Collectors.toList());
        qrCodes.addAll(qrCodes);
        return qrCodes;
    }

    protected QRCode getQrCodeAtRandomInstant(LocationQrCodeGenerator location) {
        try {
            return location.getQrCodeAt(this.randomVisitInstant());
        } catch (CleaCryptoException e) {
            log.error("Cannot generate QR", e);
            return null;
        }
    }
    
    protected Instant randomVisitInstant() {
        return this.between(OLDEST_VALID_VISIT_TIME, NOW);
    }
    
    protected Instant between(Instant startInclusive, Instant endExclusive) {
        long startSeconds = startInclusive.getEpochSecond();
        long endSeconds = endExclusive.getEpochSecond();
        long random = ThreadLocalRandom.current().nextLong(startSeconds, endSeconds);

        return Instant.ofEpochSecond(random);
    }

    protected List<LocationQrCodeGenerator> generateLocations() {
        log.info("generating {} locations.", NB_LOCATIONS);
        return IntStream.rangeClosed(1, NB_LOCATIONS)
            .mapToObj(i -> this.generateLocation())
            .collect(Collectors.toList());
    }

    protected LocationQrCodeGenerator generateLocation() {
        byte[] permanentLocationSecretKeyA = new byte[LocationSpecificPart.LOCATION_TEMPORARY_SECRET_KEY_SIZE];
        new Random().nextBytes(permanentLocationSecretKeyA);
        String permanentLocationSecretKey = Hex.toHexString(permanentLocationSecretKeyA);
        Instant periodStartTime = NOW.truncatedTo(ChronoUnit.HOURS);
        int periodDuration = 24;
        int venueType = 1;
        String locationPhone = "0600000000";
        int qrCodeRenewalIntervalExponentCompact = 4;
        try {
            return LocationQrCodeGenerator.builder()
                    .serverAuthorityPublicKey(configuration.getServerAuthorityPublicKey())
                    .manualContactTracingAuthorityPublicKey(configuration.getManualContactTracingAuthorityPublicKey())
                    .permanentLocationSecretKey(permanentLocationSecretKey)
                    .periodStartTime(periodStartTime)
                    .periodDuration(periodDuration)
                    .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact)
                    .staff(false)
                    .countryCode(250)
                    .venueType(venueType)
                    .venueCategory1(1)
                    .venueCategory2(1)
                    .locationPhone(locationPhone)
                    .locationPin("123456")
                    .build();
        } catch (CleaCryptoException e) {
            log.error("Cannot generate location", e);
            return null;
        }
    }
    
}
