package fr.gouv.stopc.robert.crypto.grpc.server.test.matchers;

import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM;
import lombok.SneakyThrows;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class EncryptedMatcher extends TypeSafeDiagnosingMatcher<byte[]> {

    public static EncryptedMatcher isEncrypted(Matcher<String> nextMatcher, byte[] keyForTuples) {
        return new EncryptedMatcher(nextMatcher, keyForTuples);
    }

    private final Matcher<String> nextMatcher;

    private final CryptoAESGCM aesGcm;

    private EncryptedMatcher(final Matcher<String> nextMatcher, byte[] keyForTuples) {
        this.nextMatcher = nextMatcher;
        this.aesGcm = new CryptoAESGCM(keyForTuples);
    }

    @SneakyThrows
    @Override
    protected boolean matchesSafely(final byte[] item, final Description mismatchDescription) {
        final var decryptedContent = aesGcm.decrypt(item);
        final var decryptedString = new String(decryptedContent);
        if (!nextMatcher.matches(decryptedString)) {
            mismatchDescription.appendText("an encrypted value ");
            nextMatcher.describeMismatch(decryptedString, mismatchDescription);
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("an encrypted value ");
        nextMatcher.describeTo(description);
    }
}
