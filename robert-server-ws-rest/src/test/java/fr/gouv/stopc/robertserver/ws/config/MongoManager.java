package fr.gouv.stopc.robertserver.ws.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@RequiredArgsConstructor
public class MongoManager implements TestExecutionListener {

    private static MongoTemplate template;

    private static final MongoDBContainer MONGO = new MongoDBContainer(
            DockerImageName.parse("mongo:4.2.5")
    );

    static {
        MONGO.start();
        System.setProperty("spring.data.mongodb.uri", MONGO.getReplicaSetUrl());
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        template = testContext.getApplicationContext().getBean(MongoTemplate.class);
        template.getCollectionNames().forEach((collection) -> template.dropCollection(collection));
    }
}
