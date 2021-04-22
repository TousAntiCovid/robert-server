package fr.gouv.clea.client.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.clea.client.model.HTTPResponse;

public class HttpClientWrapper {
    private HttpClient client;
    private Map<String,String> headers;

    public HttpClientWrapper(){
        // this.client = HttpClient.newHttpClient();
        this.client = this.unsecureSSLHttpClient();
        this.headers = new HashMap<>();
    }
    
    private HttpClient unsecureSSLHttpClient() {
        TrustManager[] trustAllCerts = { new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            } } };

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }

        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(10000))
                .sslContext(sslContext) // SSL context 'sc' initialised as earlier
                .build();
    }

    public <T extends HTTPResponse> T get(String uri, Class<T> returnType) throws IOException, InterruptedException{
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).GET().build();
        HttpResponse<String> response =  this.client.send(request, HttpResponse.BodyHandlers.ofString());
        T typedResponse = (T) new ObjectMapper().readValue(response.body(), returnType);
        typedResponse.statusCode = response.statusCode();
        return typedResponse;
    } 

    public int getStatusCode(String uri) throws IOException, InterruptedException{
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).GET().build();
        HttpResponse<String> response =  this.client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    } 

    public <T extends HTTPResponse> T post(String uri, String body, Class<T> returnType) throws IOException, InterruptedException{
        Builder requestBuilder = HttpRequest.newBuilder(URI.create(uri)).POST(HttpRequest.BodyPublishers.ofString(body));
        for(Entry<String, String> header : headers.entrySet()){
            requestBuilder.setHeader(header.getKey(), header.getValue());
        }
        requestBuilder.setHeader("Content-Type", "application/json");
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response =  this.client.send(request, HttpResponse.BodyHandlers.ofString());
        T typedResponse = (T) new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(response.body(), returnType);
        typedResponse.statusCode = response.statusCode();
        return typedResponse;
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
