package fr.gouv.stopc.robertserver.ws.test

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.base64Encode
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Helps to generate @[org.junit.jupiter.params.ParameterizedTest]s to
 * verify soft authentications features because, for some endpoints, users may
 * send desynchronized authentication details.
 */
class AuthDataManager : TestExecutionListener {

    override fun beforeTestClass(testContext: TestContext) {
        clock = testContext.applicationContext.getBean(RobertClock::class.java)
    }

    companion object {

        private lateinit var clock: RobertClock

        private val pushInfosOptions = listOf(
            null,
            """{"token": "valid-token", "locale": "fr-FR", "timezone": "Europe/Paris"}"""
        )

        /**
         * Generates out of range authentication details: time can't be more
         * desynchronized by more than 1 minute.
         */
        @JvmStatic
        fun unacceptableAuthParameters(): List<AuthRequestData> {
            return listOf(
                AuthRequestData("time is 5m behind", 0, (-5).minutes),
                AuthRequestData("time is 2m behind", 0, (-2).minutes),
                AuthRequestData("time is 1m 5s behind", 0, (-1).minutes - 5.seconds),
                AuthRequestData("time is 1m 5s ahead", 0, 1.minutes + 5.seconds),
                AuthRequestData("time is 1m ahead", 0, 2.minutes),
                AuthRequestData("time is 5m ahead", 0, 5.minutes)
            )
        }

        /**
         * Generates out of range but acceptables authentication details: time is
         * desynchronized by less than 1 minute.
         */
        @JvmStatic
        fun acceptableAuthParameters(): List<AuthRequestData> {
            val acceptableEpochDrift = listOf(
                "epoch is synchronized" to 0,
                "50 epoch behind" to -50,
                "2 epoch behind" to -2,
                "1 epoch behind" to -1,
                "1 epoch ahead" to 1,
                "2 epoch ahead" to 2,
                "50 epoch ahead" to 50
            )
            val acceptableTimeDrift = listOf(
                "time is synchronized" to Duration.ZERO,
                "time is 55s behind (real limit is 60s, but test execution may produce latency)" to (-55).seconds,
                "time is 30s behind" to (-30).seconds,
                "time is 30s ahead" to 30.seconds,
                "time is 60s ahead" to 60.seconds
            )
            return acceptableEpochDrift.flatMap { epochDrift ->
                acceptableTimeDrift.map { timeDrift ->
                    AuthRequestData(
                        "${epochDrift.first} and ${timeDrift.first}",
                        epochDrift.second,
                        timeDrift.second
                    )
                }
            }
        }

        /**
         * Combines given parameters without of range but acceptables authentication
         * details: time is desynchronized by less than 1 minute.
         */
        fun acceptableAuthParametersForEach(additionalArgumentCombinations: List<Any?>): List<Arguments> {
            return additionalArgumentCombinations.flatMap { arg ->
                acceptableAuthParameters().map { authParam ->
                    arguments(arg, authParam)
                }
            }
        }
    }

    data class AuthRequestData(
        val description: String,
        val epochDrift: Int = 0,
        val timeDrift: Duration,
        val pushInfo: String? = pushInfosOptions.random()
    ) {

        fun epochId() = clock.now().asEpochId() + epochDrift

        fun base64Time32() = clock.now().plus(timeDrift.toJavaDuration())
            .asTime32()
            .base64Encode()

        fun base64Mac() = "fixed 32 bytes value used as mac".base64Encode()

        override fun toString(): String {
            return description + if (null == pushInfo) " and no push infos" else " with push infos"
        }
    }
}
