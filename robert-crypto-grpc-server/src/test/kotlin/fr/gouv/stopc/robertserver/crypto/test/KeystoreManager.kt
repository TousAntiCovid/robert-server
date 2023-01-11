package fr.gouv.stopc.robertserver.crypto.test

import fr.gouv.stopc.robert.server.common.utils.ByteUtils
import fr.gouv.stopc.robert.server.crypto.structure.ICryptoStructure
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESECB
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.springframework.test.context.TestExecutionListener
import java.io.FileOutputStream
import java.math.BigInteger
import java.nio.file.Files
import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.BASIC_ISO_DATE
import java.time.temporal.ChronoUnit.DAYS
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_PASSWORD = "1234"
val KEYSTORE: KeyStore = KeyStore.getInstance("pkcs12").apply {
    load(null, null)
}

/**
 * Creates a keystore file to make robert-crypto application able to run.
 */
object KeystoreManager : TestExecutionListener {

    private val ROBERT_SERVER_KEYPAIR = KeyPairGenerator.getInstance("EC").generateKeyPair()
    private val ROBERT_SERVER_CERTIFICATE = generateSelfSignedCertificate(ROBERT_SERVER_KEYPAIR)
    private val KEYSTORE_PATH = Files.createTempFile("keystore", ".p12").also {
        it.toFile().deleteOnExit()
    }

    init {
        generateRegisterKey()
        generateAESKey("federation-key", 256)
        generateAESKey("key-encryption-key", 256)
        LocalDate.now().minusDays(5).datesUntil(LocalDate.now().plusDays(5))
            .map { it.format(BASIC_ISO_DATE) }
            .forEach { generateAESKey("server-key-$it", 192) }
        FileOutputStream(KEYSTORE_PATH.toString())
            .use { fos -> KEYSTORE.store(fos, KEYSTORE_PASSWORD.toCharArray()) }

        System.setProperty("robert-crypto.service-start-date", "2020-06-01")
        System.setProperty("robert-crypto.keystore-password", KEYSTORE_PASSWORD)
        System.setProperty("robert-crypto.keystore-configuration-uri", "file:$KEYSTORE_PATH")
    }

    private fun generateRegisterKey() = KEYSTORE.setKeyEntry(
        "register-key",
        ROBERT_SERVER_KEYPAIR.private,
        KEYSTORE_PASSWORD.toCharArray(),
        arrayOf(
            ROBERT_SERVER_CERTIFICATE
        )
    )

    private fun generateSelfSignedCertificate(keyPair: KeyPair): Certificate {
        // Generate keys
        val privateKey = keyPair.private
        val publicKey = keyPair.public

        // Generate certificate
        val x500Name = X500Name("CN=StopCovid")
        val pubKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.encoded)
        val startDate = Date()
        val endDate = Date
            .from(Instant.now().plus(365, DAYS))
        val certificateBuilder = X509v3CertificateBuilder(
            x500Name,
            BigInteger(10, SecureRandom()),
            startDate,
            endDate,
            x500Name,
            pubKeyInfo
        )
        val contentSigner = JcaContentSignerBuilder("SHA256withECDSA")
            .build(privateKey)
        return JcaX509CertificateConverter().setProvider(BouncyCastleProvider())
            .getCertificate(certificateBuilder.build(contentSigner))
    }

    private fun generateAESKey(alias: String, keySize: Int) {
        val rsaKeyGenerator = KeyGenerator.getInstance("AES")
        rsaKeyGenerator.init(keySize)
        KEYSTORE.setKeyEntry(alias, rsaKeyGenerator.generateKey(), KEYSTORE_PASSWORD.toCharArray(), null)
    }
}

fun cipherForStoredKey() = object : ICryptoStructure<Cipher, Mac> {
    override fun getSecretKey(): Key {
        throw UnsupportedOperationException("Not implemented!")
    }

    override fun encrypt(dataToEncrypt: ByteArray): ByteArray {
        val keyEncryptionKey = KEYSTORE.getKey("key-encryption-key", KEYSTORE_PASSWORD.toCharArray())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyEncryptionKey)
        val cipherText = cipher.doFinal(dataToEncrypt)
        return ByteUtils.addAll(cipher.iv, cipherText)
    }

    override fun decrypt(encryptedData: ByteArray): ByteArray {
        val ivLength = 12
        val keyEncryptionKey = KEYSTORE.getKey("key-encryption-key", KEYSTORE_PASSWORD.toCharArray())
        val gcmParams = GCMParameterSpec(128, encryptedData, 0, ivLength)
        val partToDecrypt = encryptedData.copyOfRange(12, encryptedData.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keyEncryptionKey, gcmParams)
        return cipher.doFinal(partToDecrypt)
    }
}

fun cipherForEbidAtEpoch(epochId: Int): CryptoSkinny64 {
    val epochDate = clock.atEpoch(epochId)
        .asInstant()
        .atZone(UTC)
        .toLocalDate()
        .format(BASIC_ISO_DATE)
    val serverKey =
        KEYSTORE.getKey(String.format("server-key-%s", epochDate), KEYSTORE_PASSWORD.toCharArray())
    return if (null != serverKey) {
        CryptoSkinny64(serverKey.encoded)
    } else {
        val rsaKeyGenerator = KeyGenerator.getInstance("AES")
        rsaKeyGenerator.init(192)
        CryptoSkinny64(rsaKeyGenerator.generateKey().encoded)
    }
}

fun cipherForEcc(): CryptoAESECB {
    return CryptoAESECB(KEYSTORE.getKey("federation-key", KEYSTORE_PASSWORD.toCharArray()))
}
