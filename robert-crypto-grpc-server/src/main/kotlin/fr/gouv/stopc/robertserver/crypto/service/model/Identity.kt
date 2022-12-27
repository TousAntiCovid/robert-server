package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.model.IdA
import javax.crypto.SecretKey

data class Identity(
    val idA: IdA,
    val keyForMac: SecretKey,
    val keyForTuples: SecretKey,
)
