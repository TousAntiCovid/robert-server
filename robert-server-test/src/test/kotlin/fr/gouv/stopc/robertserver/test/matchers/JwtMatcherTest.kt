package fr.gouv.stopc.robertserver.test.matchers

import fr.gouv.stopc.robertserver.common.base64Decode
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.security.KeyFactory
import java.security.KeyPair
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

/**
 * Keys have been generated using the following commands:
 *
 * ```
 * # rsa_private.pem
 * openssl genpkey -algorithm RSA -out rsa_private.pem -pkeyopt rsa_keygen_bits:2048
 * # rsa_public.pem
 * openssl rsa -in rsa_private.pem -pubout -out rsa_public.pem
 * # remove PEM headers and newlines
 * PRIVATE_KEY=$(cat rsa_private.pem | grep -v '^\---' | tr -d '\n')
 * PUBLIC_KEY=$(cat rsa_public.pem | grep -v '^\---' | tr -d '\n')
 * ```
 */
class JwtMatcherTest() {

    private val EXAMPLE_JWT =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0ZXN0In0.WLlQlAVjzAGrkwUU8cIgGvEm8vfFH--cYj8c2exYcEaiz7WXM_C1cHc1oCe8fPV3GkpdLsuenOBaPPEKtBuZLLFW8INF61d-Kki9Vu-dwMMjb_FyG-dwzo3qOM7_87zGIZRgG0tokFJjDi1yt6_jS2PG82nmC8KQ0Yixq1hi6ylTuWhbm28avT8YvD6VHfq_kTe0doTUxNEKw7mbMnXsu-VOjhxA8y8xAKgTlCDi7tuZnS54nJGgveFxS0DywTRg7zwT9iErodW9I57zOhYpRcgETwDfgzYNx9UeMR9A6cLj9BHDsXxq66t35634LLzEVlyHI6KcoaxadqCtBVDnsQ"
    private val PRIVATE_KEY =
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDOQo42UQ4y6j9ecY3cyGYjBgptyeJIHXdh1iFttMrvCYULtrAUjiXc0vgrIj+SBP1qvqp1UV9bhOmT6PXi79D7AtpiheEmNYcFNw7HnrIpZvnGWY/a84Poak7SH3v/igxVJwhmsnnzdDdlrWZKtWNh9h9G1dM/SVKhsgP10HeJPW3romgyzPnqpal29dTFcFUx+4O5D1AAH5JK9ExS772Hut/ZEJrswejAMd+m4OqKPIWyplF/GNivbpnoFEpucE2Bf1jTvM25S3kQROXS33KILm4d0jMa+610Ht9HCp/Hgqu8d8NFse3cr+pl+dXeFh00djsRdhNDg9kVRL7Jb/h/AgMBAAECggEBALzvOlWG6fq5mFJqsggLiYOLhQIMa7qjQzfbbcOQWFOd5MFWFvS9QPGGTs00EgcvivK8tGfCohazKULTomi+RC+p7WFSQWs+nO+p/1/bfldufF20rJFjfvDmcE0JigwJ6EIX/xoTYqIfUp9QtuK4RND3Iq9ALsFJ6Oc0qWwCCX0roeE4LFsthdfC+XSMU51PkwscMmVrgoLOsZTLqODxMTf0Dd7T8oTmhjdCU/S7lPH7foYuOflovldlP8lMgc/Nczk3fscTafUaqA+SMeku4eGuq3l5kkBC8T3UY2p5/goQBNNSXPADVao8qByJFRTW4mWs40Z4lUmvmbRS0y35YAECgYEA931FThkiBAlylZx4Cx3SuP1WDUandD3vc7U0s/CXoWomOarqfKu53t+Vg8d+s6Cemvb5YPgwF/DE3maS1nJ/o04QAjSCdsBqkkODAUSMBoryR3G5S65KQTdwz9oGOzqSGkOD7Hg/fkFmLwxbxXTG+GZhSBnlZCB8apTE7H0VSSECgYEA1VpUT4Kh0DGsdDsUraOlC7dyb+PQN4kS92fo0PYn/45pOtd9GyVcDQaCqA5Cg66Yhkp+0LL5b6/GBHNC6CC4KEMVCR5U4Hzz4bUJqiupWESrcl+FegJvtewk6X6hsgr4JYSW1xC4jW7/JWiw9P3dGaFB8WIolTMcS7ASE0AE7Z8CgYBzczCdWgQQNggJ2s/0/5iJS0SVNNxw0WOeam2HcziIx+fFBwIT3lf6q/onWyyzxr22NHfo0F4/DMYJqXfeKdN47rVxEeW6V9BfIrc+JCfd9XtH8LkroMZq3d32WBKIFq6JKe2kpgOYdhGg6Pb2iZ83ySqgY9QqfS0M2xFx8He5gQKBgBPz46if7eQbkSY5lEB86mWDHXerVYCsGBL9K7/HFYyH0/2Fw5RzVP1+218+HihUfr0oYF3CLyOry5gE83/j93UEeQYmSQfJh8FW/fVlKewwV9xVZJU5fxsDX5xalGKjNfvEP5sTXD8V31SvSsVOrIvGEfWyGVuCsSW+7KHSPeO/AoGBAM+CiBuNvGGWtC1KqmbFPqVT/J2F5ZI3qSc5EwLbMJFqhD3kXxhDG/A2H0JmJnhMALsZZlA23CQkkStKj317pYK5dK+IAqJFdSA1NSaN7aw0WV9QjoFU6QDzonXZ9yBj7QpQsysKxYgqc/C+AHdD31QbFwdhypM5CRvIsDf9g9Ri"
    private val PUBLIC_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzkKONlEOMuo/XnGN3MhmIwYKbcniSB13YdYhbbTK7wmFC7awFI4l3NL4KyI/kgT9ar6qdVFfW4Tpk+j14u/Q+wLaYoXhJjWHBTcOx56yKWb5xlmP2vOD6GpO0h97/4oMVScIZrJ583Q3Za1mSrVjYfYfRtXTP0lSobID9dB3iT1t66JoMsz56qWpdvXUxXBVMfuDuQ9QAB+SSvRMUu+9h7rf2RCa7MHowDHfpuDqijyFsqZRfxjYr26Z6BRKbnBNgX9Y07zNuUt5EETl0t9yiC5uHdIzGvutdB7fRwqfx4KrvHfDRbHt3K/qZfnV3hYdNHY7EXYTQ4PZFUS+yW/4fwIDAQAB"
    private val KEYS: KeyPair

