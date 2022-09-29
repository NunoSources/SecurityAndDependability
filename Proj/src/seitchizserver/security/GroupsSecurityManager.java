package seitchizserver.security;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class GroupsSecurityManager {
	
	private static final GroupsSecurityManager instance = new GroupsSecurityManager();
	private KeyStore keyStore;
	private String keyStorePassword;
	private String keyStorePath;
	
	private GroupsSecurityManager() {
		// Does nothing
	}

	public static GroupsSecurityManager getInstance() {
		return instance;
	}

	public void loadKeystore(KeyStore keyStore, String keyStorePassword, String keyStorePath) {
		this.keyStore = keyStore;
		this.keyStorePassword = keyStorePassword;
		this.keyStorePath = keyStorePath;
	}

	public void storeKey(Key key, String groupId, String userId, String keyId, Certificate userCert) {
		String keyAlias = "/group:" + groupId + "/user:" + userId + "/key:" + keyId;
		
		try {
			Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.WRAP_MODE, userCert.getPublicKey());
			
			byte[] wrappedKey = c.wrap(key);
			SecretKeySpec wrappedKeySpec = new SecretKeySpec(wrappedKey, "AES");
			
			this.keyStore.setKeyEntry(keyAlias, wrappedKeySpec, keyStorePassword.toCharArray(), null);
			
			FileOutputStream fos = new FileOutputStream(keyStorePath);
			keyStore.store(fos, keyStorePassword.toCharArray());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | KeyStoreException | CertificateException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] getKey(String groupId, String userId, String keyId) {
		String keyAlias = "/group:" + groupId + "/user:" + userId + "/key:" + keyId;
		try {
			SecretKeySpec resultKey = (SecretKeySpec) this.keyStore.getKey(keyAlias, keyStorePassword.toCharArray());
			return resultKey.getEncoded();
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

}
