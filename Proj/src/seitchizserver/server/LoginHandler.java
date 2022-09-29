package seitchizserver.server;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import common.NetMessage;
import seitchizserver.domain.Session;
import seitchizserver.domain.UsersCat;

public class LoginHandler {

	private Skeleton skel;
	private Session session;
	private String userId;

	public LoginHandler(Skeleton skel, Session session) {
		this.skel = skel;
		this.session = session;
	}

	public boolean start() {
		NetMessage loginInfo = skel.receiveOperation();

		String[] info;
		info = Arrays.copyOf(loginInfo.getArgs(), loginInfo.getArgs().length, String[].class);

		this.userId = info[0];

		return this.session.userExists(this.userId);
	}

	void registerUser(Long nonce) {
		NetMessage challengeResp = skel.receiveOperation();
		Certificate clientCert;

		if (challengeResp == null) {
			skel.sendResponse("operation malformed", 0, null);
		} else {
			Long receivedNonce = (Long) challengeResp.getArgs()[0];
			byte[] signature = (byte[]) challengeResp.getArgs()[1];
			clientCert = (Certificate) challengeResp.getArgs()[2];
			
			// If the nonce received is not equal to the one sent, send error
			if(!receivedNonce.equals(nonce)) {
				String[] errorStr = { "register and authentication not successful" };
				skel.sendResponse(new NetMessage("error", 1, errorStr));
			}
			
			boolean sigVerify = verifySignature(clientCert, signature, receivedNonce);
			if(!sigVerify) {
				String[] errorStr = { "register and authentication not successful" };
				skel.sendResponse(new NetMessage("error", 1, errorStr));
				return;
			}
				
			session.registerUser(this.userId, clientCert);
			skel.sendResponse("successful", 0, null);
		}

	}

	boolean verifyChallenge(Long nonce) {
		NetMessage challengeResp = skel.receiveOperation();
		byte[] signature = (byte[]) challengeResp.getArgs()[0];
		Certificate clientCert = UsersCat.getInstance().getUserCertificate(userId);
		boolean sigVerify = verifySignature(clientCert, signature, nonce);
		if(!sigVerify) {
			String[] errorStr = { "authentication not successful" };
			skel.sendResponse(new NetMessage("error", 1, errorStr));
		}
		session.setUser(userId);
		return sigVerify;
	}
	
	private boolean verifySignature(Certificate clientCert, byte[] signature, Long nonce) {
		 try {
		    	// Decipher signature with public key received
		    	Cipher ck = Cipher.getInstance("RSA");
				ck.init(Cipher.DECRYPT_MODE, clientCert.getPublicKey());
				ByteBuffer decryptedNonce = ByteBuffer.allocate(Long.BYTES);
				decryptedNonce.put(ck.doFinal(signature));
				decryptedNonce.flip();//need flip 
				
				// Check if nonce received in signature match the one received
				if(nonce.longValue() != decryptedNonce.getLong()) {
					return false;
				}
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
				e.printStackTrace();
				return false;
			}
		return true;	
		
	}

}
