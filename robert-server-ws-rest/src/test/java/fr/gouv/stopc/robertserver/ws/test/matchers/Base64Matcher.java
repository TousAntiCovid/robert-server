package fr.gouv.stopc.robertserver.ws.test.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Base64;

/**
 * Creates a matcher that matches if the examined String is Base64 encoded and
 * matches the specified matcher when it is Base64 decoded. For example:
 * assertThat("Um9iZXJ0IFNlcnZlcg==", isBase64Encoded(equalTo("Robert Server"))
 */
public class Base64Matcher extends TypeSafeDiagnosingMatcher<String> {

    public static Base64Matcher isBase64Encoded(Matcher<String> contentMatcher) {
        return new Base64Matcher(contentMatcher);
    }

    public static String toBase64(byte[] content) {
        return Base64.getEncoder().encodeToString(content);
    }

    public static String toBase64(String content) {
        return toBase64(content.getBytes());
    }

    public static String toBase64(String content, int truncateBytes) {
        final var truncatedContent = new String(content.getBytes(), 0, truncateBytes);
        return toBase64(truncatedContent);
    }

    private final Matcher<String> contentMatcher;

    private Base64Matcher(Matcher<String> contentMatcher) {
        this.contentMatcher = contentMatcher;
    }

    @Override
    protected boolean matchesSafely(String value, Description mismatchDescription) {
        final var content = new String(Base64.getDecoder().decode(value));
        mismatchDescription.appendText("a Base64 encoded string that contains ");
        contentMatcher.describeMismatch(content, mismatchDescription);
        return contentMatcher.matches(content);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a Base64 encoded String that contains ");
        contentMatcher.describeTo(description);
    }
}
