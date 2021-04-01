package fr.gouv.clea.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BatchConstants {

    public static final String EXPOSED_VISITS_TABLE = "exposed_visits";
    public static final String SINGLE_PLACE_CLUSTER_PERIOD_TABLE = "cluster_periods";
    public static final String SINGLE_PLACE_CLUSTER_TABLE = "cluster_periods";
    public static final String LTID_COLUMN = "ltid";
    public static final String PERIOD_COLUMN = "period_start";
    public static final String TIMESLOT_COLUMN = "timeslot";
    
    public static final String LTID_PARAM = "ltid";
    public static final String CLUSTERMAP_JOB_CONTEXT_KEY = "clusterMap";
}
