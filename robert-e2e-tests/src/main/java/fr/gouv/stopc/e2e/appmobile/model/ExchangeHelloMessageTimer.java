package fr.gouv.stopc.e2e.appmobile.model;

import lombok.Value;

import java.util.Timer;

@Value
public class ExchangeHelloMessageTimer extends Timer {

    String functionalContext;

}
