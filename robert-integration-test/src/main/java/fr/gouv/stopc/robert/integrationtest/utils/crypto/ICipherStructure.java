package fr.gouv.stopc.robert.integrationtest.utils.crypto;

import javax.crypto.Cipher;
import java.security.spec.AlgorithmParameterSpec;

public interface ICipherStructure {

    Cipher getCipher();
    
    Cipher getDecryptCypher();

    AlgorithmParameterSpec getAlgorithmParameterSpec();
}
