package fr.gouv.clea.client.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpClientWrapper {
    private HttpClient client;

    public HttpClientWrapper(){
        this.client = HttpClient.newHttpClient();
    }

    public <T> T get(String uri, Class<T> returnType) throws IOException, InterruptedException{
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).GET().build();
        HttpResponse<String> response =  this.client.send(request, HttpResponse.BodyHandlers.ofString());
        return (T) new ObjectMapper().readValue(response.body(), returnType);
    } 
    
    public <T> T post(String uri, String body, Class<T> returnType) throws IOException, InterruptedException{
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response =  this.client.send(request, HttpResponse.BodyHandlers.ofString());
        return (T) new ObjectMapper().readValue(response.body(), returnType);
    }

    public int postStatusCode(String uri, String body) throws IOException, InterruptedException{
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<Void> response =  this.client.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode();
    }
}
