package fr.gouv.clea.prefixes;

import fr.gouv.clea.indexation.model.output.ClusterFile;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PrefixesStorageService {

    //FIXME: what kind of concurrent object to use?
    @Getter
    private final Map<String, ClusterFile> clustersMap = new ConcurrentHashMap<>();
}
