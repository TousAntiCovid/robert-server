package fr.gouv.clea.scenario;

import fr.gouv.clea.client.service.CleaClient;
import fr.gouv.tacw.qr.LocationQrCodeGenerator;
import fr.inria.clea.lsp.CleaEncryptionException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ScenarioAppContext {
    private final String manualContactTracingAuthorityPublicKey;
    private final String serverAuthorityPublicKey;
    private final String permanentLocationSecretKey;
    private final Map<String, Integer> venueCategories;
    private final Map<String, Integer> venueTypes;
    private Map<String, CleaClient> visitors;
    private Map<String, LocationQrCodeGenerator> locations;

    public ScenarioAppContext() {
        visitors = new HashMap<String, CleaClient>(10);
        manualContactTracingAuthorityPublicKey = "TBD";
        serverAuthorityPublicKey = "TBD";
        permanentLocationSecretKey = "TBD";
        venueCategories = new HashMap<>();
        venueCategories.put("restaurant", 1);
        venueTypes = new HashMap<>();
        venueTypes.put("NUMBER_1", 1);

    }

    public CleaClient getOrCreateVisitor(String name) {
        return visitors.computeIfAbsent(name, newName -> this.createVisitor(newName));
    }

    private CleaClient createVisitor(String name) {
        log.info("Creating visitor " + name);
        return new CleaClient(name);
    }
/*
    public Place getOrCreatePlace(String name) {
        Place result =placeMap.get(name);
        if ( result == null) {
            logger.info("Creating place " + name);
            result = new Place(name);
            placeMap.put(name, result);
        }
        return result;
    } */

    private LocationQrCodeGenerator createLocation(String locationName, Instant periodStartTime,
    String venueType, String venueCategory1, Integer venueCapacity, Duration qrCodeRenewalInterval) throws CleaEncryptionException{
        long qrCodeRenewalIntervalLong = qrCodeRenewalInterval.getSeconds();
        int qrCodeRenewalIntervalExponentCompact = (int)(Math.log(qrCodeRenewalIntervalLong)/Math.log(2));

        LocationQrCodeGenerator location = LocationQrCodeGenerator.builder()
                                                                .countryCode(250) // France QR Code
                                                                .staff(false) // not used in test declaration as of now
                                                                .venueCategory1(venueCategories.get(venueCategory1))
                                                                .venueType(venueTypes.get(venueType))
                                                                .periodDuration(8) // not used in test declaration as of now
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
            String venueType, String venueCategory1, Integer venueCapacity, Duration qrCodeRenewalInterval) throws CleaEncryptionException {
        
        return (locations.containsKey(locationName) ? locations.get(locationName) : this.createLocation(locationName, periodStartTime, venueType, venueCategory1, venueCapacity, qrCodeRenewalInterval));
    }
}
