package fr.gouv.clea.scenario;

import fr.gouv.clea.client.service.CleaClient;
import fr.gouv.tacw.qr.LocationQrCodeGenerator;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ScenarioAppContext {
    private Map<String, CleaClient> visitors;

    public ScenarioAppContext() {
        visitors = new HashMap<String, CleaClient>(10);
    }

    public CleaClient getOrCreateVisitor(String name) {
        return visitors.computeIfAbsent(name, newName -> this.createVisitor(newName));
    }

    private CleaClient createVisitor(String name) {
        log.info("Creating visitor " + name);
        return new CleaClient(name);
    }

    public LocationQrCodeGenerator getOrCreatelocation(String locationName, Instant periodStartTime, String venueType,String venueCategory1,int venueCapacity, String qrCodeRenewalInterval){
        
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
}
