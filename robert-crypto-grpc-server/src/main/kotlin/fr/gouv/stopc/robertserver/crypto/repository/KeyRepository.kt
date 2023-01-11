package fr.gouv.stopc.robertserver.crypto.repository

import fr.gouv.stopc.robertserver.common.logger
import fr.gouv.stopc.robertserver.crypto.RobertCryptoProperties
import org.springframework.stereotype.Repository
import java.security.Key
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.streams.toList

private const val SERVER_KEY_SIZE = 24

// Because Java does not know Skinny64, specify AES instead
private const val KEYSTORE_KG_ALGONAME = "AES"

// private static final String ALIAS_SERVER_ECDH_PUBLIC_KEY = "server-ecdh-key";
private const val ALIAS_SERVER_ECDH_PRIVATE_KEY = "register-key"

// private static final String ALIAS_SERVER_KEK = "server-key-encryption-key;

// private static final String ALIAS_SERVER_KEK = "server-key-encryption-key;
private const val ALIAS_CLIENT_KEK = "key-encryption-key" // KEK

private const val ALIAS_FEDERATION_KEY = "federation-key" // K_G

private const val ALIAS_SERVER_KEY_PREFIX = "server-key-" // K_S

interface KeyRepository {
    fun reloadKeys(password: String)
    fun getCachedKeyNames(): Set<String>
    fun getServerKeyPair(): KeyPair
    fun getKeyEncryptionKey(): Key
    fun getServerKey(date: LocalDate): Key?
    fun getFederationKey(): Key
}

@Repository
class KeystoreKeyRepository(
    config: RobertCryptoProperties,
    private val keystore: KeyStore
) : KeyRepository {

    private val log = logger()

    private val serverKeyFormatter = DateTimeFormatter.ofPattern("'$ALIAS_SERVER_KEY_PREFIX'yyyyMMdd")

    private val cachedCertificates = ConcurrentHashMap<String, Certificate>()

    private val cachedKeys = ConcurrentHashMap<String, Key>()

    private val pin = config.keystorePassword

    override fun getCachedKeyNames() = cachedKeys.keys

    override fun reloadKeys(password: String) {
        val pin = password.toCharArray()

        val federationKey = keystore.getKey(ALIAS_FEDERATION_KEY, pin)
        cachedKeys.replace(ALIAS_FEDERATION_KEY, federationKey)

        val keyEncryptionKey = keystore.getKey(ALIAS_CLIENT_KEK, pin)
        cachedKeys.replace(ALIAS_CLIENT_KEK, keyEncryptionKey)

        val keysNames = keystore.aliases().toList()
            .filter { alias -> alias.startsWith("server-key-") }
        keysNames.forEach { alias ->
            log.info("loading key {}", alias)
            val key = keystore.getKey(alias, pin)
            if (null != key) {
                cachedKeys.putIfAbsent(alias, key)
            }
        }

        val dates = keysNames.map { LocalDate.parse(it, serverKeyFormatter) }
        val minDate = dates.min()
        val maxDate = dates.max()
        val missingKeys = minDate.datesUntil(maxDate).toList() - dates.toSet()
        if (missingKeys.isNotEmpty()) {
            log.warn(
                "The server key repository is missing {} keys: {}",
                missingKeys.size,
                missingKeys.joinToString(",")
            )
        }
    }

    override fun getServerKeyPair(): KeyPair {
        val publicKey = getCertificate(ALIAS_SERVER_ECDH_PRIVATE_KEY).publicKey
        val privateKey = getKey(ALIAS_SERVER_ECDH_PRIVATE_KEY) as PrivateKey
        return KeyPair(publicKey, privateKey)
    }

    override fun getKeyEncryptionKey() = getKey(ALIAS_CLIENT_KEK)!!

    override fun getServerKey(date: LocalDate) = getKey(serverKeyFormatter.format(date))

    override fun getFederationKey() = getKey(ALIAS_FEDERATION_KEY)!!

    private fun getCertificate(alias: String): Certificate = cachedCertificates.getOrPut(alias) {
        keystore.getCertificate(alias)
    }

    private fun getKey(alias: String): Key? {
        return try {
            cachedKeys.getOrPut(alias) {
                keystore.getKey(alias, pin.toCharArray())
                    .also {
                        if (null == it) {
                            throw IllegalStateException("Keystore does not contain key for alias '$alias'")
                        }
                    }
            }
        } catch (e: IllegalStateException) {
            log.error(e.message)
            null
        }
    }
}
