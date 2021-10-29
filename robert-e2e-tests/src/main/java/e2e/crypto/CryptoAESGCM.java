package e2e.crypto;

import e2e.crypto.exception.RobertServerCryptoException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
public class CryptoAESGCM extends CryptoAES {

    private static final String AES_ENCRYPTION_CIPHER_SCHEME = "AES/GCM/NoPadding";

    private static final int IV_LENGTH = 12;

    /**
     * @param key to be used for cipher
     */
    public CryptoAESGCM(byte[] key) {

        super(AES_ENCRYPTION_CIPHER_SCHEME, new SecretKeySpec(key, CryptoAES.AES_ENCRYPTION_KEY_SCHEME));
    }

    @Override
    public byte[] encrypt(byte[] plainText) throws RobertServerCryptoException {
        return super.encrypt(plainText);
    }

    @Override
    public byte[] decrypt(byte[] cipherText) throws RobertServerCryptoException {
        this.algorithmParameterSpec = new GCMParameterSpec(128, cipherText, 0, IV_LENGTH);
        byte[] toDecrypt = new byte[cipherText.length - IV_LENGTH];
        System.arraycopy(cipherText, IV_LENGTH, toDecrypt, 0, cipherText.length - IV_LENGTH);
        try {
            this.getDecryptCypher().init(Cipher.DECRYPT_MODE, this.getSecretKey(), this.algorithmParameterSpec);
            return super.decrypt(toDecrypt);
        } catch (Exception e) {
            throw new RobertServerCryptoException(e);
        }
    }
}
