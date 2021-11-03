package e2e.phone;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import e2e.external.crypto.CryptoAESGCM;
import e2e.external.crypto.exception.RobertServerCryptoException;
import e2e.external.crypto.model.EphemeralTupleJson;
import e2e.phone.tools.ClientIdentifierBundle;
import e2e.phone.tools.Contact;
import e2e.robert.ws.rest.RegisterSuccessResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static e2e.phone.tools.EcdhUtils.deriveKeysFromBackendPublicKey;
import static e2e.phone.tools.EcdhUtils.generateKeyPair;

@Slf4j
public class AppMobile {

    @Getter
    private final String captchaId;

    @Getter
    @Setter
    private String captchaSolution;

    @Getter
    @Setter
    private String robertPublicKey;

    private long timestart;

    @Getter(AccessLevel.PRIVATE)
    List<Contact> contacts;

    @Getter(AccessLevel.PRIVATE)
    List<EphemeralTupleJson> decodedTuples;

    @Getter(AccessLevel.PRIVATE)
    Optional<ClientIdentifierBundle> clientIdentifierBundleWithPublicKey;

    KeyPair keyPair;

    public AppMobile(String captchaId) {
        this.captchaId = captchaId;
    }

    public void generateUsefullData()
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        this.keyPair = generateKeyPair();
        this.contacts = new ArrayList<>();
        generateKeyForTuples();
    }

    private void generateKeyForTuples() throws RobertServerCryptoException {
        this.clientIdentifierBundleWithPublicKey = deriveKeysFromBackendPublicKey(
                Base64.getDecoder().decode(this.robertPublicKey), keyPair
        );
    }

    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public void decryptRegisterResponse(@NotNull RegisterSuccessResponse registerData)
            throws RobertServerCryptoException {
        this.timestart = registerData.getTimeStart();
        updateTuples(registerData.getTuples());
    }

    private void updateTuples(byte[] encryptedTuples) throws RobertServerCryptoException {
        CryptoAESGCM aesGcm = new CryptoAESGCM(clientIdentifierBundleWithPublicKey.get().getKeyForTuples());
        try {
            byte[] decryptedTuples = aesGcm.decrypt(encryptedTuples);
            ObjectMapper objectMapper = new ObjectMapper();
            this.decodedTuples = objectMapper.readValue(
                    decryptedTuples,
                    new TypeReference<>() {
                    }
            );
        } catch (IOException | RobertServerCryptoException e) {
            throw new RobertServerCryptoException(e);
        }
    }

}
