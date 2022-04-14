package fr.gouv.stopc.robertserver.ws.test.matchers;

import org.junit.jupiter.api.Test;

import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.isBase64Encoded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Base64MatcherTest {

    // echo -n 'Robert Server' | base64
    // outputs 'Um9iZXJ0IFNlcnZlcg=='

    @Test
    void can_unwrap_base64_encoded_data() {
        assertThat("Um9iZXJ0IFNlcnZlcg==", isBase64Encoded(equalTo("Robert Server")));
        assertThat("Um9iZXJ0IFNlcnZlcg==", isBase64Encoded(not(equalTo("other server"))));
    }

    @Test
    void can_describe_mismatch() {
        final var error = assertThrows(
                AssertionError.class,
                () -> assertThat("Um9iZXJ0IFNlcnZlcg==", isBase64Encoded(equalTo("Robert zzzzzz")))
        );
        assertThat(
                error.getMessage().replace("\r", ""), equalTo(
                        "\n" +
                                "Expected: a Base64 encoded String that contains \"Robert zzzzzz\"\n" +
                                "     but: a Base64 encoded string that contains was \"Robert Server\""
                )
        );
    }

    @Test
    void can_detect_invalid_base64_input() {
        final var error = assertThrows(
                IllegalArgumentException.class, () -> assertThat("!!!", isBase64Encoded(equalTo("Robert Server")))
        );
        assertThat(error.getMessage(), equalTo("Illegal base64 character 21"));
    }

}
