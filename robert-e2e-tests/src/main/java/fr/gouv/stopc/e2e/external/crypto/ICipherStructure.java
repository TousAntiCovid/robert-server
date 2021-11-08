package fr.gouv.stopc.e2e.external.crypto;

import javax.crypto.Cipher;

import java.security.spec.AlgorithmParameterSpec;

public interface ICipherStructure {

    Cipher getCipher();

    Cipher getDecryptCypher();

    AlgorithmParameterSpec getAlgorithmParameterSpec();
}
