package fr.gouv.tac.mobile.emulator.utils;

import java.util.Timer;

import lombok.Getter;

public class ExchangeHelloMessageTimer extends Timer {

    @Getter
    private final String functionalContext;

    public ExchangeHelloMessageTimer(String functionalContext) {
        this.functionalContext = functionalContext;

    }

}
