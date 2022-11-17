package fr.gouv.stopc.robertserver.ws.test.matchers

import fr.gouv.stopc.robertserver.test.LogbackManager.Companion.assertThatInfoLogs
import fr.gouv.stopc.robertserver.test.LogbackManager.Companion.assertThatWarnLogs
import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.assertThatRegistrationForIdA
import org.assertj.core.api.HamcrestCondition.matching
import org.assertj.core.condition.VerboseCondition
import org.assertj.core.condition.VerboseCondition.verboseCondition
import org.hamcrest.Matchers.matchesPattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies logs about exceeded time drift are produced and the delta is stored
 * in the user registration.
 */
fun verifyExceededTimeDriftIsProperlyHandled(user: String, timeDrift: Duration) {
    assertThatWarnLogs().areExactly(
        1,
        matching(
            matchesPattern(
                "Witnessing abnormal time difference -?\\d+ between client: [0-9-T:.Z=E]+ and server: [0-9-T:.Z=E]+"
            )
        )
    )
    assertThatInfoLogs()
        .contains("Discarding authenticated request because provided time is too far from current server time")

    assertThatRegistrationForIdA(user)
        .hasEntrySatisfying("lastTimestampDrift", timeDriftCloseTo(timeDrift))
}

fun timeDriftCloseTo(timeDrift: Duration): VerboseCondition<Any> = verboseCondition(
    { value -> timeDrift.absoluteValue - (value as Long).seconds.absoluteValue <= 2.seconds },
    "a number with absolute value close to $timeDrift +/- 2s",
    { value -> " was $value" }
)
