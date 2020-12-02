package fr.gouv.tacw.ws.service;

import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.utils.TimeUtils;

@Service
public class TestTimestampService {
	public TestTimestampService() {
	}
	
	/**
	 * @return a valid timestamp five days ago.
	 */
	public long validTimestamp() {
		return TimeUtils.roundedCurrentTimeTimestamp() - TimeUtils.TIME_ROUNDING * 4 * 24 * 5;
	}
	public String validTimestampString() {
		return Long.toString(this.validTimestamp());
	}

	/**
	 * @return a timestamp 10 days in the future.
	 */
	public long futureTimestamp() {
		return TimeUtils.roundedCurrentTimeTimestamp() + TimeUtils.TIME_ROUNDING * 4 * 24 * 10;
	}
	public String futureTimestampString() {
		return Long.toString(this.futureTimestamp());
	}
}