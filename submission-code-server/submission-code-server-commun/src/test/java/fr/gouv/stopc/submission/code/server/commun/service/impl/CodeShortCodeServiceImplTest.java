package fr.gouv.stopc.submission.code.server.commun.service.impl;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeShortCodeServiceImplTest {


    @Test
    @Disabled( "for benchmark only")
    void generateCodeShortCodeTest() {
        Long size = new Long("300000");
        final long start = System.currentTimeMillis();
        final List<String> shortCodeIds = Stream.generate(new ShortCodeServiceImpl()::generateCode)
                .distinct()
                .limit(size)
                .collect(Collectors.toList());
        System.out.println(String.format("expected %s and get %s in %s millis", size, shortCodeIds.size(), System.currentTimeMillis()-start));
        assertEquals(size, shortCodeIds.size());
    }

    @Test
    void getShuffledAlphaNumListTest() {
        final long start = System.currentTimeMillis();
        final List<Character> shuffledAlphaNumList = ShortCodeServiceImpl.getShuffledAlphaNumList();
        System.out.println(String.format("expected %s and get %s in %s millis", 26+10, shuffledAlphaNumList.size(), System.currentTimeMillis()-start));
        assertEquals(26+10, shuffledAlphaNumList.size());
    }




}