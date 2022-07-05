package fr.gouv.stopc.robertserver.ws.test;

import io.restassured.response.ValidatableResponse;
import org.assertj.core.api.AbstractIntegerAssert;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class StatisticsManager implements TestExecutionListener {

    private static ValidatableResponse todaySnapshot;

    @Override
    public void beforeTestMethod(@NotNull TestContext testContext) {
        todaySnapshot = getTodayStatistics();
    }

    public static AbstractIntegerAssert<?> assertThatTodayStatistic(String statisticName) {
        final int originalValue = todaySnapshot.extract().path("[0]." + statisticName);
        final int currentValue = getTodayStatistics().extract().path("[0]." + statisticName);
        return assertThat(currentValue - originalValue)
                .as("'%s' statistic value increment", statisticName);
    }

    private static ValidatableResponse getTodayStatistics() {
        return given()
                .param("fromDate", LocalDate.now().toString())
                .param("toDate", LocalDate.now().toString())
                .get("/internal/api/v1/kpi")
                .then();
    }
}
