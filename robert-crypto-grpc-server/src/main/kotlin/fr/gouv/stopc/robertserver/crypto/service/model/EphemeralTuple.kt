package fr.gouv.stopc.robertserver.crypto.service.model

import kotlinx.serialization.Serializable

/**
 * An ephemeral tuple is a pair `(ebid, ecc)` for a given `epochDd`.
 */
@Serializable
data class EphemeralTuple(
    val epochId: Int,
    val key: TupleKey,
) {

    constructor(epochId: Int, ebid: Ebid, ecc: Ecc) : this(epochId, TupleKey(ebid, ecc))

    /**
     * A pair `(ebid, ecc)`.
     */
    @Serializable
    data class TupleKey(
        val ebid: Ebid,
        val ecc: Ecc,
    )
}
