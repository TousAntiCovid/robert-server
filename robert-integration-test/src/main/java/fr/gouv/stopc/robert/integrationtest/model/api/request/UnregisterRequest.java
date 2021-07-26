package fr.gouv.stopc.robert.integrationtest.model.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class UnregisterRequest {
    private byte[] ebid;;
    private Integer epochId;;
    private byte[] time;
    private byte[] mac;
    private String pushToken;
}
