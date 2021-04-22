package fr.gouv.clea.scenario;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import org.bouncycastle.util.encoders.Hex;

import fr.gouv.clea.client.service.CleaClient;
import fr.gouv.clea.qr.LocationQrCodeGenerator;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScenarioAppContext {
    private final Map<String, Integer> venueCategories1;
    private final Map<String, Integer> venueTypes;
    private Map<String, CleaClient> visitors;
    private Map<String, LocationQrCodeGenerator> locations;
    private Map<String, LocationQrCodeGenerator> staffLocations;
    private String manualContactTracingAuthorityPublicKey;
    private String serverAuthorityPublicKey;
    private int venueTypeCounter = 0;
    private int venueCategoryCounter = 0;


    public ScenarioAppContext() throws Exception {
        visitors = new HashMap<String, CleaClient>(10);
        locations = new HashMap<String, LocationQrCodeGenerator>(10);
        staffLocations = new HashMap<String, LocationQrCodeGenerator>(10);
        this.initializeKeys();
        venueTypes = new HashMap<String, Integer>(Map.of("restaurant", venueTypeCounter));
        venueTypeCounter++;
        venueCategories1 = new HashMap<String, Integer>(Map.of("NUMBER_1", venueCategoryCounter));
        venueCategoryCounter++;
    }

    public void initializeKeys() throws Exception {
        Properties properties = new Properties();
        properties.load(ScenarioAppContext.class.getClassLoader().getResourceAsStream("application.properties"));
        this.serverAuthorityPublicKey = properties.getProperty("serverAuthorityPublicKey");
        this.manualContactTracingAuthorityPublicKey = properties.getProperty("manualContactTracingAuthorityPublicKey");
        if (Objects.isNull(this.serverAuthorityPublicKey) || this.serverAuthorityPublicKey.isBlank()) {
        this.generateKeys();
        } else {
            log.info("Server Authority Public Key : " + this.serverAuthorityPublicKey);
            log.info("Manual Contact Tracing Authority Public Key : " + this.manualContactTracingAuthorityPublicKey);
    }
    }

    public void generateKeys() throws Exception {
        CleaEciesEncoder cleaEciesEncoder = new CleaEciesEncoder();
        String[] serverAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        this.serverAuthorityPublicKey = serverAuthorityKeyPair[1];
        log.info("Server Authority Private Key: " + serverAuthorityKeyPair[0]);
        log.info("Server Authority Public Key : " + this.serverAuthorityPublicKey);

        String[] manualContactTracingAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        this.manualContactTracingAuthorityPublicKey = manualContactTracingAuthorityKeyPair[1];
        log.info("Manual Contact Tracing Authority Private Key: " + serverAuthorityKeyPair[0]);
        log.info("Manual Contact Tracing Authority Public Key : " + this.manualContactTracingAuthorityPublicKey);
    }

    public void updateOrCreateRiskConfig(String vtype, String vcategory1, Integer vcategory2, Integer backwardThreshold, Integer backwardExposureTime, Float backwardRisk, Integer forwardThreshold, Integer forwardExposureTime, Float forwardRisk){
        if(!venueTypes.containsKey(vtype)){
            venueTypes.put(vtype, venueTypeCounter);
            venueTypeCounter++;
        }
        if(!venueCategories1.containsKey(vcategory1)){
            venueCategories1.put(vcategory1, venueCategoryCounter);
            venueCategoryCounter++;
        }
    }

    public CleaClient getOrCreateVisitor(String name) {
        return visitors.computeIfAbsent(name, newName -> this.createVisitor(newName));
    }

    private CleaClient createVisitor(String name) {
        log.info("Creating visitor " + name);
        return new CleaClient(name);
    }

    private LocationQrCodeGenerator createDynamicLocation(String locationName, Instant periodStartTime, String venueType,
            String venueCategory1, Integer venueCategory2, Duration qrCodeRenewalInterval, Integer periodDuration) throws CleaCryptoException {
        long qrCodeRenewalIntervalLong = qrCodeRenewalInterval.getSeconds();
        int qrCodeRenewalIntervalExponentCompact = (int) (Math.log(qrCodeRenewalIntervalLong) / Math.log(2));
        return this.createLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2, qrCodeRenewalIntervalExponentCompact, periodDuration);
    }

    private LocationQrCodeGenerator createStaticLocation(String locationName, Instant periodStartTime, String venueType,
    String venueCategory1, Integer venueCategory2, Integer periodDuration) throws CleaCryptoException {
        int qrCodeRenewalIntervalExponentCompact = 0x1F;
        return this.createLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2, qrCodeRenewalIntervalExponentCompact, periodDuration);
    }

    private LocationQrCodeGenerator  createLocation(String locationName, Instant periodStartTime, String venueType,
    String venueCategory1, Integer venueCategory2,Integer qrCodeRenewalIntervalExponentCompact, Integer periodDuration) throws CleaCryptoException {
        log.info("Creating location " + locationName);
        final String permanentLocationSecretKey = Hex.toHexString(UUID.randomUUID().toString().getBytes());
        LocationQrCodeGenerator location = LocationQrCodeGenerator.builder()
                                                                .countryCode(250) // France Country Code
                                                                .staff(false)
                                                                .venueCategory1(venueCategories1.get(venueCategory1))
                                                                .venueCategory2(venueCategory2)
                                                                .venueType(venueTypes.get(venueType))
                                                                .periodDuration(periodDuration)
                                                                .periodStartTime(periodStartTime)
                                                                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact)
                                                                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                                                                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                                                                .permanentLocationSecretKey(permanentLocationSecretKey)
                                                                .build();
        LocationQrCodeGenerator staffLocation = LocationQrCodeGenerator.builder()
                                                                .countryCode(250) // France Country Code
                                                                .staff(true)
                                                                .venueCategory1(venueCategories1.get(venueCategory1))
                                                                .venueCategory2(venueCategory2)
                                                                .venueType(venueTypes.get(venueType))
                                                                .periodDuration(periodDuration)
                                                                .periodStartTime(periodStartTime)
                                                                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact)
                                                                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                                                                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                                                                .permanentLocationSecretKey(permanentLocationSecretKey)
                                                                .build();
        staffLocations.put(locationName, staffLocation);
        locations.put(locationName, location);
        return location;
    }

    public LocationQrCodeGenerator getOrCreateDynamicLocation(String locationName, Instant periodStartTime, String venueType,
            String venueCategory1, Integer venueCategory2, Duration qrCodeRenewalInterval) throws CleaCryptoException {
        return this.getOrCreateDynamicLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                        qrCodeRenewalInterval, 24);
    }

    public LocationQrCodeGenerator getOrCreateDynamicLocation(String locationName, Instant periodStartTime,
    String venueType, String venueCategory1, Integer venueCategory2, Duration qrCodeRenewalInterval,
    Integer periodDuration) throws CleaCryptoException {
        return locations.containsKey(locationName) ? locations.get(locationName)
        : this.createDynamicLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalInterval, periodDuration);
    }

    public LocationQrCodeGenerator getOrCreateStaticLocation(String locationName, Instant periodStartTime, String venueType,
            String venueCategory1, Integer venueCategory2, Integer periodDuration) throws CleaCryptoException {
        return locations.containsKey(locationName) ? locations.get(locationName)
                : this.createStaticLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2, periodDuration);
    }

    public LocationQrCodeGenerator getOrCreateStaticLocation(String locationName, Instant periodStartTime, String venueType,
            String venueCategory1, Integer venueCategory2) throws CleaCryptoException {
        return this.getOrCreateStaticLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2, 24);
    }

    public LocationQrCodeGenerator getLocation(String locationName) {
        return locations.get(locationName);
    }
    public LocationQrCodeGenerator getStaffLocation(String locationName) {
        return staffLocations.get(locationName);
    }

    public CleaClient getVisitor(String visitorName) {
        return visitors.get(visitorName);
    }

    public void triggerNewClusterIdenfication() throws IOException, InterruptedException {
        CleaClient client = visitors.values().stream().findAny().orElse(new CleaClient(""));
        client.triggerNewClusterIdenfication();
    }
}
