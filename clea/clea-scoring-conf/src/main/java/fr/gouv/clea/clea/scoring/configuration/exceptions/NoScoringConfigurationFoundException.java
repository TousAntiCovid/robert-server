package fr.gouv.clea.clea.scoring.configuration.exceptions;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class NoScoringConfigurationFoundException extends RuntimeException {

    public NoScoringConfigurationFoundException(String message) {
        super(message);
    }

}
