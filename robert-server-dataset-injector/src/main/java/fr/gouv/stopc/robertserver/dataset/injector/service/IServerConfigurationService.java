package fr.gouv.stopc.robertserver.dataset.injector.service;

public interface IServerConfigurationService {

    /**
     * TpStart in NTP seconds
     * 
     * @return the time the ROBERT service was started (permanent, never changes,
     *         not tied to an instance)
     */
    long getServiceTimeStart();

    /**
     * Country code of the current application (1 byte)
     * 
     * @return
     */
    byte getServerCountryCode();

    /**
     * @return The duration of an epoch (in seconds)
     */
    int getEpochDurationSecs();
}
