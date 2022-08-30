package fr.gouv.stopc.robert.crypto.grpc.server.test;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.time.temporal.ChronoUnit;

public class DataManager implements TestExecutionListener {

    public static final int SERVER_COUNTRY_CODE = 33;

    public static final int NUMBER_OF_DAYS_FOR_BUNDLES = 4;

    public static final int NUMBER_OF_EPOCHS_IN_A_DAY = 96;

    public static final int IV_LENGTH = 12;

    @Autowired
    private RobertClock clock;

    public static int currentEpochId;

    public static int epochIdInThePast;

    public static int epochIdInTheFuture;

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        testContext.getApplicationContext()
                .getAutowireCapableBeanFactory()
                .autowireBean(this);

        currentEpochId = clock.now().asEpochId();

        epochIdInThePast = clock.now().minus(25, ChronoUnit.DAYS).asEpochId();

        epochIdInTheFuture = clock.now().plus(10, ChronoUnit.DAYS).asEpochId();

    }

}
