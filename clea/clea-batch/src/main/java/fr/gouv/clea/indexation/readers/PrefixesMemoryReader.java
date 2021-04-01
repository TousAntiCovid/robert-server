package fr.gouv.clea.indexation.readers;

import org.springframework.batch.item.ItemReader;

public class PrefixesMemoryReader implements ItemReader<String> {

    @Override
    public String read() {
        //read items from PrefixesStorageService instance
        return null;
    }
}
