package fr.gouv.clea.prefixes;

import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.indexation.model.output.Prefix;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;
import java.util.stream.Collectors;

public class PrefixesComputingProcessor implements ItemProcessor<List<String>, List<String>> {

    private final int prefixLength;

    public PrefixesComputingProcessor(BatchProperties config) {
        this.prefixLength = config.getPrefixLength();
    }

    @Override
    public List<String> process(List<String> ltidList) {
        return ltidList.stream().map(ltid -> Prefix.of(ltid, prefixLength)).collect(Collectors.toUnmodifiableList());
    }
}
