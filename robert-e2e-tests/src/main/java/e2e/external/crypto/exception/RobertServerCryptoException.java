package e2e.external.crypto.exception;

public class RobertServerCryptoException extends Exception {

    private static final long serialVersionUID = 1L;

    public RobertServerCryptoException(String message) {
        super(message);
    }

    public RobertServerCryptoException(Throwable cause) {
        super(cause);
    }

    public RobertServerCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