    init {
        try {
            val publicKey = X509EncodedKeySpec(PUBLIC_KEY.base64Decode())
            val privateKey = PKCS8EncodedKeySpec(PRIVATE_KEY.base64Decode())
            val keyFactory = KeyFactory.getInstance("RSA")
            KEYS = KeyPair(
                keyFactory.generatePublic(publicKey),
                keyFactory.generatePrivate(privateKey)
            )
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            throw RuntimeException(e)
        }
    }

    @Test
    fun can_verify_jwt_signature() {
        assertThat(EXAMPLE_JWT, isJwtSignedBy(KEYS))
    }

    @Test
    fun can_detect_wrong_jwt_signature() {
        val jwtWithInvalidSignature = EXAMPLE_JWT.replaceAfterLast('.', "invalid-signature")
        val error = assertThrows(AssertionError::class.java) {
            assertThat(
                jwtWithInvalidSignature,
                isJwtSignedBy(KEYS)
            )
        }
        assertThat(
            error.message?.replace("\r", ""),
            equalTo(
                """
                    
                    Expected: a jwt token with a valid signature
                         but: signature of jwt "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0ZXN0In0.invalid-signature" is invalid
                """.trimIndent()
            )
        )
    }

    @Test
    fun can_verify_jwt_claim() {
        assertThat(
            EXAMPLE_JWT,
            isJwtSignedBy(KEYS)
                .withClaim("iss", equalTo("test"))
        )
    }

    @Test
    fun can_detect_wrong_claim_value() {
        val error = assertThrows(AssertionError::class.java) {
            assertThat(
                EXAMPLE_JWT,
                isJwtSignedBy(KEYS)
                    .withClaim("iss", equalTo("other value"))
            )
        }
        assertThat(
            error.message?.replace("\r", ""),
            equalTo(
                """
                    
                    Expected: a jwt token with a valid signature and a claim "iss" with "other value"
                         but: was "test"
                """.trimIndent()
            )
        )
    }

    @Test
    fun can_detect_missing_claim() {
        val error = assertThrows(AssertionError::class.java) {
            assertThat(
                EXAMPLE_JWT,
                isJwtSignedBy(KEYS)
                    .withClaim("role", startsWith("admin"))
            )
        }
        assertThat(
            error.message?.replace("\r", ""),
            equalTo(
                """
                    
                    Expected: a jwt token with a valid signature and a claim "role" with a string starting with "admin"
                         but: was null
                """.trimIndent()
            )
        )
    }
}
