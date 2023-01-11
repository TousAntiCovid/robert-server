package fr.gouv.stopc.robertserver.crypto.repository.model

import fr.gouv.stopc.robertserver.common.base64Decode
import fr.gouv.stopc.robertserver.common.model.IdA
import fr.gouv.stopc.robertserver.crypto.cipher.decryptUsingAesGcm
import fr.gouv.stopc.robertserver.crypto.service.GENERATED_KEY_ALGORITHM_AES
import fr.gouv.stopc.robertserver.crypto.service.model.Identity
import java.security.Key
import javax.crypto.spec.SecretKeySpec

data class EncryptedIdentity(
    val base64IdA: String,
    val encryptedKeyForMac: String,
    val encryptedKeyForTuples: String
) {
    fun decrypt(kek: Key) = Identity(
        idA = IdA(base64IdA),
        keyForMac = SecretKeySpec(
            encryptedKeyForMac.base64Decode().decryptUsingAesGcm(kek),
            GENERATED_KEY_ALGORITHM_AES
        ),
        keyForTuples = SecretKeySpec(
            encryptedKeyForTuples.base64Decode().decryptUsingAesGcm(kek),
            GENERATED_KEY_ALGORITHM_AES
        )
    )
}
