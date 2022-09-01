package fr.gouv.stopc.robert.crypto.grpc.server.test;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.service.RobertClock.RobertInstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class DataManager implements TestExecutionListener {

    public static final int FRENCH_COUNTRY_CODE = 33;

    public static final int NUMBER_OF_DAYS_FOR_BUNDLES = 4;

    @Autowired
    private RobertClock clock;

    /** Robert instant at the beginning of the test */
    public static RobertInstant NOW;

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        testContext.getApplicationContext()
                .getAutowireCapableBeanFactory()
                .autowireBean(this);

        NOW = clock.now();

    }

}
