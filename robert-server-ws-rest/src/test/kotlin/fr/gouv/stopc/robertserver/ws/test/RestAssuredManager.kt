package fr.gouv.stopc.robertserver.ws.test

import fr.gouv.stopc.robertserver.common.RobertClock
import io.restassured.RestAssured
import io.restassured.specification.RequestSpecification
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

fun When() = RestAssured.`when`()
fun RequestSpecification.When() = this.`when`()

object RestAssuredManager : TestExecutionListener {

    private lateinit var clock: RobertClock

    override fun beforeTestMethod(testContext: TestContext) {
        RestAssured.port = testContext.applicationContext
            .environment
            .getRequiredProperty("local.server.port", Int::class.java)
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        clock = testContext.applicationContext.getBean(RobertClock::class.java)
    }
}
