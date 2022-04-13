package fr.gouv.stopc.robertserver.ws.test;

import fr.gouv.stopc.robertserver.database.model.Registration;
import org.assertj.core.api.ListAssert;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class MongodbManager implements TestExecutionListener {

    private static final MongoDBContainer mongodbContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.2.11")
    );

    private static MongoOperations mongoOperations;

    static {
        mongodbContainer.start();
        System.setProperty("spring.data.mongodb.uri", mongodbContainer.getReplicaSetUrl("robert"));
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        mongoOperations = testContext.getApplicationContext().getBean(MongoOperations.class);
    }

    public static ListAssert<Registration> assertThatRegistrations() {
        return assertThat(mongoOperations.find(new Query(), Registration.class));
    }
}
