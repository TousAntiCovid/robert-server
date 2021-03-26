package fr.gouv.clea.scenario;

public class ScenarioAppContext {
    public CleaClient getOrCreateVisitor(String name) {
        CleaClient result = visitorMap.get(name);
        if ( result == null) {
            logger.info("Creating visitor " + name);
            result = new CleaClient(name);
            visitorMap.put(name, result);
        }
        return result;
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
