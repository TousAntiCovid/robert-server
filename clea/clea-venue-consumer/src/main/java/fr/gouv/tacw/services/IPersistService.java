package fr.gouv.tacw.services;

import fr.gouv.tacw.data.DetectedVenue;

public interface IPersistService {

    void checkAndPersist(DetectedVenue detectedVenue);

    void deleteDetectedVenues();
}
