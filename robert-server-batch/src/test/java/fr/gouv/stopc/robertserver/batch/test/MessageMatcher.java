package fr.gouv.stopc.robertserver.batch.test;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.Registration;
import org.assertj.core.api.*;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageMatcher implements TestExecutionListener {

    private static MongoOperations mongoOperations;

    private static RobertClock clock;

    @Override
    public void beforeTestMethod(@NonNull TestContext testContext) {
        mongoOperations = testContext.getApplicationContext().getBean(MongoOperations.class);
        clock = testContext.getApplicationContext().getBean(RobertClock.class);
    }

    public static ListAssert<Contact> assertThatContactsToProcess() {
        return assertThat(mongoOperations.find(new Query(), Contact.class))
                .as("Mongodb contact_to_process collection");
    }

    public static ObjectAssert<Registration> assertThatRegistrationForIdA(final String idA) {
        final var query = new Query().addCriteria(Criteria.where("permanentIdentifier").is(idA.getBytes()));
        return assertThat(mongoOperations.find(query, Registration.class))
                .hasSize(1)
                .first()
                .as("Registration for idA %s", idA);
    }

    public static AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> assertThatEpochExpositionsForIdA(
            String idA) {
        return assertThatRegistrationForIdA(idA)
                .extracting(Registration::getExposedEpochs)
                .asList();
    }

    public static AbstractInstantAssert<?> assertThatLatestRiskEpochForIdA(String idA) {
        var EPOCH_INSTANT = new InstanceOfAssertFactory<>(
                Integer.class, value -> AssertionsForClassTypes.assertThat(clock.atEpoch(value).asInstant())
        );
        return assertThatRegistrationForIdA(idA)
                .extracting(Registration::getLatestRiskEpoch, EPOCH_INSTANT)
                .as("Last risk update");
    }

    public static AbstractInstantAssert<?> assertThatLastContactTimestampForIdA(String idA) {
        final var NTP_INSTANT = new InstanceOfAssertFactory<>(
                Long.class, value -> AssertionsForClassTypes.assertThat(clock.atNtpTimestamp(value).asInstant())
        );
        return assertThatRegistrationForIdA(idA)
                .extracting(Registration::getLastContactTimestamp, NTP_INSTANT)
                .as("Last contact timestamp");
    }
}
