package fr.gouv.tacw.ws.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.utils.TimeUtils;

@Service
public class TestTimestampService {
	@Value("${tacw.database.visit_token_retention_period_days}")
	private long visitTokenRetentionPeriodDays;
	
	/**
	 * @return a valid timestamp five days ago.
	 */
	public long validTimestamp() {
		return TimeUtils.roundedCurrentTimeTimestamp() - TimeUnit.DAYS.toSeconds(5);
	}
	public String validTimestampString() {
		return Long.toString(this.validTimestamp());
	}

	/**
	 * @return a timestamp of retention time - 5 days ago, i.e. impossible to check because tokens
	 * are already purged.
	 */
	public long preRetentionTimeTimestamp() {
		return TimeUtils.roundedCurrentTimeTimestamp() - TimeUnit.DAYS.toSeconds(visitTokenRetentionPeriodDays + 5);
	}
	public String preRetentionTimeTimestampString() {
		return Long.toString(this.preRetentionTimeTimestamp());
	}
	
	/**
	 * @return a timestamp of retention time
	 */
	public long retentionTimeTimestamp() {
		return TimeUtils.roundedCurrentTimeTimestamp() - TimeUnit.DAYS.toSeconds(visitTokenRetentionPeriodDays);
	}
	
	/**
	 * @return a timestamp 10 days in the future.
	 */
	public long futureTimestamp() {
		return TimeUtils.roundedCurrentTimeTimestamp() + TimeUnit.DAYS.toSeconds(10);
	}
	public String futureTimestampString() {
		return Long.toString(this.futureTimestamp());
	}
}
