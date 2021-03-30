package fr.gouv.tousantic.analytics.server.config.mongodb;

import fr.gouv.tousantic.analytics.server.model.mongo.TokenIdentifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class MongoIndexCreationListener {

    private static final String TOKEN_IDENTIFIER_IDX_NAME = "TokenIdentifierIdentifierUk";
    private static final String TOKEN_IDENTIFIER_EXPIRATION_DATE_IDX_NAME = "TokenIdentifierExpirationDateIdx";

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexesCreationAfterStartup() {

        log.info("Mongo ensureIndexesCreationAfterStartup init");
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ensureTokenIdentifierCreationDateIndex();
        ensureTokenIdentifierIdentifierUniqueIndex();


        stopWatch.stop();
        log.info("Mongo ensureIndexesCreationAfterStartup takes: {} ms", stopWatch.getTotalTimeMillis());
    }


    public void ensureTokenIdentifierIdentifierUniqueIndex() {
        mongoTemplate.indexOps(TokenIdentifier.class).ensureIndex(new Index().named(TOKEN_IDENTIFIER_IDX_NAME).on(TokenIdentifier.IDENTIFIER_FIELD_NAME, Sort.Direction.ASC).unique());
    }

    public void ensureTokenIdentifierCreationDateIndex() {
        ensureExpirationIndex(TokenIdentifier.class, TOKEN_IDENTIFIER_EXPIRATION_DATE_IDX_NAME, TokenIdentifier.EXPIRATION_DATE_FIELD_NAME, Duration.ofSeconds(0));
    }

    private void ensureExpirationIndex(final Class entity, final String indexName, final String field, final Duration duration) {
        final Optional<IndexInfo> alreadyExistingValidIndex = mongoTemplate.indexOps(entity)
                .getIndexInfo()
                .stream()
                .filter(indexInfo -> indexName.contentEquals(indexInfo.getName()) && indexInfo.getExpireAfter().isPresent() && indexInfo.getExpireAfter().get().equals(duration))
                .findFirst();

        if (alreadyExistingValidIndex.isEmpty()) {
            log.info("No suitable index found with name {}, drop it and recreate it", indexName);
            mongoTemplate.indexOps(entity).dropIndex(indexName);
            mongoTemplate.indexOps(entity).ensureIndex(
                    new Index().named(indexName).on(field, Sort.Direction.ASC).expire(duration).background()
            );
            log.info("Index {}, created", indexName);
        }
    }
}
