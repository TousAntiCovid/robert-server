package fr.gouv.stopc.robert.server.batch.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.CollectionUtils;

import com.google.protobuf.ByteString;

import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactRequest;
import fr.gouv.stopc.robert.server.batch.exception.RobertScoringException;
import fr.gouv.stopc.robert.server.batch.model.ScoringResult;
import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.extern.slf4j.Slf4j;

import static java.util.function.Predicate.*;
import static java.util.stream.Collectors.toList;

/**
 * Process contacts to compute the score and evaluate the risk.
 * It includes many checks according to Robert Specification (see section 6.2, page 9): 
 * https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf
 */
@Slf4j
public class ContactProcessor implements ItemProcessor<Contact, Contact> {

    private IServerConfigurationService serverConfigurationService;

    private IRegistrationService registrationService;

    private ICryptoServerGrpcClient cryptoServerClient;

    private ScoringStrategyService scoringStrategy;

    private PropertyLoader propertyLoader;

    private int nbToBeProcessed;

    private int nbToBeDiscarded;

    public ContactProcessor(
            final IServerConfigurationService serverConfigurationService,
            final IRegistrationService registrationService,
             final ICryptoServerGrpcClient cryptoServerClient,
            final ScoringStrategyService scoringStrategy,
            final PropertyLoader propertyLoader) {

        this.serverConfigurationService = serverConfigurationService;
        this.registrationService = registrationService;
         this.cryptoServerClient = cryptoServerClient;
        this.scoringStrategy = scoringStrategy;
        this.propertyLoader = propertyLoader;
    }

    /**
     * NOTE:
     * validation step order has evolved from spec because of delegation of validation of messages to crypto back-end
     * @param contact
     * @return
     * @throws RobertServerCryptoException
     * @throws RobertScoringException
     */
    @Override
    public Contact process(Contact contact) throws RobertServerCryptoException, RobertScoringException {
        log.debug("Contact processing started");

        if (CollectionUtils.isEmpty(contact.getMessageDetails())) {
            log.warn("No messages in contact; discarding contact");
            return contact;
        }

        byte[] serverCountryCode = new byte[1];
        serverCountryCode[0] = this.serverConfigurationService.getServerCountryCode();

        final var helloMessageDetails = contact.getMessageDetails().stream()
                .map(
                        helloMessageDetail -> fr.gouv.stopc.robert.crypto.grpc.server.messaging.HelloMessageDetail
                                .newBuilder()
                                .setTimeSent(helloMessageDetail.getTimeFromHelloMessage())
                                .setTimeReceived(helloMessageDetail.getTimeCollectedOnDevice())
                                .setMac(ByteString.copyFrom(helloMessageDetail.getMac()))
                                .build()
                )
                .collect(toList());

        final var response = cryptoServerClient.validateContactHelloMessageMac(
                ValidateContactRequest.newBuilder()
                        .setEbid(ByteString.copyFrom(contact.getEbid()))
                        .setEcc(ByteString.copyFrom(contact.getEcc()))
                        .setServerCountryCode(
                                ByteString
                                        .copyFrom(new byte[] { this.serverConfigurationService.getServerCountryCode() })
                        )
                        .addAllHelloMessageDetails(helloMessageDetails)
                        .build()
        );

        if (null == response) {
            log.warn("The contact could not be validated. Discarding all its hello messages");
            return contact;
        }

        // remove invalid HelloMessageDetails
        if (!response.getInvalidHelloMessageDetailsList().isEmpty()) {
            log.info(
                    "Removing HelloMessageDetails having invalid Mac: {}", response.getInvalidHelloMessageDetailsCount()
            );
            contact.getMessageDetails()
                    .removeIf(
                            helloMessageDetail -> response.getInvalidHelloMessageDetailsList().stream()
                                    .anyMatch(
                                            invalid -> Arrays
                                                    .equals(helloMessageDetail.getMac(), invalid.getMac().toByteArray())
                                                    && helloMessageDetail.getTimeCollectedOnDevice() == invalid
                                                            .getTimeReceived()
                                                    && helloMessageDetail.getTimeFromHelloMessage() == invalid
                                                            .getTimeSent()
                                    )
                    );
        }

        if (contact.getMessageDetails().isEmpty()) {
            log.info("All hello messages have been rejected.");
            return contact;
        }

        // Call db to get registration
        final var idA = response.getIdA().toByteArray();
        final Optional<Registration> registrationRecord = registrationService.findById(idA);

        if (!registrationRecord.isPresent()) {
            log.info("Recovered id_A is unknown (fake or now unregistered?): {}; discarding contact", idA);
            return contact;
        }
        final var registration = registrationRecord.get();

        List<HelloMessageDetail> toBeDiscarded = new ArrayList<>();

        Integer epoch = null;
        this.nbToBeProcessed = contact.getMessageDetails().size();

        log.debug("{} HELLO message(s) to process", this.nbToBeProcessed);
        for (HelloMessageDetail helloMessageDetail : contact.getMessageDetails()) {

            // Check step #2: is contact managed by this server?
            if (!Arrays.equals(response.getCountryCode().toByteArray(), serverCountryCode)) {
                log.info(
                        "Country code {} is not managed by this server ({}); rerouting contact to federation network",
                        response.getCountryCode().toByteArray(),
                        serverCountryCode
                );

                // TODO: send the message to the dedicated country server
                // remove the message from the database
                return contact;
            } else {
                epoch = response.getEpochId();

                // Check steps #5, #6
                if (!step5CheckDeltaTaAndTimeABelowThreshold(helloMessageDetail)
                        || !step6CheckTimeACorrespondsToEpochiA(
                                response.getEpochId(),
                                helloMessageDetail.getTimeCollectedOnDevice()
                        )) {
                    toBeDiscarded.add(helloMessageDetail);
                }
            }
        }

        contact.setMessageDetails(
                contact.getMessageDetails().stream()
                        .filter(not(toBeDiscarded::contains))
                        .collect(toList())
        );

        if (CollectionUtils.isEmpty(contact.getMessageDetails())) {
            log.warn("Contact did not contain any valid messages; discarding contact");
            this.displayStatus();
            return contact;
        }

        step9ScoreAndAddContactInListOfExposedEpochs(contact, epoch, registration);

        this.registrationService.saveRegistration(registration);

        this.displayStatus();

        return contact;
    }

