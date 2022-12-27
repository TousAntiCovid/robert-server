package fr.gouv.stopc.robertserver.test.matchers

import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher
import java.security.KeyPair
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

fun isJwtSignedBy(keys: KeyPair): JwtMatcher {
    val signatureVerifier = when (val publicKey = keys.public) {
        is RSAPublicKey -> RSASSAVerifier(publicKey)
        is ECPublicKey -> ECDSAVerifier(publicKey)
        else -> throw IllegalArgumentException("Don't know how to verify JWT signature for key ${keys.public}")
    }
    return JwtMatcher(signatureVerifier)
}

class JwtMatcher(
    private val signatureVerifier: JWSVerifier
) : TypeSafeDiagnosingMatcher<String>() {

    private val claimMatchers = mutableMapOf<String, Matcher<*>>()

    fun <T> withClaim(name: String, valueMatcher: Matcher<T>): JwtMatcher {
        claimMatchers[name] = valueMatcher
        return this
    }

    override fun describeTo(description: Description) {
        description.appendText("a jwt token with a valid signature")
        claimMatchers.forEach { (name: String, matcher: Matcher<*>) ->
            description.appendText(" and a claim ").appendValue(name).appendText(" with ")
            matcher.describeTo(description)
        }
    }

    override fun matchesSafely(jwtString: String, mismatchDescription: Description): Boolean {
        val parsedJwt = SignedJWT.parse(jwtString)
        if (!parsedJwt.verify(signatureVerifier)) {
            mismatchDescription.appendText("signature of jwt ")
                .appendValue(jwtString)
                .appendText(" is invalid")
            return false
        }
        val actualClaims = parsedJwt.jwtClaimsSet
        val mismatches = claimMatchers
            .filter { (name, expectedValue) -> !expectedValue.matches(actualClaims.getClaim(name)) }
        mismatches.forEach { (name, matcher) ->
            matcher.describeMismatch(actualClaims.getClaim(name), mismatchDescription)
        }
        return mismatches.isEmpty()
    }
}
