package seitchizcliente;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;

import common.NetMessage;
import common.dto.MessageInfo;
import common.dto.PhotoInfo;
import common.dto.UserInfo;

public class Client {

	private static Scanner sc = new Scanner(System.in);
	private Socket socket = null;
	private static ObjectInputStream in;
	private static ObjectOutputStream out;
	private Certificate myCert;
	private Cipher privateCipher;
	private Cipher privateUnwrap;

	// create new client
	public Client(String truststore, String keystore, String keystorePassword) {
		// Set up SSL parameters
		System.setProperty("javax.net.ssl.trustStore", truststore);

		FileInputStream keyStoreFile = null;
		KeyStore keyStore = null;
		try {
			keyStoreFile = new FileInputStream(keystore);
		} catch (FileNotFoundException e1) {
			System.out.println("The keystore file does not exist");
			System.exit(-1);
		}
		try {
			keyStore = KeyStore.getInstance("JKS");

		} catch (KeyStoreException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		try {
			keyStore.load(keyStoreFile, keystorePassword.toCharArray());
			this.myCert = keyStore.getCertificate("rKey");
			this.privateCipher = Cipher.getInstance("RSA");
			this.privateCipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey("rKey", keystorePassword.toCharArray()));
			this.privateUnwrap = Cipher.getInstance("RSA");
			this.privateUnwrap.init(Cipher.UNWRAP_MODE, keyStore.getKey("rKey", keystorePassword.toCharArray()));

			keyStoreFile.close();
		} catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException | InvalidKeyException
				| UnrecoverableKeyException | NoSuchPaddingException e) {
			System.out.println("The keystore is malformed or the password is wrong");
			e.printStackTrace();
			System.exit(-1);
		}

	}

	// client connects to server
	public void connect(String address, int port) throws ClassNotFoundException {

		try {
			// creates new socket with user's address and port
			SocketFactory sf = SSLSocketFactory.getDefault();
			this.socket = (SSLSocket) sf.createSocket(address, port);
			in = new ObjectInputStream(this.socket.getInputStream());
			out = new ObjectOutputStream(this.socket.getOutputStream());
			// client sends request to server if the user is registered or not

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void disconnect() throws IOException {
		// closes channels
		in.close();
		out.close();
		sc.close();
		this.socket.close();
	}

	// client sends request to server if the user is registered or not
	// if server's reply is "registered" then the app shutsdown and server registers
	// new client
	// if server's reply is "not authenticated" then the app replies with an error
	// message and shuts down
	// if server's reply is "authenticated" then the app procceeds to show the menu
	// (printMenu)
	public NetMessage isRegistered(String clientID) throws IOException, ClassNotFoundException {
		String[] args = new String[2];
		args[0] = clientID;
		sendOperation("login", 1, args);
		NetMessage serverReply = getResponse();
		return serverReply;
	}

	public void register(Long nonce) {
		byte[] signature = null;
		try {
			signature = this.privateCipher.doFinal(longToBytes(nonce));
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		sendOperation("register", 3, new Object[] { nonce, signature, this.myCert });
	}

	private byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}

	public void authenticate(Long nonce) {
		byte[] signature = null;
		try {
			signature = this.privateCipher.doFinal(longToBytes(nonce));
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		sendOperation("register", 1, new Object[] { signature });
	}

	// if server's reply is "authenticated" then the app procceeds to show the menu
	public void printMenu() {

		System.out.println("follow <userID> – adiciona o cliente à lista de seguidores do utilizador.\n"
				+ "unfollow <userID> - remove o clienteda lista de seguidores do utilizador.\n"
				+ "viewfollowers – obtém a lista de seguidores do cliente.\n"
				+ "post <photo> – Faz post de uma fotografia.\n"
				+ "wall <nPhotos> - mostra as nPhotos mais recentes que se encontram nos perfis dos utilizadores seguidos.\n"
				+ "like <photoID> – coloca um like na fotografia.\n" + "newgroup <groupID> – cria um grupo privado.\n"
				+ "addu <userID> <groupID> – adiciona o utilizador como membro do grupo indicado.\n"
				+ "removeu <userID> <groupID> – remove o utilizador do grupo indicado.\n"
				+ "ginfo [groupID]  mostra uma lista dos grupos de que o cliente é dono, e uma lista dos grupos a que pertence.\n"
				+ "msg <groupID> <msg> – envia uma mensagem (msg) para o grupo.\n"
				+ "collect <groupID> – recebe todas as mensagens que tenham sigo enviadas para o grupo e que o cliente ainda não tenha recebido.\n"
				+ "history <groupID> – mostra o histórico das mensagens do grupo indicado que o cliente já leu anteriormente\n");

	}

	public void sendOperation(String operation, int n_args, Object[] args) {
		try {
			out.writeObject(operation);
			out.writeInt(n_args);
			for (int i = 0; i < n_args; i++) {
				out.writeObject(args[i]);
			}
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public NetMessage setUserName(String name) {
		String[] args = new String[1];
		args[0] = name;
		sendOperation("set username", 1, args);
		return getResponse();
	}

	public void follow(String userID) throws IOException, ClassNotFoundException {
		String[] args = new String[1];
		args[0] = userID;
		sendOperation("follow", 1, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
			return;
		}
	}

	private NetMessage getResponse() {
		NetMessage op = null;
		try {
			String command = (String) in.readObject();
			int nArguments = (Integer) in.readInt();
			Object[] arguments = new Object[nArguments];
			for (int i = 0; i < nArguments; i++) {
				arguments[i] = in.readObject();
			}
			op = new NetMessage(command, nArguments, arguments);
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return op;
	}

	public void unfollow(String userID) throws IOException, ClassNotFoundException {
		String[] args = new String[1];
		args[0] = userID;
		sendOperation("unfollow", 1, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
			return;
		}
	}

	public void viewfollowers() throws IOException, ClassNotFoundException {
		sendOperation("viewfollowers", 0, null);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
			return;
		}
		UserInfo ui;
		if (serverReply.getN() > 0) {
			for (int i = 0; i < serverReply.getN(); i++) {
				ui = (UserInfo) serverReply.getArgs()[i];
				System.out.println(ui.getId());
			}
		}
	}

	public void post(String photoPath) {
		Object[] args = new Object[1];

		PhotoInfo photo = new PhotoInfo(photoPath);
		args[0] = photo;
		sendOperation("post", 1, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful"))
			System.out.println("erro: " + serverReply.getArgs()[0]);
		return;
	}

	public void wall(Integer n) {
		Object[] args = new Object[1];
		args[0] = n;
		sendOperation("wall", 1, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
			return;
		}

		PhotoInfo currentPhoto;
		System.out.println("Likes:");
		for (int i = 0; i < serverReply.getN(); i++) {
			currentPhoto = (PhotoInfo) serverReply.getArgs()[i];
			System.out.println(currentPhoto.getId() + " - " + currentPhoto.getLikes());
			File photoFile = new File(currentPhoto.getPath());
			photoFile.renameTo(new File("user_received/" + currentPhoto.getId() + "." + currentPhoto.getExtension()));
		}
	}

	public void like(String photoId) {
		Object[] args = new Object[1];
		args[0] = photoId;
		sendOperation("like", 1, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
			return;
		}
	}

	public void newgroup(String groupId) {
		Object[] args = new Object[1];
		args[0] = groupId;
		sendOperation("newgroup", 1, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
			return;
		}
	}

	public void addu(String userId, String groupId) {
		Object[] args = new Object[2];
		args[0] = userId;
		args[1] = groupId;
		sendOperation("addu", 2, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
			return;
		}
	}

	public void removeu(String userId, String groupId) {
		Object[] args = new Object[2];
		args[0] = userId;
		args[1] = groupId;
		sendOperation("removeu", 2, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
			return;
		}
	}

	public void ginfo() {
		sendOperation("ginfo", 0, null);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
			return;
		}

		int nOwner = (Integer) serverReply.getArgs()[0];
		System.out.println("User owns:");
		for (int i = 1; i < nOwner + 1; i++) {
			System.out.println("\t" + serverReply.getArgs()[i]);
		}

		int nBelongs = (Integer) serverReply.getArgs()[nOwner + 1];
		System.out.println("User belongs to:");
		for (int i = nOwner + 2; i < nOwner + 1 + nBelongs + 1; i++) {
			System.out.println("\t" + serverReply.getArgs()[i]);
		}
	}

	public void ginfo(String groupId) {
		Object[] args = new Object[1];
		args[0] = groupId;
		sendOperation("ginfo", 1, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
			return;
		}

		System.out.println("Owner: " + serverReply.getArgs()[0]);
		System.out.println("Members:");
		for (int i = 1; i < serverReply.getN(); i++) {
			System.out.println("\t" + serverReply.getArgs()[i]);
		}
	}

	public void msg(String groupId, String msg) {
		// Get the current key
		sendOperation("gKey", 1, new Object[] { groupId });
		NetMessage serverReply = getResponse();
		byte[] encryptedMsg = encryptMessage(msg, (byte[]) serverReply.getArgs()[0]);
		Object[] args = new Object[2];
		args[0] = groupId;
		args[1] = encryptedMsg;
		sendOperation("msg", 2, args);
		serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
		}
	}

	private byte[] encryptMessage(String msg, byte[] key) {
		try {
			Key keySpec = this.privateUnwrap.unwrap(key, "AES", Cipher.SECRET_KEY);

			Cipher cw = Cipher.getInstance("AES");
			cw.init(Cipher.ENCRYPT_MODE, keySpec);
			byte[] result = cw.doFinal(msg.getBytes());
			return result;
		} catch (InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException
				| NoSuchPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void collect(String groupId) {
		Object[] args = new Object[1];
		args[0] = groupId;
		sendOperation("collect", 1, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
		}

		decryptMessages(Arrays.copyOf(serverReply.getArgs(), serverReply.getN(), MessageInfo[].class));

		System.out.println("Messages:");
		for (int i = 0; i < serverReply.getN(); i++) {
			MessageInfo msg = (MessageInfo) serverReply.getArgs()[i];
			System.out.println(msg.getOwner() + " - " + msg.getText());
		}
	}

	public void history(String groupId) {
		Object[] args = new Object[1];
		args[0] = groupId;
		sendOperation("history", 1, args);
		NetMessage serverReply = getResponse();
		if (!serverReply.getMessageStr().equals("successful")) {
			System.out.println("erro: " + serverReply.getArgs()[0]);
		}
		
		decryptMessages(Arrays.copyOf(serverReply.getArgs(), serverReply.getN(), MessageInfo[].class));

		System.out.println("Messages:");
		for (int i = 0; i < serverReply.getN(); i++) {
			MessageInfo msg = (MessageInfo) serverReply.getArgs()[i];
			System.out.println(msg.getOwner() + " - " + msg.getText());
		}
	}

	private void decryptMessages(MessageInfo[] messages) {
		try {
			Cipher c = Cipher.getInstance("AES");
			for (int i = 0; i < messages.length; i++) {
				MessageInfo cMessage = messages[i];
				Key unwrappedKey = privateUnwrap.unwrap(cMessage.getKey(), "AES", Cipher.SECRET_KEY);
				c.init(Cipher.DECRYPT_MODE, unwrappedKey);
				
				byte[] decoded = c.doFinal(cMessage.getEncryptedText());
				messages[i].setText(new String(decoded));
			}
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			e.printStackTrace();
		}
	}

}
