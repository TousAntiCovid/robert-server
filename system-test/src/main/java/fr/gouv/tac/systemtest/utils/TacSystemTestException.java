package fr.gouv.tac.systemtest.utils;

public class TacSystemTestException extends Exception {

    public TacSystemTestException(Exception e) {
        super(e);
    }

    public TacSystemTestException(String message) {
        super(message);
    }
}
