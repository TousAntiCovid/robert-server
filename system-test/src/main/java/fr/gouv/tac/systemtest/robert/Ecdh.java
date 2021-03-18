package fr.gouv.tac.systemtest.robert;

import lombok.extern.slf4j.Slf4j;
import org.bson.internal.Base64;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

@Slf4j
public class Ecdh {

    public String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public String encodeHexString(byte[] byteArray) {
        StringBuilder hexStringBuffer = new StringBuilder();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    public byte hexToByte(String hexString) {
        int firstDigit = toDigit(hexString.charAt(0));
        int secondDigit = toDigit(hexString.charAt(1));
        return (byte) ((firstDigit << 4) + secondDigit);
    }

    private int toDigit(char hexChar) {
        int digit = Character.digit(hexChar, 16);
        if (digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: " + hexChar);
        }
        return digit;
    }

    public byte[] decodeHexString(String hexString) {
        if (hexString.length() % 2 == 1) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
        }
        return bytes;
    }


    public static void main(String[] args) throws Exception {

        // Generate ephemeral ECDH keypair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair kp = kpg.generateKeyPair();
        byte[] ourPk = kp.getPublic().getEncoded();

        log.info("RO = {}", -Math.log(1.0 - 0.1) / 15.0);
        // Display our public key
        log.info("Public Key: " + Base64.encode(ourPk));
        log.info("PRIVATE Key: " + Base64.encode(kp.getPrivate().getEncoded()));

        log.info("EC : {}", Security.getProviders("AlgorithmParameters.EC")[0]
                .getService("AlgorithmParameters", "EC").getAttribute("SupportedCurves"));
    }
}
