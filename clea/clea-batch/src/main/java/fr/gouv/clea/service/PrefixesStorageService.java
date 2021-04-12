package fr.gouv.clea.service;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PrefixesStorageService {

    @Getter
    private final Map<String, List<String>> prefixWithAssociatedLtidsMap = new ConcurrentHashMap<>();

    public void addPrefixIfAbsent(final String prefix) {
        prefixWithAssociatedLtidsMap.computeIfAbsent(prefix, p -> new ArrayList<>());
    }

    public void addLtidToPrefix(final String prefix, final String ltid) {
        prefixWithAssociatedLtidsMap.get(prefix).add(ltid);
    }
}
