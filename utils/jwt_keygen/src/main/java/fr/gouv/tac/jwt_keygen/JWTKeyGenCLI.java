package fr.gouv.tac.jwt_keygen;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

/**
 * JWT keys generator. 
 * Usage examples: 
 *   java -jar jwt_keygen-0.1.0-SNAPSHOT.jar
 *   java -jar jwt_keygen-0.1.0-SNAPSHOT.jar http://127.0.0.1:8500/v1/kv/ $VAULT_TOKEN
 */
public class JWTKeyGenCLI {

    private static final String ROBERT_JWT_PUBLICKEY = "robert.jwt.publickey";
    private static final String ROBERT_JWT_PRIVATEKEY = "robert.jwt.privatekey";
    private String baseUrl;
    private String authToken;

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        JWTKeyGenCLI runner = new JWTKeyGenCLI();
        runner.run(args);
    }

    public void run(String[] args) throws NoSuchAlgorithmException, IOException {
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        String privateKeyString = Encoders.BASE64.encode(keyPair.getPrivate().getEncoded());
        String publicKeyString = Encoders.BASE64.encode(keyPair.getPublic().getEncoded());

        System.out.println("Private Key:" + privateKeyString);
        System.out.println("Public Key:" + publicKeyString);

        if (args.length == 2) {
            System.out.println("Updating Vault with generated keys ...");
            this.baseUrl = args[0];
            this.authToken = args[1];
            this.setVaultKeyValue(ROBERT_JWT_PRIVATEKEY, privateKeyString);
            this.setVaultKeyValue(ROBERT_JWT_PUBLICKEY, publicKeyString);
            System.out.println("Update done!");
        }
    }

    public void setVaultKeyValue(String key, String value) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut(this.baseUrl + key);
        httpPut.setHeader("Content-type", "application/json");
        httpPut.setHeader("X-Vault-Token", this.authToken);
        httpPut.setHeader("X-Vault-Request", "true");
        httpPut.setEntity(new StringEntity("{\"value\":\"" + value + "\"}"));
     
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
        String responseBody = httpclient.execute(httpPut, responseHandler);
        System.out.println(responseBody);
    }
}
