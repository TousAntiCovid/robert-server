package fr.gouv.stopc.robert.crypto.grpc.server.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.robert.crypto.grpc.server.test.matchers.EphemeralTupleMatcher;
import lombok.SneakyThrows;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Collection;

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

        final Collection<EphemeralTupleMatcher.EphemeralTuple> tupleCollection = OBJECT_MAPPER.readValue(
                item,
                new TypeReference<>() {
                }
        );

        if (!nextMatcher.matches(tupleCollection)) {
            mismatchDescription.appendText("a json which ");
            nextMatcher.describeMismatch(tupleCollection, mismatchDescription);
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("a json with ");
        nextMatcher.describeTo(description);
    }
}
