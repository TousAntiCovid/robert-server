package fr.gouv.stopc.robertserver.utils;

import lombok.extern.slf4j.Slf4j;
import org.bson.internal.Base64;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Scanner;

@Slf4j
public class Ecdh {

    public String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
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
        Ecdh ec = new Ecdh();

        Scanner sc = new Scanner(System.in);

        // Generate ephemeral ECDH keypair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair kp = kpg.generateKeyPair();
        byte[] ourPk = kp.getPublic().getEncoded();

        log.info("RO = {}", -Math.log(1.0 - 0.1) / 15.0);
        // Display our public key
        //console.printf("Public Key: %s%n", printHexBinary(ourPk));
        log.info("Public Key: " + Base64.encode(ourPk));
        log.info("PRIVATE Key: " + Base64.encode(kp.getPrivate().getEncoded()));

        log.info("EC : {}", Security.getProviders("AlgorithmParameters.EC")[0]
                .getService("AlgorithmParameters", "EC").getAttribute("SupportedCurves"));


//    // Read other's public key:
//    System.out.println("Saisissez une chaîne : ");
//    String str = sc.nextLine();
//    sc.close();
//
//    byte[] mobilePk = ec.decodeHexString(str);
//
//    System.out.println("Public Key Exter2 : " + ec.encodeHexString(mobilePk));
//
//
//    KeyFactory kf = KeyFactory.getInstance("EC");
//    X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(mobilePk);
//    PublicKey otherPublicKey = kf.generatePublic(pkSpec);
//
//    // Perform key agreement
//    KeyAgreement ka = KeyAgreement.getInstance("ECDH");
//    ka.init(kp.getPrivate());
//    ka.doPhase(otherPublicKey, true);
//
//    // Read shared secret
//    byte[] sharedSecret = ka.generateSecret();
//    System.out.println("Shared secret: " + ec.encodeHexString(sharedSecret));
//
//
//    // Derive a key from the shared secret and both public keys
//    MessageDigest hash = MessageDigest.getInstance("SHA-256");
//    hash.update(sharedSecret);
//    // Simple deterministic ordering
//    List<ByteBuffer> keys = Arrays.asList(ByteBuffer.wrap(ourPk), ByteBuffer.wrap(mobilePk));
//    Collections.sort(keys);
//    hash.update(keys.get(0));
//    hash.update(keys.get(1));
//
//    byte[] derivedKey = hash.digest();
//    System.out.println("Derived Key from Shared secret: " + ec.encodeHexString(derivedKey));
    }


}
