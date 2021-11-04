package fr.gouv.tac.e2e.appmobile.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HelloMessageDetail {

    Long timeCollectedOnDevice;

    Integer timeFromHelloMessage;

    byte[] mac;

    Integer rssiCalibrated;

}
