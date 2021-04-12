package fr.gouv.clea.prefixes;

import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.indexation.model.output.Prefix;
import fr.gouv.clea.service.PrefixesStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

@RequiredArgsConstructor
public class PrefixesMemoryWriter implements ItemWriter<List<String>> {

    private final PrefixesStorageService prefixesStorageService;

    private final int prefixLength;

    public PrefixesMemoryWriter(final BatchProperties config, final PrefixesStorageService prefixesStorageService) {
        this.prefixLength = config.getStaticPrefixLength();
        this.prefixesStorageService = prefixesStorageService;
    }

    @Override
    public void write(final List<? extends List<String>> ltids) {
        ltids.get(0).forEach(ltid -> {
            final String prefix = Prefix.of(ltid, prefixLength);
            prefixesStorageService.addPrefixIfAbsent(prefix);
            prefixesStorageService.addLtidToPrefix(prefix, ltid);
        });
    }
}
