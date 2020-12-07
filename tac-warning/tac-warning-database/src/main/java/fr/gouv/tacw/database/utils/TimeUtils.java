package fr.gouv.tacw.database.utils;

public final class TimeUtils {
    //  Number of seconds to fill the gap between UNIX timestamp (1/1/1970) and NTP timestamp (1/1/1900)
    public final static long SECONDS_FROM_01_01_1900 = 2208988800L;
    
    // TAC-Warning does not keep real visit time but is rounded to the quarter of hour
    public final static long TIME_ROUNDING = 900;
    
    // The number of TIME UNIT in a day given the TIME_ROUNDING
    public final static long DAY_UNIT = (60 * 60 * 24) / TIME_ROUNDING;
    
    /**
     * Convert UNIX timestamp in milliseconds to NTP seconds
     * @param unixTimeInMillis UNIX time in millis
     * @return time converted in NTP in seconds
     */
    public static long convertUnixMillistoNtpSeconds(final long unixTimeInMillis) {
        return (unixTimeInMillis / 1000) + SECONDS_FROM_01_01_1900;
    }
    
    /**
     * Get timestamp rounded to TIME_ROUNDING used by TAC Warning.
     * @param timestamp the timestamp in seconds
     * @return the rounded timestamp
     */
    public static Long roundedTimestamp(long timestamp) {
        return timestamp - (timestamp % TIME_ROUNDING);
    }
    
    public static long roundedCurrentTimeTimestamp() {
    	long ntpSeconds = convertUnixMillistoNtpSeconds(System.currentTimeMillis());
    	return roundedTimestamp(ntpSeconds);
    }
}
