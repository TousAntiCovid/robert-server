package fr.gouv.clea.prefixes;

import fr.gouv.clea.indexation.model.output.ClusterFile;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

@RequiredArgsConstructor
public class PrefixesMemoryWriter implements ItemWriter<List<String>> {

    private final PrefixesStorageService prefixesStorageService;

    @Override
    public void write(List<? extends List<String>> prefixes) {
        prefixes.get(0).forEach(prefix -> prefixesStorageService.getClustersMap().computeIfAbsent(prefix, key -> new ClusterFile()));
    }
}
