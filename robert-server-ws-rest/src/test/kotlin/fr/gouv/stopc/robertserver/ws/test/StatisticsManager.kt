package fr.gouv.stopc.robertserver.ws.test

import io.restassured.RestAssured.given
import io.restassured.response.ValidatableResponse
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

class StatisticsManager : TestExecutionListener {

    companion object {

        private lateinit var todaySnapshot: ValidatableResponse

        fun assertThatTodayStatistic(statisticName: String?): AbstractIntegerAssert<*> {
            val originalValue = todaySnapshot!!.extract().path<Int>(statisticName)
            val currentValue = getStatistics().extract().path<Int>(statisticName)
            return assertThat(currentValue - originalValue)
                .`as`("'$statisticName' statistic value increment")
        }

        private fun getStatistics() = given()
            .get("/internal/api/v2/kpis")
            .then()
    }

    override fun beforeTestMethod(testContext: TestContext) {
        todaySnapshot = getStatistics()
    }
}
