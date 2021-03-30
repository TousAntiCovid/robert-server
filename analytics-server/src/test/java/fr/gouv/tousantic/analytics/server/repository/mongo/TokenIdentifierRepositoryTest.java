package fr.gouv.tousantic.analytics.server.repository.mongo;

import fr.gouv.tousantic.analytics.server.config.mongodb.MongoConfiguration;
import fr.gouv.tousantic.analytics.server.config.mongodb.MongoIndexCreationListener;
import fr.gouv.tousantic.analytics.server.config.validation.ValidationConfiguration;
import fr.gouv.tousantic.analytics.server.model.mongo.TokenIdentifier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

import javax.validation.ConstraintViolationException;
import java.time.ZonedDateTime;
import java.util.Optional;

@ActiveProfiles(value = "test")
@DataMongoTest
@Import(value = {MongoConfiguration.class, MongoIndexCreationListener.class, ValidationConfiguration.class})
public class TokenIdentifierRepositoryTest {

    @Autowired
    private TokenIdentifierRepository tokenIdentifierRepository;

    @BeforeEach
    public void setUp() {
        tokenIdentifierRepository.deleteAll();
    }

    @Test
    public void shouldSaveTokenIdentifierThenFindItById() {

        final TokenIdentifier tokenIdentifier = TokenIdentifier.builder().identifier("someIdentfier").expirationDate(ZonedDateTime.now()).build();

        final TokenIdentifier savedTokenIdentifier = tokenIdentifierRepository.save(tokenIdentifier);
        Assertions.assertThat(savedTokenIdentifier.getId()).isNotBlank();
        Assertions.assertThat(savedTokenIdentifier.getIdentifier()).isEqualTo(tokenIdentifier.getIdentifier());
        Assertions.assertThat(savedTokenIdentifier.getExpirationDate()).isEqualToIgnoringSeconds(tokenIdentifier.getExpirationDate());


        final Optional<TokenIdentifier> result = tokenIdentifierRepository.findById(savedTokenIdentifier.getId());
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getId()).isEqualTo(savedTokenIdentifier.getId());
        Assertions.assertThat(result.get().getIdentifier()).isEqualTo(savedTokenIdentifier.getIdentifier());
        Assertions.assertThat(savedTokenIdentifier.getExpirationDate()).isEqualToIgnoringSeconds(tokenIdentifier.getExpirationDate());
    }

    @Test
    public void shouldSaveTokenIdentifierThenFindItByIdentifier() {

        final TokenIdentifier tokenIdentifier = TokenIdentifier.builder().identifier("someIdentfier").expirationDate(ZonedDateTime.now()).build();

        final TokenIdentifier savedTokenIdentifier = tokenIdentifierRepository.save(tokenIdentifier);
        Assertions.assertThat(savedTokenIdentifier.getId()).isNotBlank();
        Assertions.assertThat(savedTokenIdentifier.getIdentifier()).isEqualTo(tokenIdentifier.getIdentifier());
        Assertions.assertThat(savedTokenIdentifier.getExpirationDate()).isEqualToIgnoringSeconds(tokenIdentifier.getExpirationDate());


        final Optional<TokenIdentifier> result = tokenIdentifierRepository.findByIdentifier(tokenIdentifier.getIdentifier());
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getId()).isEqualTo(savedTokenIdentifier.getId());
        Assertions.assertThat(result.get().getIdentifier()).isEqualTo(savedTokenIdentifier.getIdentifier());
        Assertions.assertThat(savedTokenIdentifier.getExpirationDate()).isEqualToIgnoringSeconds(tokenIdentifier.getExpirationDate());
    }

    @Test()
    public void shouldNotAllowToSaveTheSameIdentifierTwice() {

        final TokenIdentifier tokenIdentifier1 = TokenIdentifier.builder().identifier("someIdentfier").expirationDate(ZonedDateTime.now()).build();
        final TokenIdentifier tokenIdentifier2 = TokenIdentifier.builder().identifier("someIdentfier").expirationDate(ZonedDateTime.now()).build();

        tokenIdentifierRepository.save(tokenIdentifier1);
        Assertions.assertThatExceptionOfType(DuplicateKeyException.class).isThrownBy(() -> tokenIdentifierRepository.save(tokenIdentifier2));
    }

    @Test
    public void shouldNotAllowToSaveWithoutIdentifier() {
        final TokenIdentifier tokenIdentifier = TokenIdentifier.builder().expirationDate(ZonedDateTime.now()).build();
        Assertions.assertThatThrownBy(() -> tokenIdentifierRepository.save(tokenIdentifier)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    public void shouldNotAllowToSaveWithoutExpiration() {
        final TokenIdentifier tokenIdentifier = TokenIdentifier.builder().identifier("someIdentifier").build();
        Assertions.assertThatThrownBy(() -> tokenIdentifierRepository.save(tokenIdentifier)).isInstanceOf(ConstraintViolationException.class);
    }
}