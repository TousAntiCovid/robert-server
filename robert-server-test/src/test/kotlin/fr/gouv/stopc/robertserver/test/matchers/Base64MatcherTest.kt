package fr.gouv.stopc.robertserver.test.matchers

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Tests assumes that string _Robert Server_ is base64 encoded as `Um9iZXJ0IFNlcnZlcg==`.
 *
 * ```
 * echo -n 'Robert Server' | base64
 * outputs 'Um9iZXJ0IFNlcnZlcg=='
 * ```
 */
class Base64MatcherTest() {

    @Test
    fun can_unwrap_base64_encoded_data() {
        assertThat("Um9iZXJ0IFNlcnZlcg==", isBase64Encoded(equalTo("Robert Server")))
        assertThat("Um9iZXJ0IFNlcnZlcg==", isBase64Encoded(not(equalTo("other server"))))
    }

    @Test
    fun can_describe_mismatch() {
        val error = assertThrows(AssertionError::class.java) {
            assertThat(
                "Um9iZXJ0IFNlcnZlcg==",
                isBase64Encoded(
                    equalTo("Robert zzzzzz")
                )
            )
        }
        assertThat(
            error.message?.replace("\r", ""),
            equalTo(
                """
                    
                    Expected: a Base64 encoded String that contains "Robert zzzzzz"
                         but: a Base64 encoded string that contains was "Robert Server"
                """.trimIndent()
            )
        )
    }

    @Test
    fun can_detect_invalid_base64_input() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            assertThat(
                "!!!",
                isBase64Encoded(
                    equalTo("Robert Server")
                )
            )
        }
        assertThat(error.message, equalTo("Illegal base64 character 21"))
    }
}
