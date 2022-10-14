package fr.gouv.stopc.robertserver.ws.test;

import fr.gouv.stopc.robertserver.common.RobertClock;
import io.restassured.RestAssured;
import org.hamcrest.Matcher;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import static org.hamcrest.Matchers.equalTo;

public class RestAssuredManager implements TestExecutionListener {

    private static RobertClock clock;

    @Override
    public void beforeTestMethod(TestContext testContext) {
        RestAssured.port = testContext.getApplicationContext()
                .getEnvironment()
                .getRequiredProperty("local.server.port", Integer.class);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        clock = testContext.getApplicationContext().getBean(RobertClock.class);
    }

    public static Matcher<Long> equalToServiceStartNtpTimeStamp() {
        return equalTo(clock.atEpoch(0).asNtpTimestamp());
    }
}
