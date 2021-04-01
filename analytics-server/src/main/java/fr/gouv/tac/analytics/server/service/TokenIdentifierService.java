package fr.gouv.tac.analytics.server.service;

import fr.gouv.tac.analytics.server.model.mongo.TokenIdentifier;
import fr.gouv.tac.analytics.server.repository.mongo.TokenIdentifierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TokenIdentifierService {

    private final TokenIdentifierRepository tokenIdentifierRepository;

    @Transactional(readOnly = true)
    public boolean tokenIdentifierExist(final String tokenIdentifier) {
        return tokenIdentifierRepository.findByIdentifier(tokenIdentifier).isPresent();
    }

    @Transactional
    public TokenIdentifier save(final String tokenIdentifier, final ZonedDateTime expirationDate) {

        final TokenIdentifier tokenIdentifierToSave = TokenIdentifier.builder()
                .identifier(tokenIdentifier)
                .expirationDate(expirationDate)
                .build();

        final TokenIdentifier savedTokenIdentifier = tokenIdentifierRepository.save(tokenIdentifierToSave);
        log.debug("TokenIdentifier saved : {}", savedTokenIdentifier);

        return savedTokenIdentifier;
    }

}
