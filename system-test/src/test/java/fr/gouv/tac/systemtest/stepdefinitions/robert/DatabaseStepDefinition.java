package fr.gouv.tac.systemtest.stepdefinitions.robert;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import fr.gouv.tac.systemtest.ScenarioAppContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;

import javax.inject.Inject;

import static fr.gouv.tac.systemtest.config.MongoConfig.ID_TABLE_COLLECTION_NAME;
import static fr.gouv.tac.systemtest.config.MongoConfig.ROBERT_DATABASE_NAME;

public class DatabaseStepDefinition {

    private final ScenarioAppContext scenarioAppContext;

    @Inject
    public DatabaseStepDefinition(final ScenarioAppContext scenarioAppContext) {
        this.scenarioAppContext = scenarioAppContext;
    }

    @Given("one user is present in database")
    public void one_user_is_present_in_database() {
        final MongoClient mongoClient = new MongoClient("localhost", 27017);
        final MongoCollection<Document> collection = mongoClient.getDatabase(ROBERT_DATABASE_NAME).getCollection(ID_TABLE_COLLECTION_NAME);

        // As users are anonymized in database, we consider that an empty base is
        Assertions.assertEquals(1L, collection.countDocuments());
    }

    @Then("no user are present in database")
    public void no_user_are_present_in_database() throws InterruptedException {

        final MongoClient mongoClient = new MongoClient("localhost", 27017);
        final MongoCollection<Document> collection = mongoClient.getDatabase(ROBERT_DATABASE_NAME).getCollection(ID_TABLE_COLLECTION_NAME);

        // As users are anonymized in database, we consider that an empty base means user is deleted
        Assertions.assertEquals(0L, collection.countDocuments());
    }
}
