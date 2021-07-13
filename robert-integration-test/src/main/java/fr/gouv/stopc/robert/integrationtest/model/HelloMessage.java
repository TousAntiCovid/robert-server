package fr.gouv.stopc.robert.integrationtest.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class HelloMessage {

    private Integer timeCollectedOnDevice;
    private Integer timeFromHelloMessage;
    private byte[] mac;
    private Integer rssiCalibrated;

}