package fr.gouv.stopc.robertserver.ws.config;

import io.restassured.RestAssured;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class RestAssuredManager implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        RestAssured.port = testContext.getApplicationContext()
                .getEnvironment()
                .getRequiredProperty("local.server.port", Integer.class);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
