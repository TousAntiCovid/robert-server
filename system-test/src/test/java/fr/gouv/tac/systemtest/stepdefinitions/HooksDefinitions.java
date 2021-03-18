package fr.gouv.tac.systemtest.stepdefinitions;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import io.cucumber.java.Before;
import org.bson.Document;

import static fr.gouv.tac.systemtest.config.MongoConfig.ID_TABLE_COLLECTION_NAME;
import static fr.gouv.tac.systemtest.config.MongoConfig.ROBERT_DATABASE_NAME;

public class HooksDefinitions {

    @Before("@beforeEmptyBase")
    public void emptyBase() {
        final MongoClient mongoClient = new MongoClient("localhost", 27017);
        final MongoCollection<Document> collection = mongoClient.getDatabase(ROBERT_DATABASE_NAME).getCollection(ID_TABLE_COLLECTION_NAME);
        collection.drop();
        // As users are anonymized in database, we consider that an empty base means user is deleted
    }
}
