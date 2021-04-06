package fr.gouv.clea.prefixes;

import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.indexation.model.output.Prefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PrefixesMemoryWriter implements ItemWriter<List<String>> {

    private final PrefixesStorageService prefixesStorageService;

    private final int prefixLength;

    public PrefixesMemoryWriter(BatchProperties config, PrefixesStorageService prefixesStorageService) {
        this.prefixLength = config.getPrefixLength();
        this.prefixesStorageService = prefixesStorageService;
    }

    @Override
    public void write(List<? extends List<String>> ltids) {
        ltids.get(0).forEach(ltid -> {
            final String prefix = Prefix.of(ltid, prefixLength);
            prefixesStorageService.getClustersMap().computeIfAbsent(prefix, p -> new ArrayList<>());
            prefixesStorageService.getClustersMap().get(prefix).add(ltid);
        });
    }
}
