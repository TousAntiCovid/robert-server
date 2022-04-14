package fr.gouv.stopc.robertserver.ws.test.matchers;

import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static fr.gouv.stopc.robertserver.ws.test.matchers.JwtMatcher.isJwtSignedBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtMatcherTest {

    private static final String EXAMPLE_JWT = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0ZXN0In0.I8E1ZfiSUjQIiVvH6bBRGlPwt_qK-HMRZuFulz5hoK2WMjNNpuciE3tRqTV7_0S6mG6cm-IU9qdhDWkosZyxel2WaGR3ftSYtOkNek6GfxvLnoiSpK1coy4FJ4oBCecT113m1PH-mt1gAo35I8snarGw7EEPrCs0dLNEDS9HOElfbAo3069J_FeFRTYdzuQ3v_f_Wuboxm9xI1vJ94LB1Ze8OrxW8XMizTTCbxfrHvI8oASuzSviPcdUhp-7PBuPqPIgCw33cOT9tfCL43fCk1CCSmm8bjncZ_FrPweU2TojZxvlXe6D8x0TtNikmoFZn65e-0A_ykWNq9f0DTmv0A";

    private static final String PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDDZ9orHCOMO4ljhnf9aNgR0wknl2M2l8JYdWbN7hsAD0nfYhABBc1wJJm8LtnPCvTLXb+GsQ3aDlGunfjf+e/Oe4HTOD98tgwCptBXLLoNBSNh+sNo2Fppt9ImJOkmOFCyowDq1LmvbPdqWHlj4Tkg+4tyE3y4KETRx3qmQT4C0Hs4+3Cv70dzxTTd1zWOS73PIaOxNpf5fKEHcQLGIo9z+FMO9clcQ4fVnaWdkT+JOkriulmEZCv4QTFkESNaFbFrSXYnArlQIGzrEiYuyii2+BnLSNB6RJdNyDjZSirALNPx+tW6feb9Aq1+L1mWhNXktgAlEjwBRSJ7y0NsPwFzAgMBAAECggEABnzsQYItq/pOFX/hfAh/JBhdtXLRWH6xdT5wq2W8f8QCL+iRn3cR2ydb82Oa18UFW7tBILfAPf07uRCFsuoZjEGNjfXQJdPlkjD3ue1CQCxrgFVTJ6gHWHtB/wI6u4ojI8tFvIZTo/96N424ehiATRgNCTycZ4DuyAhXii2gYXd7qOz4nTT6d/se/1VPz2S3lGPZo2gPSeLPTO5CFjmyfFY+O3tbnsOK9Dz2Jwm7afP7rmq52/aWb7OPHTofmS+I1ga3bBtOOIc/RJMgsW7Zam90qWWYup4UK+XcBR9L1Nsb3pGxwV1tGrbHwx137RUHAQXYlORlrtZxubq1vYIwAQKBgQD86et3FMeAsRVEj/T5fk4Ir5DH5NXyppgFo1lyKpRFCCkJG777qu1jauu9y0jENBc3qVUR5koJAufuc5RLgUCo59s6yymoOFoMCAipGZZWuKAi431iwjkDpj6ax3ajS8erAXHkI8AUjoNOuWkxyd0nNEyWMO87+p/EsHTDhBAd4wKBgQDFykhDs+jBeJ4tMSQ3voRqo95EWzyQY5ZZiga9QbLpthOxRsqsgH5pJf+98GoTXp6lw++Zp3YW7KSx598ZVw1EGDBqFeFDdyGyUdBl9UGaUB+ZoPDTVz6K84ivmegfb9BeY6UkDW0Pl3PI5gqA6T+sGE/QOEHBhxoBid/5SwPjMQKBgQDzYbwJZUKzUjYQO6RYL7ayXxkpc8p7btvemRprZhQ29ipfLIc2Mn+ta2gexbBpt2McWmSWDBH06An/itQLcP+jSzPKOVYGHuxwpY6IBCqLm7HmwRqZXiR1oZafZhGCBVvI8WQDUm/2mEo+COj1U7fxkT+eTMr74iu7oL9MoxrLVwKBgQCmuRNfFmkNpKnYuEKqRosCBS2Xezy7DWcwBLwXjijM/nDtDxpKfGmSIrjHl78iorCiVF5ErDdLraBKYoTZrFp/WtB8rALdRq99y58aQrlQ5Vctvc8iprkOkahXNSnITg1bcy1CDZsv+F9eKiMSdQr5+uhUQQeUpVhTka/dSNF3EQKBgAy/w+XUvvUAWyU1jsh12RtYYIqcXmAb5P53NKwjNuGVFAQzGap5NI+LUV4wi/2T3Nyl11S001pHHFHY+IUJEVLsHXmNIx0k19cAT7yy1SpkBxGSGzb2BaEZWJzFNOvWsKMbkQwn/kOPKCyeBgjt3+IVTOz//FDI8+Cl7oJXmhcc";

    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAw2faKxwjjDuJY4Z3/WjYEdMJJ5djNpfCWHVmze4bAA9J32IQAQXNcCSZvC7Zzwr0y12/hrEN2g5Rrp343/nvznuB0zg/fLYMAqbQVyy6DQUjYfrDaNhaabfSJiTpJjhQsqMA6tS5r2z3alh5Y+E5IPuLchN8uChE0cd6pkE+AtB7OPtwr+9Hc8U03dc1jku9zyGjsTaX+XyhB3ECxiKPc/hTDvXJXEOH1Z2lnZE/iTpK4rpZhGQr+EExZBEjWhWxa0l2JwK5UCBs6xImLsootvgZy0jQekSXTcg42UoqwCzT8frVun3m/QKtfi9ZloTV5LYAJRI8AUUie8tDbD8BcwIDAQAB";

    private static final KeyPair KEYS;

    static {
        try {
            final var publicKey = new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY));
            final var privateKey = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIVATE_KEY));
            final var keyFactory = KeyFactory.getInstance("RSA");
            KEYS = new KeyPair(keyFactory.generatePublic(publicKey), keyFactory.generatePrivate(privateKey));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void can_verify_jwt_signature() {
        assertThat(EXAMPLE_JWT, isJwtSignedBy(KEYS));
    }

    @Test
    void can_detect_wrong_jwt_signature() {
        final var jwtWithInvalidSignature = EXAMPLE_JWT.replaceAll("\\.[^.]*$", ".invalid-signature");
        final var error = assertThrows(
                AssertionError.class,
                () -> assertThat(jwtWithInvalidSignature, isJwtSignedBy(KEYS))
        );
        assertThat(
                error.getMessage().replace("\r", ""), stringContainsInOrder(
                        "Expected: a jwt token with a valid signature",
                        "     but: signature of jwt \"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0ZXN0In0.invalid-signature\" is invalid"
                )
        );
    }

    @Test
    void can_verify_jwt_claim() {
        assertThat(
                EXAMPLE_JWT, isJwtSignedBy(KEYS)
                        .withClaim("iss", equalTo("test"))
        );
    }

    @Test
    void can_detect_wrong_claim_value() {
        final var error = assertThrows(
                AssertionError.class,
                () -> assertThat(
                        EXAMPLE_JWT, isJwtSignedBy(KEYS)
                                .withClaim("iss", equalTo("other value"))
                )
        );
        assertThat(
                error.getMessage().replace("\r", ""), equalTo(
                        "\n" +
                                "Expected: a jwt token with a valid signature and a claim \"iss\" with \"other value\"\n"
                                +
                                "     but: was \"test\""
                )
        );
    }

    @Test
    void can_detect_missing_claim() {
        final var error = assertThrows(
                AssertionError.class,
                () -> assertThat(
                        EXAMPLE_JWT, isJwtSignedBy(KEYS)
                                .withClaim("role", startsWith("admin"))
                )
        );
        assertThat(
                error.getMessage().replace("\r", ""), equalTo(
                        "\n" +
                                "Expected: a jwt token with a valid signature and a claim \"role\" with a string starting with \"admin\"\n"
                                +
                                "     but: was null"
                )
        );
    }
}
