package fr.gouv.stopc.robert.server.batch.service;

import com.google.protobuf.ByteString;
import com.mongodb.MongoException;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactResponse;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.model.ScoringResult;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.DocumentCallbackHandler;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactProcessingService {

    private final MongoTemplate mongoTemplate;

    private final CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplBlockingStub cryptoGrpcClient;

    private final PropertyLoader propertyLoader;

    private final ScoringStrategyService scoringStrategyService;

    private final ContactService contactService;

    private final IServerConfigurationService serverConfigurationService;

    private final IRegistrationService registrationService;

    @Timed(value = "robert.batch", extraTags = { "operation", "CONTACT_SCORING_STEP" })
    public void performs() {
        log.info("START : Contact scoring.");
        // long totalItemCount = contactService.count().longValue();
        // TOTAL_CONTACT_COUNT_KEY = totalItemCount

        mongoTemplate.executeQuery(
                new BasicQuery("{}"),
                "CONTACTS_TO_PROCESS",
                new ContactProcessingService.ContactScoringRowCallbackHandler(
                        registrationService, scoringStrategyService, propertyLoader, serverConfigurationService
                )
        );

        log.info("END : Contact scoring.");
    }

    private class ContactScoringRowCallbackHandler implements DocumentCallbackHandler {

        private final IRegistrationService registrationService;

        private final ScoringStrategyService scoringStrategyService;

        private final PropertyLoader propertyLoader;

        private final IServerConfigurationService serverConfigurationService;

        final int timeDiffTolerance;

        final long tpstStartNTPsec;

        final byte[] serverCountryCode;

        public ContactScoringRowCallbackHandler(IRegistrationService registrationService,
                ScoringStrategyService scoringStrategyService,
                PropertyLoader propertyLoader,
                IServerConfigurationService serverConfigurationService) {
            this.registrationService = registrationService;
            this.scoringStrategyService = scoringStrategyService;
            this.propertyLoader = propertyLoader;
            this.serverConfigurationService = serverConfigurationService;

            this.timeDiffTolerance = propertyLoader.getHelloMessageTimeStampTolerance();
            this.tpstStartNTPsec = serverConfigurationService.getServiceTimeStart();
            this.serverCountryCode = new byte[] { serverConfigurationService.getServerCountryCode() };
        }

        @Override
        @Counted(value = "CONTACT_SCORING_STEP_PROCEEDED_REGISTRATIONS")
        public void processDocument(Document document) throws MongoException, DataAccessException {

            final var contact = mongoTemplate.getConverter().read(Contact.class, document);

            if (CollectionUtils.isEmpty(contact.getMessageDetails())) {
                log.info("No messages in contact, discarding contact");
                contactService.delete(contact);
                return;
            }

            ValidateContactResponse grpcResponse = null;
            try {
                grpcResponse = cryptoGrpcClient.validateContact(
                        ValidateContactRequest.newBuilder()
                                .setEbid(ByteString.copyFrom(contact.getEbid()))
                                .setEcc(ByteString.copyFrom(contact.getEcc()))
                                .setServerCountryCode(ByteString.copyFrom(serverCountryCode))
                                .addAllHelloMessageDetails(
                                        contact.getMessageDetails().stream()
                                                .map(this::toGrpcHelloMessageDetail)
                                                .collect(toList())
                                )
                                .build()
                );
            } catch (StatusRuntimeException ex) {
                log.error("RPC failed: {}", ex.getMessage());
            }
            final var response = grpcResponse;

            if (null == response) {
                log.info("The contact could not be validated. Discarding all its hello messages");
                contactService.delete(contact);
                return;
            }

            // Remove invalid HelloMessageDetails
            if (!response.getInvalidHelloMessageDetailsList().isEmpty()) {
                log.info("Removing {} invalid HelloMessageDetails", response.getInvalidHelloMessageDetailsCount());
                contact.getMessageDetails()
                        .removeIf(
                                helloMessageDetail -> matchesInvalidHelloMessageDetails(
                                        response.getInvalidHelloMessageDetailsList(), helloMessageDetail
                                )
                        );
            }

            if (contact.getMessageDetails().isEmpty()) {
                log.info("All hello messages have been rejected");
                contactService.delete(contact);
                return;
            }

            // Check step #2: is contact managed by this server ?
            if (!Arrays.equals(response.getCountryCode().toByteArray(), serverCountryCode)) {
                log.info(
                        "Country code {} is not managed by this server ({})", response.getCountryCode().toByteArray(),
                        serverCountryCode
                );
                contactService.delete(contact);
                return;
            }

            // Call db to get registration
            final var idA = response.getIdA().toByteArray();
            final Optional<Registration> registrationRecord = registrationService.findById(idA);

            if (registrationRecord.isEmpty()) {
                log.info("No identity exists for id_A {} extracted from ebid, discarding contact", idA);
                contactService.delete(contact);
                return;
            }
            final var registration = registrationRecord.get();

            contact.setMessageDetails(
                    contact.getMessageDetails().stream()
                            .filter(this::step5CheckDeltaTaAndTimeABelowThreshold)
                            .filter(
                                    helloMessage -> step6CheckTimeACorrespondsToEpochiA(
                                            response.getEpochId(), helloMessage.getTimeCollectedOnDevice()
                                    )
                            )
                            .collect(toList())
            );

            if (CollectionUtils.isEmpty(contact.getMessageDetails())) {
                log.info("Contact did not contain any valid messages; discarding contact");
                contactService.delete(contact);
                return;
            }

            try {
                step9ScoreAndAddContactInListOfExposedEpochs(contact, response.getEpochId(), registration);
                this.registrationService.saveRegistration(registration);
                contactService.delete(contact);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private fr.gouv.stopc.robert.crypto.grpc.server.messaging.HelloMessageDetail toGrpcHelloMessageDetail(
                HelloMessageDetail helloMessageDetail) {
            return fr.gouv.stopc.robert.crypto.grpc.server.messaging.HelloMessageDetail
                    .newBuilder()
                    .setTimeSent(helloMessageDetail.getTimeFromHelloMessage())
                    .setTimeReceived(helloMessageDetail.getTimeCollectedOnDevice())
                    .setMac(ByteString.copyFrom(helloMessageDetail.getMac()))
                    .build();
        }

        private boolean matchesInvalidHelloMessageDetails(
                List<fr.gouv.stopc.robert.crypto.grpc.server.messaging.HelloMessageDetail> invalidHelloMessageDetails,
                HelloMessageDetail helloMessageDetail) {
            return invalidHelloMessageDetails.stream()
                    .anyMatch(
                            invalid -> Arrays.equals(helloMessageDetail.getMac(), invalid.getMac().toByteArray())
                                    && helloMessageDetail.getTimeCollectedOnDevice() == invalid.getTimeReceived()
                                    && helloMessageDetail.getTimeFromHelloMessage() == invalid.getTimeSent()
                    );
        }

        /**
         * Robert Spec Step #5: check that the delta between tA (16 bits) & timeA (32
         * bits) [truncated to 16bits] is below threshold.
         */
        private boolean step5CheckDeltaTaAndTimeABelowThreshold(HelloMessageDetail helloMessageDetail) {
            // Process 16-bit values for sanity check
            final long timeFromHelloNTPsecAs16bits = CastUtils
                    .castIntegerToLong(helloMessageDetail.getTimeFromHelloMessage(), 2);
            final long timeFromDeviceAs16bits = CastUtils.castLong(helloMessageDetail.getTimeCollectedOnDevice(), 2);

            if (TimeUtils
                    .toleranceCheckWithWrap(timeFromHelloNTPsecAs16bits, timeFromDeviceAs16bits, timeDiffTolerance)) {
                return true;
            }

            log.warn(
                    "Time tolerance was exceeded: |{} (HELLO) vs {} (receiving device)| > {}; discarding HELLO message",
                    timeFromHelloNTPsecAs16bits,
                    timeFromDeviceAs16bits,
                    timeDiffTolerance
            );
            return false;
        }

        /**
         * Robert Spec Step #6
         */
        private boolean step6CheckTimeACorrespondsToEpochiA(int epochId, long timeFromDevice) {
            final long epochIdFromMessage = TimeUtils.getNumberOfEpochsBetween(tpstStartNTPsec, timeFromDevice);

            // Check if epochs match with a limited tolerance
            if (Math.abs(epochIdFromMessage - epochId) > 1) {
                log.warn(
                        "Epochid from message {} vs epochid from ebid {} > 1 (tolerance); discarding HELLO message",
                        epochIdFromMessage,
                        epochId
                );
                return false;
            }
            return true;
        }

        /**
         * Robert spec Step #9: add i_A in LEE_A
         */
        private void step9ScoreAndAddContactInListOfExposedEpochs(Contact contact, int epochIdFromEBID,
                Registration registrationRecord) throws Exception {
            List<EpochExposition> exposedEpochs = registrationRecord.getExposedEpochs();

            // Exposed epochs should be empty, never null
            if (Objects.isNull(exposedEpochs)) {
                exposedEpochs = new ArrayList<>();
            }

            // Add EBID's epoch to exposed epochs list
            Optional<EpochExposition> epochToAddTo = exposedEpochs.stream()
                    .filter(item -> item.getEpochId() == epochIdFromEBID)
                    .findFirst();

            ScoringResult scoredRisk = this.scoringStrategyService.execute(contact);
            if (epochToAddTo.isPresent()) {
                List<Double> epochScores = epochToAddTo.get().getExpositionScores();
                epochScores.add(scoredRisk.getRssiScore());
            } else {
                exposedEpochs.add(
                        EpochExposition.builder()
                                .expositionScores(Collections.singletonList(scoredRisk.getRssiScore()))
                                .epochId(epochIdFromEBID)
                                .build()
                );
            }
            registrationRecord.setExposedEpochs(exposedEpochs);
            registrationRecord.setOutdatedRisk(true);

        }
    }
}
