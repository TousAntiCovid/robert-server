package fr.gouv.stopc.e2e.external.common.utils;

public final class TimeUtils {

    // Number of seconds to fill the gap between UNIX timestamp (1/1/1970) and NTP
    // timestamp (1/1/1900)
    public final static long SECONDS_FROM_01_01_1900_TO_01_01_1970 = 2208988800L;

    // Epoch duration is 15 minutes so 15 * 60 = 900 seconds
    public final static int EPOCH_DURATION_SECS = 900;

    private TimeUtils() {
        throw new AssertionError();
    }

    public static long convertNTPSecondsToUnixMillis(long ntpTime) {
        return (ntpTime - SECONDS_FROM_01_01_1900_TO_01_01_1970) * 1000;
    }

    /**
     * Convert UNIX timestamp in milliseconds to NTP seconds
     * 
     * @param unixTimeInMillis UNIX time in millis
     * @return time converted in NTP in seconds
     */
    public static long convertUnixMillistoNtpSeconds(final long unixTimeInMillis) {
        return (unixTimeInMillis / 1000) + SECONDS_FROM_01_01_1900_TO_01_01_1970;
    }

    /**
     * ref {@see <a href=
     * "https://stackoverflow.com/questions/29112071/how-to-convert-ntp-time-to-unix-epoch-time-in-c-language-linux/29138806#29138806">Stackoverflow
     * NTP to unix time interval</a>}
     * 
     * @param from the timestamp from which to calculate the number of epochs (e.g.
     *             ROBERT service time start); as NTP timestamp in seconds
     * @param to   the timestamp until which to calculate the number of epochs (e.g.
     *             current time); as NTP timestamp in seconds
     * @return number of epochs between the two dates (i.e. current epoch)
     */
    public static int getNumberOfEpochsBetween(final long from, final long to) {
        long numberEpochs = (to - from) / EPOCH_DURATION_SECS;
        return Integer.parseInt(Long.toString(numberEpochs));
    }

    public static int getCurrentEpochFromTo(final long timeStart, final long timeTo) {
        return getNumberOfEpochsBetween(timeStart, convertUnixMillistoNtpSeconds(timeTo));
    }
}
