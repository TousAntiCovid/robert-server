package fr.gouv.stopc.robertserver.ws.test;

import io.restassured.response.ValidatableResponse;
import org.assertj.core.api.AbstractIntegerAssert;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class StatisticsManager implements TestExecutionListener {

    private static ValidatableResponse todaySnapshot;

    @Override
    public void beforeTestMethod(@NotNull TestContext testContext) {
        todaySnapshot = getTodayStatistics();
    }

    public static AbstractIntegerAssert<?> assertThatTodayStatistic(String statisticName) {
        final int originalValue = todaySnapshot.extract().path(statisticName);
        final int currentValue = getTodayStatistics().extract().path(statisticName);
        return assertThat(currentValue - originalValue)
                .as("'%s' statistic value increment", statisticName);
    }

    private static ValidatableResponse getTodayStatistics() {
        return given()
                .get("/internal/api/v2/kpis")
                .then();
    }
}
