package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.common.model.IdA
import fr.gouv.stopc.robertserver.crypto.cipher.encryptUsingAesGcm
import fr.gouv.stopc.robertserver.crypto.grpc.model.EncryptedIdentity
import java.security.Key
import javax.crypto.SecretKey

data class Identity(
    val idA: IdA,
    val keyForMac: SecretKey,
    val keyForTuples: SecretKey
) {
    fun encrypt(kek: Key) = EncryptedIdentity(
        idA.toBase64String(),
        keyForMac.encoded
            .encryptUsingAesGcm(kek)
            .base64Encode(),
        keyForTuples.encoded
            .encryptUsingAesGcm(kek)
            .base64Encode()
    )
}
