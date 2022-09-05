package fr.gouv.stopc.robert.crypto.grpc.server.test;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

public class MatchersTest {

    @Test
    void match_isEncrypted() throws RobertServerCryptoException {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        var aesGcm = new CryptoAESGCM(key);
        var encryptedData = aesGcm.encrypt("crypto".getBytes());
        // assertThat(encryptedData, isEncrypted(equalTo("crypto".getBytes()), aesGcm));
    }

    @Test
    void match_isEncoded() {
        String test = "{ \"test\" : [\"test_one\", \"test_two\", \"test_three\"] }";
        // assertThat(test.getBytes(), isJson(jsonObject()) );
    }

}
