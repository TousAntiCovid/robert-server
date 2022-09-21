package fr.gouv.stopc.robertserver.crypto.test;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class ClockManager implements TestExecutionListener {

    private static RobertClock ROBERT_CLOCK = new RobertClock("2020-06-01");

    @Override
    public void beforeTestMethod(final TestContext testContext) throws Exception {
        ROBERT_CLOCK = testContext.getApplicationContext()
                .getBean(RobertClock.class);
    }

    public static RobertClock clock() {
        return ROBERT_CLOCK;
    }
}
