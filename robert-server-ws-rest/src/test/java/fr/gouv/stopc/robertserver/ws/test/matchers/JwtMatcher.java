package fr.gouv.stopc.robertserver.ws.test.matchers;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

public class JwtMatcher {

    public static JwtSignatureMatcher isJwtSignedBy(final KeyPair keys) {
        return new JwtSignatureMatcher(keys);
    }

    @RequiredArgsConstructor
    public static class JwtSignatureMatcher extends TypeSafeDiagnosingMatcher<String> {

        private final Map<String, Matcher<?>> claimMatchers = new HashMap<>();

        private final KeyPair keys;

        public JwtSignatureMatcher withClaim(String name, Matcher<?> valueMatcher) {
            claimMatchers.put(name, valueMatcher);
            return this;
        }

        @Override
        protected boolean matchesSafely(final String jwtToken, final Description mismatchDescription) {
            try {
                final var claims = Jwts.parserBuilder()
                        .setSigningKey(keys.getPrivate())
                        .build()
                        .parseClaimsJws(jwtToken)
                        .getBody();
                return claimMatchers.entrySet().stream()
                        .filter(e -> !e.getValue().matches(claims.get(e.getKey())))
                        .peek(e -> e.getValue().describeMismatch(claims.get(e.getKey()), mismatchDescription))
                        .count() == 0;
            } catch (Exception e) {
                mismatchDescription.appendText("signature of jwt ").appendValue(jwtToken).appendText(" is invalid");
                return false;
            }
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("a jwt token with a valid signature");
            claimMatchers.forEach((name, matcher) -> {
                description.appendText(" and a claim ").appendValue(name).appendText(" with ");
                matcher.describeTo(description);
            });
        }
    }
}
