package fr.gouv.stopc.robert.server.batch.utils;

import lombok.Getter;

/**
 * Utility class used by Registration or Contact Id Mapping processors
 * to get the mapped id.
 * The last mapped id gives the number of mapped items and is used
 * by the partitioner to split entries among the available workers.
 * Workers will process in parallel.
 * Since the id mapping is run sequentially, no need to synchronize
 * incrementCurrentIdOfItemIdMapping.
 */
@Getter
public class ItemProcessingCounterUtils {

    /** Holder for multithreading */
    private static class ItemProcessingCounterUtilsHolder {
        private final static ItemProcessingCounterUtils instance = new ItemProcessingCounterUtils();
    }

    private long currentIdFromItemIdMapping = 0;

    public int numberOfProcessedContacts = 0;

    public int numberOfProcessedRegistrations = 0;

    private ItemProcessingCounterUtils() {
        // Do not instantiate me directly! Use getInstance() instead.
    }

    public static ItemProcessingCounterUtils getInstance() {
        return ItemProcessingCounterUtilsHolder.instance;
    }

    public synchronized int addNumberOfProcessedContacts(int contactCount) {
        numberOfProcessedContacts += contactCount;
        return numberOfProcessedContacts;
    }

    public synchronized int addNumberOfProcessedRegistrations(int registrationCount) {
        numberOfProcessedRegistrations += registrationCount;
        return numberOfProcessedRegistrations;
    }

    public long incrementCurrentIdOfItemIdMapping() {
        currentIdFromItemIdMapping++;
        return currentIdFromItemIdMapping;
    }

    public void resetCounters() {
        this.numberOfProcessedContacts = 0;
        this.numberOfProcessedRegistrations = 0;
        this.currentIdFromItemIdMapping = 0;
    }
}
