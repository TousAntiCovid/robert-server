package fr.gouv.tacw.ws.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TacWarningUnauthorizedException extends RuntimeException {}
