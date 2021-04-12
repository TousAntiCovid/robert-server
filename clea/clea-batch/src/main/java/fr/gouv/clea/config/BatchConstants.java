package fr.gouv.clea.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BatchConstants {

    public static final String EXPOSED_VISITS_TABLE = "exposed_visits";
    public static final String SINGLE_PLACE_CLUSTER_PERIOD_TABLE = "cluster_periods";
    public static final String PERIOD_COLUMN = "period_start";
    public static final String TIMESLOT_COLUMN = "timeslot";
    
    public static final String LTID_COL = "ltid";
    public static final String VENUE_TYPE_COL = "venue_type";
    public static final String VENUE_CAT1_COL = "venue_category1";
    public static final String VENUE_CAT2_COL = "venue_category2";
    public static final String PERIOD_START_COL = "period_start";
    public static final String FIRST_TIMESLOT_COL = "first_timeslot";
    public static final String LAST_TIMESLOT_COL = "last_timeslot";
    public static final String CLUSTER_START_COL = "cluster_start";
    public static final String CLUSTER_DURATION_COL = "cluster_duration_in_seconds";
    public static final String RISK_LEVEL_COL = "risk_level";
    public static final String CLUSTER_INDEX_FILENAME = "clusterIndex.json";
    public static final String JSON_FILE_EXTENSION = ".json";
}
