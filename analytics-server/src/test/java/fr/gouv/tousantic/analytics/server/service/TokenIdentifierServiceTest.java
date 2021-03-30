package fr.gouv.tousantic.analytics.server.service;


import fr.gouv.tousantic.analytics.server.model.mongo.TokenIdentifier;
import fr.gouv.tousantic.analytics.server.repository.mongo.TokenIdentifierRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
public class TokenIdentifierServiceTest {

    @Captor
    private ArgumentCaptor<TokenIdentifier> tokenIdentifierArgumentCaptor;

    @Mock
    private TokenIdentifierRepository tokenIdentifierRepository;

    @InjectMocks
    private TokenIdentifierService tokenIdentifierService;


    @Test
    public void shouldSayIdentifierExistWhenItIsInDb() {

        final String tokenIdentifier = "someId";

        Mockito.when(tokenIdentifierRepository.findByIdentifier(tokenIdentifier)).thenReturn(Optional.of(new TokenIdentifier()));

        final boolean result = tokenIdentifierService.tokenIdentifierExist(tokenIdentifier);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void shouldSayIdentifierDoesNotExistWhenItIsNotInDb() {

        final String tokenIdentifier = "someId";

        Mockito.when(tokenIdentifierRepository.findByIdentifier(tokenIdentifier)).thenReturn(Optional.empty());

        final boolean result = tokenIdentifierService.tokenIdentifierExist(tokenIdentifier);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void shouldSaveTokenIdentifierInDb() {

        final String tokenIdentifier = "someId";
        final ZonedDateTime expirationDate = ZonedDateTime.now();
        final TokenIdentifier ti = new TokenIdentifier();

        Mockito.when(tokenIdentifierRepository.save(tokenIdentifierArgumentCaptor.capture())).thenReturn(ti);

        final TokenIdentifier result = tokenIdentifierService.save(tokenIdentifier, expirationDate);

        Assertions.assertThat(result).isEqualTo(ti);

        final TokenIdentifier capturedTokenIdenfier = tokenIdentifierArgumentCaptor.getValue();
        Assertions.assertThat(capturedTokenIdenfier.getId()).isNull();
        Assertions.assertThat(capturedTokenIdenfier.getIdentifier()).isEqualTo(tokenIdentifier);
        Assertions.assertThat(capturedTokenIdenfier.getExpirationDate()).isEqualTo(expirationDate);


    }
}