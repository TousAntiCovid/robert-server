package fr.gouv.stopc.robert.server.batch.service;

import java.util.List;

import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;

public interface BatchRegistrationService {

    /**
     * Keep epochs within the contagious period
     * @param exposedEpochs
     * @return
     */
    List<EpochExposition> getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(List<EpochExposition> exposedEpochs,
            int currentEpochId, int contagiousPeriod, int epochDuration);

    boolean updateRegistrationIfRisk(Registration registration, long timeStart, double riskThreshold);

}