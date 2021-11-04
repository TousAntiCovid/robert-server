package e2e.appmobile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import e2e.appmobile.tools.ClientIdentifierBundle;
import e2e.appmobile.tools.Contact;
import e2e.external.crypto.CryptoAESGCM;
import e2e.external.crypto.exception.RobertServerCryptoException;
import e2e.external.crypto.model.EphemeralTupleJson;
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

import static e2e.appmobile.tools.EcdhUtils.deriveKeysFromBackendPublicKey;
import static e2e.appmobile.tools.EcdhUtils.generateKeyPair;

@Slf4j
public class AppMobile {

    @Getter
    private final String userName;

    @Getter
    @Setter
    private String captchaId;

    @Getter
    @Setter
    private String captchaSolution;

    @Setter
    private String robertPublicKey;

    private long timestart;

    private KeyPair keyPair;

    private List<Contact> contacts;

    private List<EphemeralTupleJson> decodedTuples;

    private ClientIdentifierBundle clientIdentifierBundleWithPublicKey;

    public AppMobile(String userName) {
        this.userName = userName;
    }

    public void generateApplicationMobileEngineData()
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        this.keyPair = generateKeyPair();
        this.contacts = new ArrayList<>();
        generateKeyForTuples();
    }

    private void generateKeyForTuples() throws RobertServerCryptoException {
        deriveKeysFromBackendPublicKey(
                Base64.getDecoder().decode(this.robertPublicKey), keyPair
        )
                .ifPresent(clientIdentifierBundle -> this.clientIdentifierBundleWithPublicKey = clientIdentifierBundle);
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
        CryptoAESGCM aesGcm = new CryptoAESGCM(clientIdentifierBundleWithPublicKey.getKeyForTuples());
        try {
            this.decodedTuples = new ObjectMapper().readValue(
                    aesGcm.decrypt(encryptedTuples),
                    new TypeReference<>() {
                    }
            );
        } catch (IOException | RobertServerCryptoException e) {
            throw new RobertServerCryptoException(e);
        }
    }

}
