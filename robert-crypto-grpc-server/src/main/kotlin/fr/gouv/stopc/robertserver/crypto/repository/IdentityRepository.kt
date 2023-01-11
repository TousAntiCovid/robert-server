package fr.gouv.stopc.robertserver.crypto.repository

import fr.gouv.stopc.robertserver.crypto.repository.model.EncryptedIdentity
import org.springframework.dao.IncorrectResultSizeDataAccessException
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

    fun save(encryptedIdentity: EncryptedIdentity) {
        val now = Instant.now()
        insert.execute(
            mapOf(
                "ida" to encryptedIdentity.base64IdA,
                "key_for_mac" to encryptedIdentity.encryptedKeyForMac,
                "key_for_tuples" to encryptedIdentity.encryptedKeyForTuples,
                "creation_time" to Timestamp.from(now),
                "last_update" to Timestamp.from(now)
            )
        )
    }

    fun findByIdA(base64IdA: String): EncryptedIdentity? = try {
        jdbcTemplate.queryForObject("select ida, key_for_mac, key_for_tuples from identity where ida = ?", { rs, _ ->
            EncryptedIdentity(
                base64IdA = rs.getString("ida"),
                encryptedKeyForMac = rs.getString("key_for_mac"),
                encryptedKeyForTuples = rs.getString("key_for_tuples")
            )
        }, base64IdA)
    } catch (e: IncorrectResultSizeDataAccessException) {
        if (e.actualSize == 0) {
            null
        } else {
            throw e
        }
    }

    fun deleteByIdA(base64IdA: String) {
        jdbcTemplate.update("delete from identity where ida = ?", base64IdA)
    }
}
