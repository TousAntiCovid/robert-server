package fr.gouv.stopc.robertserver.ws.test

import fr.gouv.stopc.robertserver.ws.repository.model.KpiName
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.*
import io.restassured.RestAssured.given
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

class StatisticsManager : TestExecutionListener {

    companion object {

        private lateinit var todaySnapshot: ValidatableResponse

        fun assertThatTodayStatistic(statisticName: KpiName?): AbstractIntegerAssert<*> {
            val originalValue = todaySnapshot.extract().path<Int>(statisticName?.key)
            val currentValue = getStatistics().extract().path<Int>(statisticName?.key)
            return assertThat(currentValue - originalValue)
                .`as`("'${statisticName?.key}' statistic value increment")
        }

        fun assertThatTodayStatisticsDidntIncrement(kpis: List<KpiName>) {
            kpis.map { kpi -> assertThatTodayStatistic(kpi).isEqualTo(0) }
        }

        private fun getStatistics() = given()
            .get("/internal/api/v2/kpis")
            .then()
    }

    override fun beforeTestMethod(testContext: TestContext) {
        todaySnapshot = getStatistics()
    }
}
