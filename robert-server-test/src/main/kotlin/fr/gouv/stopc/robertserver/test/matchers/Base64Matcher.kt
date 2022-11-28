package fr.gouv.stopc.robertserver.test.matchers

import fr.gouv.stopc.robertserver.common.base64Decode
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher

/**
 * Creates a matcher that matches if the examined [String] is Base64 encoded and matches the specified matcher when it is Base64 decoded.
 *
 * For example:
 * ```
 * assertThat("Um9iZXJ0IFNlcnZlcg==", isBase64Encoded(equalTo("Robert Server"))
 * ```
 */
fun isBase64Encoded(contentMatcher: Matcher<String>): Base64Matcher {
    return Base64Matcher(contentMatcher)
}

class Base64Matcher internal constructor(private val contentMatcher: Matcher<String>) : TypeSafeDiagnosingMatcher<String>() {

    override fun matchesSafely(value: String, mismatchDescription: Description): Boolean {
        val content = value.base64Decode().decodeToString()
        mismatchDescription.appendText("a Base64 encoded string that contains ")
        contentMatcher.describeMismatch(content, mismatchDescription)
        return contentMatcher.matches(content)
    }

    override fun describeTo(description: Description) {
        description.appendText("a Base64 encoded String that contains ")
        contentMatcher.describeTo(description)
    }
}
