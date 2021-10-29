package e2e.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import e2e.crypto.CryptoAESGCM;
import e2e.crypto.exception.RobertServerCryptoException;
import e2e.dto.RegisterSuccessResponse;
import lombok.AccessLevel;
import lombok.Getter;
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

import static e2e.crypto.EcdhUtils.deriveKeysFromBackendPublicKey;
import static e2e.crypto.EcdhUtils.generateKeyPair;

@Slf4j
public class AppMobile {

    @Getter
    private final String captchaId;

    private final String captcha;

    private final String robertPublicKey;

    private long timestart;

    @Getter(AccessLevel.PRIVATE)
    List<Contact> contacts;

    @Getter(AccessLevel.PRIVATE)
    List<EphemeralTupleJson> decodedTuples;

    @Getter(AccessLevel.PRIVATE)
    Optional<ClientIdentifierBundle> clientIdentifierBundleWithPublicKey;

    KeyPair keyPair;

    public AppMobile(String captchaId, String captcha, String robertPublicKey)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        this.captcha = captcha;
        this.captchaId = captchaId;
        this.robertPublicKey = robertPublicKey;
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
