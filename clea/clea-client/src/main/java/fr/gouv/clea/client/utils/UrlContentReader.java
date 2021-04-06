package fr.gouv.clea.client.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UrlContentReader implements ContentReader {
    public URI uriFrom(String filePrefix, String... segments) {
        return URI.create(Path.of(filePrefix, segments).toString());
    }
    
    public Optional<String> getContent(URI uri) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return Optional.of(response.body());
        } catch (IOException | InterruptedException e) {
            log.error("Error retrieving url " + uri, e);
            return Optional.empty();
        }
    }
}
