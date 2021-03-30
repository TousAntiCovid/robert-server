package fr.gouv.tac.analytics.server.repository.mongo;

import fr.gouv.tac.analytics.server.model.mongo.TokenIdentifier;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TokenIdentifierRepository extends MongoRepository<TokenIdentifier, String> {


    Optional<TokenIdentifier> findByIdentifier(String identifier);
}
