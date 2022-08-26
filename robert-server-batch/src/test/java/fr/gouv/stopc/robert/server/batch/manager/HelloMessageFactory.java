package fr.gouv.stopc.robert.server.batch.manager;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelloMessageFactory {

    public static List<HelloMessageDetail> generateHelloMessagesStartingAndDuring(
            RobertClock.RobertInstant starting,
            Duration duration) {

        return Stream.iterate(
                starting,
                helloMessageInstant -> helloMessageInstant.isBefore(starting.plus(duration)),
                helloMessageInstant -> helloMessageInstant.plus(10, ChronoUnit.SECONDS)
        )
                .map(
                        helloMessageInstant -> HelloMessageDetail
                                .builder()
                                .rssiCalibrated(randRssi())
                                .timeCollectedOnDevice(
                                        helloMessageInstant.asNtpTimestamp()
                                )
                                .timeFromHelloMessage(
                                        helloMessageInstant.plus(1, ChronoUnit.SECONDS)
                                                .as16LessSignificantBits()
                                )
                                .mac(("mac___" + starting.toString()).getBytes())
                                .build()
                )
                .collect(Collectors.toList());
    }

    public static int randRssi() {
        return ThreadLocalRandom.current().nextInt(-75, -10);
    }
}
