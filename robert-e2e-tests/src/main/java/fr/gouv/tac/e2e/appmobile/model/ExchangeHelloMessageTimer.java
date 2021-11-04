package fr.gouv.tac.e2e.appmobile.model;

import lombok.Getter;

import java.util.Timer;

public class ExchangeHelloMessageTimer extends Timer {

    @Getter
    private final String functionalContext;

    public ExchangeHelloMessageTimer(String functionalContext) {
        this.functionalContext = functionalContext;
    }

}
