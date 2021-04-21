package fr.gouv.clea.client.utils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileContentReader implements ContentReader {
    public URI uriFrom(String filePrefix, String... segments) {
        return Path.of(filePrefix, segments).toUri();
    }
    
    public Optional<String> getContent(URI fileUri) {
        try {
            String indexString = Files.readString(Path.of(fileUri), StandardCharsets.UTF_8);
            return Optional.of(indexString);
        } catch (IOException e) {
            log.error("Error retrieving file " + fileUri, e);
            return Optional.empty();
        }
    }
}
