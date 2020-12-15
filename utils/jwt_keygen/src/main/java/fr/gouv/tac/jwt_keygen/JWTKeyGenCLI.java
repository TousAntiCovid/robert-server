package fr.gouv.tac.jwt_keygen;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

/**
 * JWT keys generator. 
 * If vault url and token provides, update directly keys with the newly generated key pair.
 * If specified, $ENV is added as a suffix to the key name.
 * 
 * TODO: need to refactor this code.
 * 
 * Usage examples: 
 *   java -jar jwt_keygen-0.1.0-SNAPSHOT.jar
 *   java -jar jwt_keygen-0.1.0-SNAPSHOT.jar http://127.0.0.1:8500 $VAULT_TOKEN [$ENV]
 */
public class JWTKeyGenCLI {

    private static final String ROBERT_PATH ="robert-server";
    private static final String TACW_PATH = "tac-warning-server";
    private static final String ROBERT_JWT_PUBLICKEY = "robert.jwt.publickey";
    private static final String ROBERT_JWT_PRIVATEKEY = "robert.jwt.privatekey";
    private static final String METADATA_PATH = "/v1/secret/metadata/"; 
    private static final String DATA_PATH = "/v1/secret/data/"; 
    private String baseUrl;
    private String authToken;
    private String environment;

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, ParseException {
        JWTKeyGenCLI runner = new JWTKeyGenCLI();
        runner.run(args);
    }

    public void run(String[] args) throws NoSuchAlgorithmException, IOException, ParseException {
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        String privateKeyString = Encoders.BASE64.encode(keyPair.getPrivate().getEncoded());
        String publicKeyString = Encoders.BASE64.encode(keyPair.getPublic().getEncoded());

        System.out.println("Private Key:" + privateKeyString);
        System.out.println("Public Key:" + publicKeyString);

        if (args.length == 2 || args.length == 3) {
            System.out.println("Updating Vault with generated keys ...");
            this.baseUrl = args[0];
            this.authToken = args[1];
            if (args.length == 3) {
                this.environment = "/" + args[2];
            } else {
                this.environment = "";
            }
        
            if ( this.hasVaultValues(TACW_PATH+this.environment) && this.hasVaultValues(ROBERT_PATH+this.environment) ) {
                JSONObject tacwData = this.getVaultValues(TACW_PATH+this.environment);
                JSONObject data = (JSONObject) tacwData.get("data");
                data.put(ROBERT_JWT_PUBLICKEY, publicKeyString);
                System.out.println(tacwData);
                this.setVaultSecret(TACW_PATH+this.environment, tacwData);

                JSONObject robertData = this.getVaultValues(ROBERT_PATH+this.environment);
                data = (JSONObject) robertData.get("data");
                data.put(ROBERT_JWT_PRIVATEKEY, privateKeyString);
                System.out.println(robertData);
                this.setVaultSecret(ROBERT_PATH+this.environment, robertData);
                System.out.println("Update done!");
            } else {
                System.out.println("Cannot update! Unable to fetch Vault values!");
            }
        }
    }

    public void setVaultSecret(String secretPath, JSONObject value) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut(this.baseUrl + DATA_PATH + secretPath);
        httpPut.setHeader("Content-type", "application/json");
        httpPut.setHeader("X-Vault-Token", this.authToken);
        httpPut.setHeader("X-Vault-Request", "true");
        httpPut.setEntity(new StringEntity(value.toJSONString()));
     
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };

        System.out.println(httpPut);
        System.out.println(value.toJSONString());
        String responseBody = httpclient.execute(httpPut, responseHandler);
        System.out.println(responseBody);
    }

    public boolean hasVaultValues(String path) throws IOException, ParseException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(this.baseUrl + METADATA_PATH + path);
        httpGet.setHeader("X-Vault-Token", this.authToken);
        httpGet.setHeader("X-Vault-Request", "true");
     
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };

        System.out.println(httpGet);
        String responseBody = httpclient.execute(httpGet, responseHandler);
        JSONParser jsonParser = new JSONParser();
        JSONObject response = (JSONObject) jsonParser.parse(responseBody);
        JSONObject data = (JSONObject) response.get("data");
        JSONObject versions = (JSONObject) data.get("versions");
        System.out.println("found " + versions.size() + " versions of " + path);
        return !versions.isEmpty();
    }
    
    public JSONObject getVaultValues(String path) throws IOException, ParseException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(this.baseUrl + DATA_PATH + path);
        httpGet.setHeader("X-Vault-Token", this.authToken);
        httpGet.setHeader("X-Vault-Request", "true");
     
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                System.out.println(response);
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };

        System.out.println(httpGet);
        String responseBody = httpclient.execute(httpGet, responseHandler);
        JSONParser jsonParser = new JSONParser();
        JSONObject response = (JSONObject) jsonParser.parse(responseBody);
        JSONObject data = (JSONObject) response.get("data");
        System.out.println(data);
        return data;
    }

}
