package fr.gouv.clea.prefixes;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PrefixesStorageService {

    @Getter
    private final Map<String, List<String>> prefixWithAssociatedLtidsMap = new ConcurrentHashMap<>();
    public static final List<String> ltidsList = new ArrayList<>();
    public static long multipleLtidCount = 0L;

}
