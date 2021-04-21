package fr.gouv.clea.scoring.configuration.exceptions;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class NoScoringConfigurationFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoScoringConfigurationFoundException(String message) {
        super(message);
    }

}
