package fr.gouv.clea.prefixes;

import fr.gouv.clea.indexation.model.output.Prefix;
import fr.gouv.clea.service.PrefixesStorageService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PrefixesMemoryWriterTest {

    @Mock
    private PrefixesStorageService prefixesStorageService;

    private final int prefixesLength = 2;

    @Test
    void write_adds_prefix_if_absent_and_adds_ltid_to_same_prefix() {

        String prefix = "c1";
        final String uuid1 = "c10e44c1-4aa9-46c5-b7cd-c6521088202a";
        final String uuid2 = "c10e44c1-4aa9-46c5-b7cd-c6521088202b";

        // list of list of strings because of writer interface contract
        List<String> list = List.of(uuid1, uuid2);
        final List<? extends List<String>> ltidsList = List.of(list);

        final PrefixesMemoryWriter writer = new PrefixesMemoryWriter(prefixesStorageService, prefixesLength);

        writer.write(ltidsList);
        
        Mockito.verify(prefixesStorageService, times(list.size())).addPrefixIfAbsent(ArgumentMatchers.matches(prefix));
        Mockito.verify(prefixesStorageService, times(list.size())).addLtidToPrefix(eq(prefix), anyString());
        Mockito.verify(prefixesStorageService, times(1)).addLtidToPrefix(prefix, uuid1);
        Mockito.verify(prefixesStorageService, times(1)).addLtidToPrefix(prefix, uuid2);
    }
}
