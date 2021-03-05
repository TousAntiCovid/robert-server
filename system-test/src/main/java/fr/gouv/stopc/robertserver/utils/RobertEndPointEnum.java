package fr.gouv.stopc.robertserver.utils;

import lombok.Getter;

public enum RobertEndPointEnum {

    REGISTER(201, "/register", "v5"),
    UNREGISTER(200, "/unregister", "v5"),
    DELETE_EXPOSURE_HISTORY(200, "/deleteExposureHistory", "v5"),
    STATUS(200,"/status", "v5"),
    REPORT(200,"/report", "v5");

    @Getter
    private String apiVersion;

    @Getter
    private int okStatusCode;

    @Getter
    private String endpoint;

    RobertEndPointEnum(int okStatusCode, String endpoint, String apiVersion) {
        this.okStatusCode = okStatusCode;
        this.endpoint = endpoint;
        this.apiVersion = apiVersion;
    }
}
