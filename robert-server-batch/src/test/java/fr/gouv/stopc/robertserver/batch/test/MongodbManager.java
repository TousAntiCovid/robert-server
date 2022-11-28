package fr.gouv.stopc.robertserver.batch.test;

import com.mongodb.client.MongoCollection;
import fr.gouv.stopc.robertserver.common.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.Registration.RegistrationBuilder;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Function;

import static java.util.function.Function.identity;

public class MongodbManager implements TestExecutionListener {

    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(
            DockerImageName.parse("mongo:4.2.11")
    );

    private static MongoOperations mongoOperations;

    private static RobertClock clock;

    static {
        MONGO_DB_CONTAINER.start();
        System.setProperty(
                "spring.data.mongodb.uri",
                MONGO_DB_CONTAINER.getReplicaSetUrl("robert") + "?connectTimeoutMS=1000&socketTimeoutMS=1000"
        );
    }

    @Override
    public void beforeTestMethod(@NonNull TestContext testContext) {
        mongoOperations = testContext.getApplicationContext().getBean(MongoOperations.class);
        mongoOperations.getCollectionNames()
                .stream()
                .map(mongoOperations::getCollection)
                .forEach(MongoCollection::drop);
        clock = testContext.getApplicationContext().getBean(RobertClock.class);
    }

    public static void givenRegistrationExistsForIdA(final String idA) {
        givenRegistrationExistsForIdA(idA, identity());
    }

    public static Registration givenRegistrationExistsForIdA(final String idA,
            final Function<RegistrationBuilder, RegistrationBuilder> transformer) {
        final var registration = Registration.builder()
                .permanentIdentifier(idA.getBytes());
        return mongoOperations.save(transformer.apply(registration).build());
    }

    public static ContactFactory.ContactBuilder givenPendingContact() {
        return new ContactFactory.ContactBuilder(mongoOperations, clock);
    }

}
