package fr.gouv.stopc.robert.server.batch;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ActiveProfiles({ "legacy", "test" })
@TestPropertySource(properties = { "spring.commandLineRunner.reassesRisk=on" })
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ReassessRiskLineRunnerTest {

    private final ApplicationContext context;

    private final MongoTemplate mongoTemplate;

    private final RobertClock robertClock;

    @Test
    void registration_should_not_be_updated_when_not_at_risk_and_not_notified() {
        // Given
        byte[] rndBytes = getRandomId();

        var registration = Registration.builder()
                .atRisk(false)
                .isNotified(false)
                .permanentIdentifier(rndBytes)
                .build();
        mongoTemplate.save(registration);

        // When
        launchReassesRiskLineRunner();

        // Then
        List<Registration> updatedRegistration = mongoTemplate.find(
                new Query()
                        .addCriteria(Criteria.where("permanentIdentifier").is(rndBytes)),
                Registration.class
        );

        assertThat(updatedRegistration).hasSize(1);
        assertThat(updatedRegistration.get(0)).as("Object has not been updated").isEqualTo(registration);
    }

    @Test
    void risk_level_should_be_reset_when_at_risk_and_notified_and_last_contact_date_is_above_7_days_ago() {
        // Given
        byte[] rndBytes = getRandomId();

        final var nowMinus8DaysNtpTimestamp = robertClock.at(
                Instant.now()
                        .truncatedTo(DAYS)
                        .minus(8, DAYS)
        ).asNtpTimestamp();

        mongoTemplate.save(
                Registration.builder()
                        .permanentIdentifier(rndBytes)
                        .atRisk(true)
                        .isNotified(true)
                        .latestRiskEpoch(4808)
                        .lastContactTimestamp(nowMinus8DaysNtpTimestamp)
                        .build()
        );

        // When
        launchReassesRiskLineRunner();

        // Then
        Registration processedRegistration = mongoTemplate
                .findOne(
                        new Query().addCriteria(Criteria.where("permanentIdentifier").is(rndBytes)),
                        Registration.class
                );

        AssertionsForClassTypes.assertThat(processedRegistration).as("Registration is null").isNotNull();
        AssertionsForClassTypes.assertThat(processedRegistration.isAtRisk()).as("Registration is not at risk")
                .isFalse();
        AssertionsForClassTypes.assertThat(processedRegistration.isNotified())
                .as("Registration is notified for current risk").isTrue();
    }

    private byte[] getRandomId() {
        SecureRandom sr = new SecureRandom();
        byte[] rndBytes = new byte[5];
        sr.nextBytes(rndBytes);
        return rndBytes;
    }

    private void launchReassesRiskLineRunner() {
        var lineRunner = context.getBean(ReassessRiskLineRunner.class);
        lineRunner.run("");
    }
}
