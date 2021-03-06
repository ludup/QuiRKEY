package com.hypersocket.quirkey.tests;

import java.net.URL;
import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hypersocket.crypto.ECCryptoProvider;
import com.hypersocket.crypto.ECCryptoProviderFactory;
import com.hypersocket.quirkey.client.ClientAuthenticationTransaction;
import com.hypersocket.quirkey.client.ClientRegistrationTransaction;
import com.hypersocket.quirkey.server.ServerAuthenticationTransaction;
import com.hypersocket.quirkey.server.ServerRegistrationTransaction;

public class TestClientServerInteraction {

	private static final String ID = "Mobile ID";
	private static final String MOBILE_NAME = "Lee's Mobile";
	private static final String USER_NAME = "Lee";

	@BeforeClass
	public static void setupJCEProvider() {
		Security.insertProviderAt(new BouncyCastleProvider(), 0);
	}

	@Test
	public void testClientServerRegistration() throws Exception {

		ECCryptoProvider provider = ECCryptoProviderFactory
				.createInstance("secp256r1");

		KeyPair serverKey = provider.generateKeyPair();
		KeyPair clientKey = provider.generateKeyPair();

		ServerRegistrationTransaction server = new ServerRegistrationTransaction(
				USER_NAME, new URL("http://localhost"), serverKey, "secp256r1",
				false, 0);

		String registrationInfo = server.generateRegistrationInfo();

		ClientRegistrationTransaction client = new ClientRegistrationTransaction(
				clientKey, registrationInfo, "secp256r1");

		String clientRequest = client.generateRegistrationRequest(ID,
				MOBILE_NAME);

		boolean registered = true;

		String serverResponse;
		if (registered) {
			serverResponse = server.processRequestionRequest(clientRequest);
			clientRequest = client.verifyRegistrationResponse(serverResponse);
			Assert.assertTrue(server
					.processRegistrationConfirmation(clientRequest));
		} else {
			serverResponse = server.generateErrorMessage(100,
					"Client already registered");
		}
	}

	@Test
	public void testClientServerAuthentication() throws Exception {

		ECCryptoProvider provider = ECCryptoProviderFactory
				.createInstance("secp256r1");

		KeyPair serverKey = provider.generateKeyPair();
		KeyPair clientKey = provider.generateKeyPair();

		ServerRegistrationTransaction registrationServer = new ServerRegistrationTransaction(
				USER_NAME, new URL("http://localhost"), serverKey, "secp256r1", false, 0);

		String registrationInfo = registrationServer.generateRegistrationInfo();

		ClientRegistrationTransaction registrationClient = new ClientRegistrationTransaction(
				clientKey, registrationInfo, "secp256r1");

		String registrationClientRequest = registrationClient
				.generateRegistrationRequest(ID, MOBILE_NAME);
		
		String registrationServerResponse = registrationServer
				.processRequestionRequest(registrationClientRequest);
		
		registrationClientRequest = registrationClient
				.verifyRegistrationResponse(registrationServerResponse);
		
		if (registrationServer.processRegistrationConfirmation(registrationClientRequest)) {
			ServerAuthenticationTransaction authenticationServer = new ServerAuthenticationTransaction(
					new URL("http://localhost"), "secp256r1");

			String authenticationInfo = authenticationServer
					.generateAuthenticationInfo();

			ClientAuthenticationTransaction authenticationClient = new ClientAuthenticationTransaction(
					authenticationInfo, "secp256r1");

			String authenticationClientRequest = authenticationClient
					.generateAuthenticationRequest(ID, MOBILE_NAME,
							registrationClient.getServerPublicKey(),
							registrationClient.getUsername(), clientKey
									.getPrivate().getEncoded(), clientKey
									.getPublic().getEncoded());

			String authenticationServerResponse = authenticationServer
					.processAuthenticationRequest(authenticationClientRequest, serverKey
							.getPrivate().getEncoded(), serverKey.getPublic()
							.getEncoded(), USER_NAME, MOBILE_NAME, clientKey
							.getPublic().getEncoded());

			Assert.assertTrue(authenticationClient.verifyAuthenticationResponse(authenticationServerResponse, registrationClient.getServerPublicKey()));
		}
	}
}
