package fr.gouv.stopc.robert.crypto.grpc.server.exception;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NoServerKeyFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public NoServerKeyFoundException(String message) {
        super(message);
    }

}
