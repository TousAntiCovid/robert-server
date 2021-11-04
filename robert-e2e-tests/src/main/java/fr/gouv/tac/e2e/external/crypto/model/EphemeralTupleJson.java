package fr.gouv.tac.e2e.external.crypto.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class EphemeralTupleJson {

    private int epochId;

    private EphemeralTupleEbidEccJson key;

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class EphemeralTupleEbidEccJson {

        private byte[] ebid;

        private byte[] ecc;
    }

}
