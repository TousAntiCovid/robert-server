package e2e.crypto;

import javax.crypto.Cipher;

import java.security.spec.AlgorithmParameterSpec;

public interface ICipherStructure {

    Cipher getCipher();

    Cipher getDecryptCypher();

    AlgorithmParameterSpec getAlgorithmParameterSpec();
}
