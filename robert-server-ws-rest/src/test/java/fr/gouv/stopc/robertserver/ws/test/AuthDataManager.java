package fr.gouv.stopc.robertserver.ws.test;

import fr.gouv.stopc.robertserver.common.RobertClock;
import lombok.Value;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Helps generating @{@link org.junit.jupiter.params.ParameterizedTest}s to
 * verify soft authentications features because, for some endpoints, users may
 * send desynchronized authentication details.
 */
public class AuthDataManager implements TestExecutionListener {

    private static RobertClock clock;

    @Override
    public void beforeTestClass(TestContext testContext) {
        clock = testContext.getApplicationContext().getBean(RobertClock.class);
    }

    /**
     * Generates out of range authentication details: time can't be more
     * desynchronized by more than 1 minute.
     */
    public static Stream<AuthRequestData> unacceptableAuthParameters() {
        return Stream.of(
                new AuthRequestData("time is 5m behind", 0, Duration.ofMinutes(-5)),
                new AuthRequestData("time is 2m behind", 0, Duration.ofMinutes(-2)),
                new AuthRequestData("time is 1m 5s behind", 0, Duration.ofMinutes(-1).minusSeconds(5)),
                new AuthRequestData("time is 1m 5s ahead", 0, Duration.ofMinutes(1).plusSeconds(5)),
                new AuthRequestData("time is 1m ahead", 0, Duration.ofMinutes(2)),
                new AuthRequestData("time is 5m ahead", 0, Duration.ofMinutes(5))
        );
    }

    /**
     * Generates out of range but acceptables authentication details: time is
     * desynchronized by less than 1 minute.
     */
    public static Stream<AuthRequestData> acceptableAuthParameters() {
        final var acceptableEpochDrift = Stream.of(
                Pair.of("epoch is synchronized", 0),
                Pair.of("50 epoch behind", -50),
                Pair.of("2 epoch behind", -2),
                Pair.of("1 epoch behind", -1),
                Pair.of("1 epoch ahead", 1),
                Pair.of("2 epoch ahead", 2),
                Pair.of("50 epoch ahead", 50)
        );
        final var acceptableTimeDrift = List.of(
                Pair.of("time is synchronized", Duration.ZERO),
                Pair.of(
                        "time is 55s behind (real limit is 60s, but test execution may produce latency)",
                        Duration.ofSeconds(-55)
                ),
                Pair.of("time is 30s behind", Duration.ofSeconds(-30)),
                Pair.of("time is 30s ahead", Duration.ofSeconds(30)),
                Pair.of("time is 60s ahead", Duration.ofSeconds(60))
        );
        return acceptableEpochDrift.flatMap(
                epochDrift -> acceptableTimeDrift.stream().map(
                        timeDrift -> new AuthRequestData(
                                epochDrift.getFirst() + " and " + timeDrift.getFirst(),
                                epochDrift.getSecond(), timeDrift.getSecond()
                        )
                )
        );
    }

    /**
     * Combines given parameters with out of range but acceptables authentication
     * details: time is desynchronized by less than 1 minute.
     */
    public static Stream<Arguments> acceptableAuthParametersForEach(Stream<Object> additionalArgumentCombinations) {
        return additionalArgumentCombinations.flatMap(
                arg -> acceptableAuthParameters().map(authParam -> arguments(arg, authParam))
        );
    }

    @Value
    public static class AuthRequestData {

        String description;

        int epochDrift;

        Duration timeDrift;

        public int epochId() {
            return clock.now().asEpochId() + epochDrift;
        }

        public String base64Time32() {
            return toBase64(clock.now().plus(timeDrift).asTime32());
        }

        public String base64Mac() {
            return toBase64("fake mac having a length of exactly 44 characters", 32);
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
