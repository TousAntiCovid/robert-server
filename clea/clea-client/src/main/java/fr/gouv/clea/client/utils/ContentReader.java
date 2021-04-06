package fr.gouv.clea.client.utils;

import java.net.URI;
import java.util.Optional;

public interface ContentReader {
    public Optional<String> getContent(URI resource);
    public URI uriFrom(String filePrefix, String... segments);
}
