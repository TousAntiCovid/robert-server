package e2e.model;

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
