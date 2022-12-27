package fr.gouv.stopc.robertserver.crypto.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class IdentityRepository(private val jdbcTemplate: JdbcTemplate) {

    private val insert = SimpleJdbcInsert(jdbcTemplate)
        .withTableName("identity")
        .usingColumns("ida", "key_for_mac", "key_for_tuples", "creation_time", "last_update")

    fun save(base64IdA: String, encryptedKeyForMac: String, encryptedKeyForTuples: String) {
        val now = Instant.now()
        insert.execute(
            mapOf(
                "ida" to base64IdA,
                "key_for_mac" to encryptedKeyForMac,
                "key_for_tuples" to encryptedKeyForTuples,
                "creation_time" to Timestamp.from(now),
                "last_update" to Timestamp.from(now)
            )
        )
    }

    fun deleteByIdA(base64IdA: String) {
        jdbcTemplate.update("delete from identity where ida = ?", base64IdA)
    }
}