    /**
     *  Robert Spec Step #5: check that the delta between tA (16 bits) & timeA (32 bits) [truncated to 16bits] is below threshold.
     */
    private boolean step5CheckDeltaTaAndTimeABelowThreshold(HelloMessageDetail helloMessageDetail) {
        // Process 16-bit values for sanity check
        final long timeFromHelloNTPsecAs16bits = castIntegerToLong(helloMessageDetail.getTimeFromHelloMessage(), 2);
        final long timeFromDeviceAs16bits = castLong(helloMessageDetail.getTimeCollectedOnDevice(), 2);
        final int timeDiffTolerance = this.propertyLoader.getHelloMessageTimeStampTolerance();

        if (TimeUtils.toleranceCheckWithWrap(timeFromHelloNTPsecAs16bits, timeFromDeviceAs16bits, timeDiffTolerance)) {
            return true;
        }

        log.warn("Time tolerance was exceeded: |{} (HELLO) vs {} (receiving device)| > {}; discarding HELLO message",
                timeFromHelloNTPsecAs16bits,
                timeFromDeviceAs16bits,
                timeDiffTolerance);
        return false;
    }



    /**
     *  Robert Spec Step #6
     */
    private boolean step6CheckTimeACorrespondsToEpochiA(int epochId, long timeFromDevice) {
        final long tpstStartNTPsec = this.serverConfigurationService.getServiceTimeStart();
        long epochIdFromMessage = TimeUtils.getNumberOfEpochsBetween(tpstStartNTPsec, timeFromDevice);

        // Check if epochs match with a limited tolerance
        if (Math.abs(epochIdFromMessage - epochId) > 1) {
            log.warn("Epochid from message {}  vs epochid from ebid  {} > 1 (tolerance); discarding HELLO message",
                    epochIdFromMessage,
                    epochId);
            return false;
        }
        return true;
    }

    /**
     * Robert spec Step #9: add i_A in LEE_A
     */
    private void step9ScoreAndAddContactInListOfExposedEpochs(Contact contact, int epochIdFromEBID, Registration registrationRecord) throws RobertScoringException {
        List<EpochExposition> exposedEpochs = registrationRecord.getExposedEpochs();

        // Exposed epochs should be empty, never null
        if (Objects.isNull(exposedEpochs)) {
            exposedEpochs = new ArrayList<>();
        }

        // Add EBID's epoch to exposed epochs list
        Optional<EpochExposition> epochToAddTo = exposedEpochs.stream()
                .filter(item -> item.getEpochId() == epochIdFromEBID)
                .findFirst();

        ScoringResult scoredRisk =  this.scoringStrategy.execute(contact);
        if (epochToAddTo.isPresent()) {
            List<Double> epochScores = epochToAddTo.get().getExpositionScores();
            epochScores.add(scoredRisk.getRssiScore());
        } else {
            exposedEpochs.add(EpochExposition.builder()
                    .expositionScores(Arrays.asList(scoredRisk.getRssiScore()))
                    .epochId(epochIdFromEBID)
                    .build());
        }
        registrationRecord.setExposedEpochs(exposedEpochs);

    }

    private void displayStatus() {
        log.debug("{} HELLO message(s) discarded", this.nbToBeDiscarded);
        log.debug("{} HELLO message(s) successfull processed", (this.nbToBeProcessed - this.nbToBeDiscarded));
    }

    private long castIntegerToLong(int x, int nbOfSignificantBytes) {
        int shift = nbOfSignificantBytes * 8;
        return Integer.toUnsignedLong(x << shift >>> shift);
    }

    private long castLong(long x, int nbOfSignificantBytes) {
        int shift = (Long.BYTES - nbOfSignificantBytes) * 8;
        return x << shift >>> shift;
    }

}
