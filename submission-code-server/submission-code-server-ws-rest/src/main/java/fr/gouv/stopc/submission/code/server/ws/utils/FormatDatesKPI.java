package fr.gouv.stopc.submission.code.server.ws.utils;

import java.time.*;

public final class FormatDatesKPI {
    /**
     * The method calculates the date dateTmpTo in UTC. This date uses for search the KPI (codes used or expired until this date).
     * @param dateTmpTo
     * @param targetZoneId
     * @return
     */
    public static OffsetDateTime normaliseDateTo(LocalDate dateTmpTo, String targetZoneId) {
        LocalDateTime localDateTimeToStart = dateTmpTo.plusDays(1L).atStartOfDay();
        ZoneOffset zoneOffset= OffsetDateTime.now(ZoneId.of(targetZoneId)).getOffset();
        OffsetDateTime dateToZone = OffsetDateTime.of(localDateTimeToStart, zoneOffset);
        return dateToZone.withOffsetSameInstant(ZoneOffset.UTC);
    }

    /**
     * The method calculates the date dateFrom in UTC. This date uses for search the KPI (codes used since this date).
     * @param dateFrom
     * @param targetZoneId
     * @return
     */
    public static OffsetDateTime normaliseDateFrom(LocalDate dateFrom, String targetZoneId) {
        ZoneOffset zoneOffset= OffsetDateTime.now(ZoneId.of(targetZoneId)).getOffset();
        LocalDateTime localDateTimeStart = dateFrom.atStartOfDay();
        OffsetDateTime dateBeginZone = OffsetDateTime.of(localDateTimeStart, zoneOffset);
        return dateBeginZone.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
