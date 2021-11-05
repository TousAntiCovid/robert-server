package fr.gouv.stopc.e2e.external.crypto.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class EphemeralTupleJson {

    private int epochId;

    private EphemeralTupleEbidEccJson key;

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class EphemeralTupleEbidEccJson {

        private byte[] ebid;

        private byte[] ecc;
    }

}
