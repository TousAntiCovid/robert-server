package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.model.IdA

/**
 * A tuples bundle is a list of [EphemeralTuple] encrypted with the _key for tuples_ of the user's application identifier.
 */
data class TuplesBundle(
    val idA: IdA,
    val encryptedTuples: String
)
