package fr.gouv.stopc.submission.code.server.commun.service.impl;

import fr.gouv.stopc.submission.code.server.commun.service.IShortCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ShortCodeServiceImpl implements IShortCodeService {

    private static final String ALPHA_UPPER_CASE = "abcdefghijklmnopqrstuvwxyz".toUpperCase();
    private static final String NUMERIC= "0123456789";
    private static final Integer CODE_SIZE = 6;

    private static final SecureRandom sRandom = new SecureRandom();

    private static final List<Character> ALPHA_NUMERIC_CHAR_ARRAY = String
            .format(
                    "%s%s",
                    ALPHA_UPPER_CASE,
                    NUMERIC)
            .chars()
            .mapToObj(c -> (char) c)
            .collect(Collectors.toList());

    public String generateCode() {
        log.info("Generating random short code");
        final List<Character> characters = getShuffledAlphaNumList();

        /**
         *  The function sRandom.nexInt(i) create a number random from 0 to i-1.
         *  The StringBuilder is more performing than string+=string.tu
         */
        StringBuilder alphaNum = new StringBuilder();
        for (int i = 0; i < CODE_SIZE; i++) {
            alphaNum.append(characters.get(sRandom.nextInt(ALPHA_NUMERIC_CHAR_ARRAY.size())));
        }
        return alphaNum.toString();
    }

    /**
     * @return return a shuffled copy of ALPHA_NUMERIC_CHAR_ARRAY
     */
    protected static List<Character> getShuffledAlphaNumList() {
        final ArrayList<Character> tempAlphaNumList = new ArrayList<>(ALPHA_NUMERIC_CHAR_ARRAY);
        Collections.shuffle(tempAlphaNumList,sRandom);
        return tempAlphaNumList;
    }

}
