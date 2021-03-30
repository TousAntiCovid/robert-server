package fr.gouv.clea.scenario;

import fr.gouv.clea.client.service.CleaClient;
import fr.gouv.clea.qr.LocationQrCodeGenerator;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import fr.inria.clea.lsp.exception.CleaEncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class ScenarioAppContext {
    private final Map<String, Integer> venueCategories;
    private final Map<String, Integer> venueTypes;
    private Map<String, CleaClient> visitors;
    private Map<String, LocationQrCodeGenerator> locations;
    private String manualContactTracingAuthorityPublicKey;
    private String serverAuthorityPublicKey;

    public ScenarioAppContext() throws Exception {
        visitors = new HashMap<String, CleaClient>(10);
        locations = new HashMap<String, LocationQrCodeGenerator>(10);
        this.generateKeys();
        venueTypes = new HashMap<String, Integer> (
                Map.of("restaurant", 1));
        venueCategories = new HashMap<String, Integer> (
                Map.of("NUMBER_1", 1));
    }

    public void generateKeys() throws Exception {
        CleaEciesEncoder cleaEciesEncoder = new CleaEciesEncoder();
        String[] serverAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        this.serverAuthorityPublicKey = serverAuthorityKeyPair[1];
        System.out.println("Server Authority Private Key: " + serverAuthorityKeyPair[0]);
        System.out.println("Server Authority Public Key : " + this.serverAuthorityPublicKey);

        String[] manualContactTracingAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        this.manualContactTracingAuthorityPublicKey = serverAuthorityKeyPair[1];
        System.out.println("Server Authority Private Key: " + serverAuthorityKeyPair[0]);
        System.out.println("Server Authority Public Key : " + this.manualContactTracingAuthorityPublicKey);
    }

    public CleaClient getOrCreateVisitor(String name) {
        return visitors.computeIfAbsent(name, newName -> this.createVisitor(newName));
    }

    private CleaClient createVisitor(String name) {
        log.info("Creating visitor " + name);
        return new CleaClient(name);
    }

    private LocationQrCodeGenerator createLocation(String locationName, Instant periodStartTime,
    String venueType, String venueCategory1, Integer venueCapacity, Duration qrCodeRenewalInterval) throws CleaCryptoException{
        log.info("Creating location " + locationName);
        long qrCodeRenewalIntervalLong = qrCodeRenewalInterval.getSeconds();
        int qrCodeRenewalIntervalExponentCompact = (int)(Math.log(qrCodeRenewalIntervalLong)/Math.log(2));

        final String permanentLocationSecretKey = Hex.toHexString(UUID.randomUUID().toString().getBytes());
        LocationQrCodeGenerator location = LocationQrCodeGenerator.builder()
                                                                .countryCode(250) // France Country Code
                                                                .staff(false) // not used in test declaration as of now
                                                                .venueCategory1(venueCategories.get(venueCategory1))
                                                                .venueType(venueTypes.get(venueType))
                                                                .periodDuration(24) // not used in test declaration as of now
                                                                .periodStartTime(periodStartTime)
                                                                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact)
                                                                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                                                                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                                                                .permanentLocationSecretKey(permanentLocationSecretKey)
                                                                .build();
        locations.put(locationName, location);
        return location;
    }

    public LocationQrCodeGenerator getOrCreateLocation(String locationName, Instant periodStartTime,
            String venueType, String venueCategory1, Integer venueCapacity, Duration qrCodeRenewalInterval) throws CleaCryptoException {
        return locations.containsKey(locationName) ?
                locations.get(locationName) :
                this.createLocation(locationName, periodStartTime, venueType, venueCategory1, venueCapacity, qrCodeRenewalInterval);
    }

    public LocationQrCodeGenerator getLocation(String locationName){
        return locations.get(locationName);
    }

    public CleaClient getVisitor(String visitorName){
        return visitors.get(visitorName);
    }
}
