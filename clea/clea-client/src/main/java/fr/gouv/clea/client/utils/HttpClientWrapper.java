package fr.gouv.clea.client.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpClientWrapper {
    private HttpClient client;
    private Map<String,String> headers;

    public HttpClientWrapper(){
        this.client = HttpClient.newHttpClient();
        this.headers = new HashMap<>();
    }

    public <T> T get(String uri, Class<T> returnType) throws IOException, InterruptedException{
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).GET().build();
        HttpResponse<String> response =  this.client.send(request, HttpResponse.BodyHandlers.ofString());
        return (T) new ObjectMapper().readValue(response.body(), returnType);
    } 

    public int getStatusCode(String uri) throws IOException, InterruptedException{
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).GET().build();
        HttpResponse<String> response =  this.client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    } 

    public <T> T post(String uri, String body, Class<T> returnType) throws IOException, InterruptedException{
        Builder requestBuilder = HttpRequest.newBuilder(URI.create(uri)).POST(HttpRequest.BodyPublishers.ofString(body));
        for(Entry<String, String> header : headers.entrySet()){
            requestBuilder.setHeader(header.getKey(), header.getValue());
        }
        requestBuilder.setHeader("Content-Type", "application/json");
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response =  this.client.send(request, HttpResponse.BodyHandlers.ofString());
        return (T) new ObjectMapper().readValue(response.body(), returnType);
    }

    public int postStatusCode(String uri, String body) throws IOException, InterruptedException{
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<Void> response =  this.client.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode();
    }

    public void addAuthorizationToken(String token){
       this.addHeader("Authorization", token);
    }

    public void addHeader(String header, String value){
        this.headers.put(header, value);
    }
}
