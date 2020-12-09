package fr.gouv.tac.jwt_keygen;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

public class JWTKeyGenCLI {

	public static void main(String[] args) throws NoSuchAlgorithmException {
		JWTKeyGenCLI runner = new JWTKeyGenCLI();
		runner.run();
	}

	public void run() throws NoSuchAlgorithmException {
		KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
		System.out.println("Private Key:");
		String privateString = Encoders.BASE64.encode(keyPair.getPrivate().getEncoded());
		System.out.println(privateString);
		System.out.println("Public Key:");
		String publicString = Encoders.BASE64.encode(keyPair.getPublic().getEncoded());
		System.out.println(publicString);
	}
}
