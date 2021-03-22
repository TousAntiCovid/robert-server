package fr.gouv.clea.consumer.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
@TestPropertySource("classpath:application.yml")
class VenueConsumerConfigurationIntegrationTest {

    @Autowired
    private VenueConsumerConfiguration config;

    @Test
    void should_get_expected_values() {
        assertThat(config.getDurationUnitInSeconds()).isEqualTo(1800);
        assertThat(config.getDriftBetweenDeviceAndOfficialTimeInSecs()).isEqualTo(300);
        assertThat(config.getCleaClockDriftInSecs()).isEqualTo(300);
        assertThat(config.getRetentionDurationInDays()).isEqualTo(14);
    }
}