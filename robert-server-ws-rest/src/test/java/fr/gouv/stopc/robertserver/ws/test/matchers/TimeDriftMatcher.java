package fr.gouv.stopc.robertserver.ws.test.matchers;

import java.time.Duration;

import static fr.gouv.stopc.robertserver.ws.test.LogbackManager.assertThatInfoLogs;
import static fr.gouv.stopc.robertserver.ws.test.LogbackManager.assertThatWarnLogs;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.assertThatRegistrationTimeDriftForUser;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.assertj.core.data.Offset.offset;
import static org.hamcrest.Matchers.matchesPattern;

public class TimeDriftMatcher {

    /**
     * Verifies logs about exceeded time drift are produced and the delta is stored
     * in the user registration.
     */
    public static void verifyExceededTimeDriftIsProperlyHandled(final String user, final Duration timeDrift) {
        assertThatWarnLogs().areExactly(
                1,
                matching(
                        matchesPattern(
                                "Witnessing abnormal time difference -?\\d+ between client: [0-9-T:.Z=E]+ and server: [0-9-T:.Z=E]+"
                        )
                )
        );
        assertThatInfoLogs()
                .contains("Discarding authenticated request because provided time is too far from current server time");

        assertThatRegistrationTimeDriftForUser(user)
                .isCloseTo(-timeDrift.toSeconds(), offset(2L));
    }

}
