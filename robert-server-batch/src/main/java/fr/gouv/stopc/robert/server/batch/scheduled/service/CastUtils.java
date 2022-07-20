package fr.gouv.stopc.robert.server.batch.scheduled.service;

public class CastUtils {

    public static long castIntegerToLong(int x, int nbOfSignificantBytes) {
        int shift = nbOfSignificantBytes * 8;
        return Integer.toUnsignedLong(x << shift >>> shift);
    }

    public static long castLong(long x, int nbOfSignificantBytes) {
        int shift = (Long.BYTES - nbOfSignificantBytes) * 8;
        return x << shift >>> shift;
    }
}
