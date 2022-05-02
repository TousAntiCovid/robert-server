package fr.gouv.stopc.robert.crypto.grpc.server.test.matchers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.robert.crypto.grpc.server.service.impl.CryptoGrpcServiceBaseImpl;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.service.IClientKeyStorageService;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class TuplesMatchers {

    private final static String UNEXPECTED_FAILURE_MESSAGE = "Should not fail";

    private final static int NUMBER_OF_DAYS_FOR_BUNDLES = 4;

    private static byte[] getIdFromDecryptedEBID(byte[] ebid) {
        byte[] idA = new byte[5];
        System.arraycopy(ebid, 3, idA, 0, idA.length);
        return idA;
    }

    private static int getEpochIdFromDecryptedEBID(byte[] ebid) {
        byte[] epochId = new byte[3];
        System.arraycopy(ebid, 0, epochId, 0, epochId.length);
        return ByteUtils.convertEpoch24bitsToInt(epochId);
    }

    private static boolean checkTuplesForDay(List<CryptoGrpcServiceBaseImpl.EphemeralTupleJson> tuples, byte[] key,
            CryptoService cryptoService) {
        byte[] id = null;
        boolean atLeastOneError = false;
        for (CryptoGrpcServiceBaseImpl.EphemeralTupleJson tuple : tuples) {
            int epochIdFromTuple = tuple.getEpochId();
            try {
                byte[] decryptedEbid = cryptoService
                        .decryptEBID(new CryptoSkinny64(key), tuple.getKey().getEbid());
                byte[] idFromMessage = getIdFromDecryptedEBID(decryptedEbid);
                if (id == null) {
                    id = idFromMessage;
                } else {
                    if (!Arrays.equals(id, idFromMessage)) {
                        log.error(
                                "ids do not match from first message {} and from other message {}", id, idFromMessage
                        );
                        atLeastOneError = true;
                    }
                }
                int epochIdFromMessage = getEpochIdFromDecryptedEBID(decryptedEbid);
                if (epochIdFromMessage != epochIdFromTuple) {
                    log.error(
                            "epoch ids do not match from message {} and from tuple {}", epochIdFromMessage,
                            epochIdFromTuple
                    );
                    atLeastOneError = true;
                }
            } catch (RobertServerCryptoException e) {
                log.error("An error occurred during EBID decription", e);
                atLeastOneError = true;
            }
        }
        return !atLeastOneError;
    }

    private static boolean checkTuplesContentMatchesKeysForDays(
            Collection<CryptoGrpcServiceBaseImpl.EphemeralTupleJson> decodedTuples,
            int epochId,
            byte[][] serverKeys,
            CryptoService cryptoService) {

        ArrayList<CryptoGrpcServiceBaseImpl.EphemeralTupleJson> list = new ArrayList(decodedTuples);

        int offset = TimeUtils.remainingEpochsForToday(epochId);
        int lowerBound = 0;
        ArrayList<Boolean> results = new ArrayList<>();
        for (int i = 0; i < serverKeys.length; i++) {
            List<CryptoGrpcServiceBaseImpl.EphemeralTupleJson> listToProcess = list.subList(lowerBound, offset);
            log.info("Chunking list of size {}", listToProcess.size());
            results.add(checkTuplesForDay(listToProcess, serverKeys[i], cryptoService));
            lowerBound = offset;
            offset += 96;
        }
        return results.stream().allMatch(Boolean::valueOf);
    }

    public static boolean checkTuples(byte[] id, byte[] tuples, int epochId, byte[][] serverKeys,
            IClientKeyStorageService clientStorageService, CryptoService cryptoService) {
        CryptoAESGCM aesGcm = new CryptoAESGCM(clientStorageService.findKeyById(id).get().getKeyForTuples());
        try {
            byte[] decryptedTuples = aesGcm.decrypt(tuples);
            ObjectMapper objectMapper = new ObjectMapper();
            Collection<CryptoGrpcServiceBaseImpl.EphemeralTupleJson> decodedTuples = objectMapper.readValue(
                    decryptedTuples,
                    new TypeReference<Collection<CryptoGrpcServiceBaseImpl.EphemeralTupleJson>>() {
                    }
            );
            boolean sizeMatches = ((NUMBER_OF_DAYS_FOR_BUNDLES - 1) * 96
                    + TimeUtils.remainingEpochsForToday(epochId)) == decodedTuples.size();

            return sizeMatches
                    && checkTuplesContentMatchesKeysForDays(decodedTuples, epochId, serverKeys, cryptoService);
        } catch (RobertServerCryptoException | IOException e) {
            fail(UNEXPECTED_FAILURE_MESSAGE);
        }
        return false;
    }

}
