package seitchizserver.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import seitchizserver.domain.GroupsCat;
import seitchizserver.domain.PhotosCat;
import seitchizserver.domain.UsersCat;
import seitchizserver.security.FileSecurityManager;
import seitchizserver.security.GroupsSecurityManager;

public class Server extends Thread {

	private static final String DATA_PATH = "data";
	
	private int port;
	private KeyStore rKeyStore;
	private ServerSocket sSoc = null;

	public Server(int port, String keystore, String keystorePassword) {
		this.port = port;
		try {
			// Set up SSL parameters
			System.setProperty("javax.net.ssl.keyStore", keystore);
			System.setProperty("javax.net.ssl.keyStoreType","JCEKS");
			System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
			
			FileInputStream keyStoreFis = new FileInputStream(keystore);
		    rKeyStore = KeyStore.getInstance("JCEKS");
		    rKeyStore.load(keyStoreFis, keystorePassword.toCharArray());
		    keyStoreFis.close();
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		File dataFile = new File(DATA_PATH);
		dataFile.mkdir();
		
		FileSecurityManager.getInstance().loadKey(rKeyStore, keystorePassword);
		GroupsSecurityManager.getInstance().loadKeystore(rKeyStore, keystorePassword, keystore);
		
		GroupsCat.load();
		UsersCat.load();
		PhotosCat.load();
	}

	public void run() {
		try {
			ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
			sSoc = (SSLServerSocket) ssf.createServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		while (true) {
			try {
				Socket inSoc = sSoc.accept();
				Connection newServerThread = new Connection(inSoc);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public void stopServer() {
		try {
			sSoc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
