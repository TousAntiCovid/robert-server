package fr.gouv.stopc.robert.server.batch.manager;

import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class HelloMessageFactory implements TestExecutionListener {

    private static PropertyLoader propertyLoader;

    private static IServerConfigurationService serverConfigurationService;

    @Override
    public void beforeTestMethod(@NonNull TestContext testContext) {
        propertyLoader = testContext.getApplicationContext().getBean(PropertyLoader.class);
        serverConfigurationService = testContext.getApplicationContext().getBean(IServerConfigurationService.class);
    }

    private static int TEN_SECONDS = 10;

    /**
     * Generate HelloMessage each 10 seconds starting and during seconds
     */
    public static List<HelloMessageDetail> generateHelloMessagesStartingAndDuringSeconds(
            RobertClock.RobertInstant starting, int durationInSec) {
        var messageDetails = new ArrayList<HelloMessageDetail>();

        var numberOfIterations = durationInSec / TEN_SECONDS;

        messageDetails.addAll(
                IntStream.rangeClosed(1, numberOfIterations)
                        .mapToObj(
                                j -> HelloMessageDetail.builder()
                                        .rssiCalibrated(randRssi())
                                        .timeCollectedOnDevice(
                                                starting.plus(j + TEN_SECONDS, ChronoUnit.SECONDS).asNtpTimestamp()
                                        )
                                        .timeFromHelloMessage(
                                                starting.plus(j + TEN_SECONDS + 1, ChronoUnit.SECONDS)
                                                        .as16LessSignificantBits()
                                        )
                                        .mac(("mac___" + j).getBytes())
                                        .build()
                        )
                        .collect(toList())
        );

        return messageDetails;
    }

    public static List<HelloMessageDetail> generateHelloMessageWithTimeCollectedOnDeviceExceeded(
            RobertClock.RobertInstant time) {
        var messageDetails = new ArrayList<HelloMessageDetail>();

        var exceededTime = time.plus(propertyLoader.getHelloMessageTimeStampTolerance() + 1, ChronoUnit.SECONDS);

        messageDetails.add(
                HelloMessageDetail.builder()
                        .rssiCalibrated(randRssi())
                        .timeCollectedOnDevice(
                                exceededTime.asNtpTimestamp()
                        )
                        .timeFromHelloMessage(
                                time.as16LessSignificantBits()
                        )
                        .mac(("mac___exceeded_time").getBytes())
                        .build()
        );

        return messageDetails;
    }

    public static List<HelloMessageDetail> generateHelloMessageWithDivergenceBetweenMessageAndRegistration(
            RobertClock.RobertInstant time) {
        var messageDetails = new ArrayList<HelloMessageDetail>();

        // In Order to provoque
        var exceededTime = time.plus(serverConfigurationService.getEpochDurationSecs() * 2, ChronoUnit.SECONDS);

        messageDetails.add(
                HelloMessageDetail.builder()
                        .rssiCalibrated(randRssi())
                        .timeCollectedOnDevice(
                                exceededTime.asNtpTimestamp()
                        )
                        .timeFromHelloMessage(
                                exceededTime.as16LessSignificantBits()
                        )
                        .mac(("mac___exceeded_time").getBytes())
                        .build()
        );

        return messageDetails;
    }

    // public static List<HelloMessageDetail>
    // generateHelloMessageWithTimeCollectedOnDeviceExceeded(RobertClock.RobertInstant
    // time) {
    // var messageDetails = new ArrayList<HelloMessageDetail>();
    //
    // var exceededTime =
    // time.plus(propertyLoader.getHelloMessageTimeStampTolerance() + 1,
    // ChronoUnit.SECONDS);
    //
    // messageDetails.add(
    // HelloMessageDetail.builder()
    // .rssiCalibrated(randRssi())
    // .timeCollectedOnDevice(
    // exceededTime.asNtpTimestamp()
    // )
    // .timeFromHelloMessage(
    // time.as16LessSignificantBits()
    // )
    // .mac(("mac___exceeded_time").getBytes())
    // .build()
    // );
    //
    // return messageDetails;

    // public static Contact addHelloMessagesForContactStartingAndDuring(Contact
    // contact, RobertClock.RobertInstant starting, int durationInSec) {
    // var messageDetails =
    // Optional.ofNullable(contact.getMessageDetails()).orElse(new ArrayList<>());
    //
    // var numberOfIterations = durationInSec / TEN_SECONDS;
    //
    // messageDetails.addAll(
    // IntStream.rangeClosed(1, numberOfIterations)
    // .mapToObj(
    // j -> HelloMessageDetail.builder()
    // .rssiCalibrated(randRssi())
    // .timeCollectedOnDevice(
    // starting.plus(j + TEN_SECONDS, ChronoUnit.SECONDS).asNtpTimestamp()
    // )
    // .timeFromHelloMessage(
    // starting.plus(j + TEN_SECONDS + 1, ChronoUnit.SECONDS)
    // .as16LessSignificantBits()
    // )
    // .mac(("mac___" + j).getBytes())
    // .build()
    // )
    // .collect(toList())
    // );
    //
    // contact.setMessageDetails(messageDetails);
    //
    // return contact;
    // }

    // public static Contact addBadHelloMessageWithExceededTimeToContact(Contact
    // contact, RobertClock.RobertInstant time) {
    // var messageDetails =
    // Optional.ofNullable(contact.getMessageDetails()).orElse(new ArrayList<>());
    //
    // var exceededTime =
    // time.plus(propertyLoader.getHelloMessageTimeStampTolerance() + 1,
    // ChronoUnit.SECONDS);
    //
    // messageDetails.add(
    // HelloMessageDetail.builder()
    // .rssiCalibrated(randRssi())
    // .timeCollectedOnDevice(
    // exceededTime.plus(TEN_SECONDS, ChronoUnit.SECONDS).asNtpTimestamp()
    // )
    // .timeFromHelloMessage(
    // time.plus(TEN_SECONDS + 1, ChronoUnit.SECONDS)
    // .as16LessSignificantBits()
    // )
    // .mac(("mac___exceeded_time").getBytes())
    // .build()
    // );
    //
    // contact.setMessageDetails(messageDetails);
    //
    // return contact;
    // }

    private static int randRssi() {
        return ThreadLocalRandom.current().nextInt(-75, -10);
    }
}
