package fr.gouv.stopc.robert.crypto.grpc.server.test.matchers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class JsonNodeMatcher extends TypeSafeDiagnosingMatcher<String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static JsonNodeMatcher isJson(Matcher nextMatcher) {
        return new JsonNodeMatcher(nextMatcher);
    }

    private final Matcher nextMatcher;

    private JsonNodeMatcher(Matcher nextMatcher) {
        this.nextMatcher = nextMatcher;
    }

    @SneakyThrows
    @Override
    protected boolean matchesSafely(final String item, final Description mismatchDescription) {

        final JsonNode tupleCollection = OBJECT_MAPPER.readTree(
                item
        );

        if (!nextMatcher.matches(tupleCollection)) {
            mismatchDescription.appendText("a json ");
            nextMatcher.describeMismatch(tupleCollection, mismatchDescription);
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("a json string ");
        nextMatcher.describeTo(description);
    }
}
