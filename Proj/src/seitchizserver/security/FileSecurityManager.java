package seitchizserver.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class FileSecurityManager {

	private static final String FILE_KEY_LOCATION = "data" + File.separator + "fileKey.aes";

	// Singleton
	private static final FileSecurityManager instance = new FileSecurityManager();

	private Key key;

	private FileSecurityManager() {

	}

	public static FileSecurityManager getInstance() {
		return instance;
	}

	public void loadKey(KeyStore keyStore, String password) {
		File fileKeyFile = new File(FILE_KEY_LOCATION);
		if (fileKeyFile.exists()) {
			key = readFileKey(fileKeyFile, keyStore, password);
		} else {
			key = createFileKey(fileKeyFile, keyStore, password);
		}
	}

	private SecretKey readFileKey(File fileKeyFile, KeyStore keyStore, String password) {
		try {
			// Read file
			byte[] encryptedKey = Files.readAllBytes(fileKeyFile.toPath());

			// Get private key to decrypt file
			final PrivateKey privateKey = (PrivateKey) keyStore.getKey("rKey", password.toCharArray());
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.UNWRAP_MODE, privateKey);

			// Decrypt file and get Key
			Key secretKey = cipher.unwrap(encryptedKey, "AES", Cipher.SECRET_KEY);
			return (SecretKey) secretKey;
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | IOException
				| InvalidKeyException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Key createFileKey(File fileKeyFile, KeyStore keyStore, String password) {
		try {
			// Create file and outputstream
			fileKeyFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(fileKeyFile);

			// Get Public key to encrypt file
			Certificate cert = keyStore.getCertificate("rKey");
			final PublicKey publicKey = cert.getPublicKey();
			Cipher cipher = Cipher.getInstance("RSA");

			// Generate random key
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
			Key secretKey = keyGen.generateKey();
			cipher.init(Cipher.WRAP_MODE, publicKey);

			// Write to file the key encrypted
			fos.write(cipher.wrap(secretKey));
			fos.close();

			return secretKey;
		} catch (KeyStoreException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| IllegalBlockSizeException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void encryptFile(File source, File destionation) {
		Cipher c = null;
		try {
			c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, key);
			source.createNewFile();
			FileInputStream fis = new FileInputStream(source);

			destionation.createNewFile();

			FileOutputStream fos = new FileOutputStream(destionation);

			CipherOutputStream cos = new CipherOutputStream(fos, c);
			byte[] b = new byte[16];
			int i = fis.read(b);
			while (i != -1) {
				cos.write(b, 0, i);
				i = fis.read(b);
			}
			source.delete();
			
			cos.close();
			fis.close();
			fos.close();
		} catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {

		}
	}

	public void decryptFile(File source, File destionation) {
		Cipher c = null;
		try {
			c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE, key);
			source.createNewFile();
			FileInputStream fis = new FileInputStream(source);

			destionation.createNewFile();
			FileOutputStream fos = new FileOutputStream(destionation);

			CipherInputStream cis = new CipherInputStream(fis, c);
			byte[] b = new byte[16];
			int i = cis.read(b);
			while (i != -1) {
				fos.write(b, 0, i);
				i = cis.read(b);
			}
			fos.flush();

			cis.close();
			fis.close();
			fos.close();
		} catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}

}
