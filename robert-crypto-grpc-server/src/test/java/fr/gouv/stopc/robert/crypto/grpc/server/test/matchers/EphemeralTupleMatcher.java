package fr.gouv.stopc.robert.crypto.grpc.server.test.matchers;

import lombok.Value;

public class EphemeralTupleMatcher {

    @Value
    public static class EphemeralTuple {

        int epochId;

        String ebid;

        String ecc;

    }

}
