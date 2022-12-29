package fr.gouv.stopc.robertserver.dataset.injector.service.impl;

import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.dataset.injector.service.IServerConfigurationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Facade for server configuration parameters and keys
 */
@Service
public class ServerConfigurationServiceImpl implements IServerConfigurationService {

    @Value("${robert.server.time-start:2020-06-01}")
    private String timeStart;

    @Value("${robert.server.country-code:0x21}")
    private byte countryCode;

    private long timeStartNtp;

    /**
     * Initializes the timeStartNtp field
     */
    @PostConstruct
    private void initTimeStartNtp() {
        final LocalDate ld = LocalDate.parse(this.timeStart);
        final ZonedDateTime zdt = ld.atStartOfDay().atZone(ZoneId.of("UTC"));
        timeStartNtp = TimeUtils.convertUnixMillistoNtpSeconds(zdt.toInstant().toEpochMilli());

    }

    @Override
    public long getServiceTimeStart() {
        return this.timeStartNtp;
    }

    @Override
    public byte getServerCountryCode() {
        return this.countryCode;
    }

    @Override
    public int getEpochDurationSecs() {
        return TimeUtils.EPOCH_DURATION_SECS;
    }

}
