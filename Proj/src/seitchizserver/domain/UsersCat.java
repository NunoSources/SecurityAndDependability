package seitchizserver.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import common.NetMessage;
import common.dto.UserInfo;
import seitchizserver.security.FileSecurityManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

public class UsersCat {

	private static final String INFO_PATH = "data" + File.separator + "users";
	private static final String FOLLOWING_PATH = "data" + File.separator + "users_followers";
	private static final String CERTIFICATIONS_PATH = "data" + File.separator + "certifications";

	private static UsersCat instance = new UsersCat();

	private HashMap<String, User> users = new HashMap<>();
	private File records;
	private File followingList;

	private UsersCat() {
		records = new File(INFO_PATH);
		File recordsDecrypted = new File(INFO_PATH + ".dec");
		FileSecurityManager.getInstance().decryptFile(records, recordsDecrypted);

		followingList = new File(FOLLOWING_PATH);
		followingList.mkdir();
		
		// USERS READ
		Scanner usersReader;
		try {
			usersReader = new Scanner(recordsDecrypted);
			while (usersReader.hasNextLine()) {
				String line = usersReader.nextLine();
				String[] cred = line.split(":");
				User u = new User(cred[0], CERTIFICATIONS_PATH + File.separator + cred[1]);
				users.put(cred[0], u);
			}
			usersReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		FileSecurityManager.getInstance().encryptFile(recordsDecrypted, records);
		
		// FOLLOW LIST READ
		Scanner followReader;
		try {
			for (User user : users.values()) {
				File file = new File(followingList + File.separator + user.getId());
				File fileDecrypted = new File(followingList + File.separator + user.getId() + ".dec");
				FileSecurityManager.getInstance().decryptFile(file, fileDecrypted);
				if(file.exists()) {
					followReader = new Scanner(fileDecrypted);
					while (followReader.hasNextLine()) {
						String name = followReader.nextLine();
						User utf = users.get(name);
						user.addFollowing(utf);
						utf.addFollower(user);
	
					}
					FileSecurityManager.getInstance().encryptFile(fileDecrypted, file);
					followReader.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private User getUser(String username) {
		return this.users.get(username);
	}

	public static UsersCat getInstance() {
		return instance;
	}

	public boolean exist(String userid) {
		return this.users.containsKey(userid);
	}

	public void registerUser(String userid, Certificate publicKey) {
		try {
			User newUser = new User(userid, publicKey);
			this.users.put(userid, newUser);

			String uk_path = userid + ".crt";
			
			// Decrypt users file
			File encryptedFile = new File(INFO_PATH);
			File decryptedFile = new File(INFO_PATH + ".dec");
			FileSecurityManager.getInstance().decryptFile(encryptedFile, decryptedFile);
			
			// write in users user_id:uk_path
			FileWriter fileWriter = new FileWriter(INFO_PATH + ".dec", true);
			fileWriter.write(userid + ":" + uk_path + "\n");
			fileWriter.close();
			
			// Encrypts users file
			FileSecurityManager.getInstance().encryptFile(decryptedFile, encryptedFile);

			// writes the the public key on a file on uk_path
			File certDir = new File(CERTIFICATIONS_PATH);
			certDir.mkdirs();
			FileOutputStream certFile = new FileOutputStream(CERTIFICATIONS_PATH + File.separator + uk_path);
			byte[] certFileBytes = publicKey.getEncoded();
			certFile.write(certFileBytes);

			certFile.close();
		} catch (IOException | CertificateEncodingException e) {
			e.printStackTrace();
		}
		users.put(userid, new User(userid, publicKey));
		System.out.println("User " + userid + " registered.");
	}

	public static void load() {
		// Does nothing, only makes the class load and initialize
	}

	public NetMessage follow(String userLogged, String userToFollow) {
		User ul = getUser(userLogged);
		User utf = getUser(userToFollow);
		if (utf == null) {
			String[] errorMsg = { "user not found" };
			return new NetMessage("error", 1, errorMsg);
		}
		if (ul.equals(utf)) {
			String[] errorMsg = { "you cannot follow yourself" };
			return new NetMessage("error", 1, errorMsg);
		}
		UserInfo[] followersList = users.get(userLogged).getFollowing();
		boolean isFollower = Arrays.stream(followersList).map(u -> u.getId()).anyMatch(userToFollow::equals);
		if (isFollower) {
			String[] errorMsg = { "you already follow " + userToFollow };
			return new NetMessage("error", 1, errorMsg);
		}
		ul.addFollowing(utf);
		utf.addFollower(ul);
		registerFollowingRecord(ul, utf);
		return new NetMessage("successful", 0, null);
	}

	private void registerFollowingRecord(User ul, User utf) {
		File file = new File(followingList + File.separator + ul.getId());
		File fileDecrypted = new File(followingList + File.separator + ul.getId() + ".dec");
		
		FileSecurityManager.getInstance().decryptFile(file, fileDecrypted);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileDecrypted, true));
			writer.append(utf.getId());
			writer.append('\n');
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		FileSecurityManager.getInstance().encryptFile(fileDecrypted, file);
	}

	public NetMessage unfollow(String userLogged, String userToUnfollow) {
		User ul = getUser(userLogged);
		User utf = getUser(userToUnfollow);
		if (utf == null) {
			String[] errorMsg = { "user not found" };
			return new NetMessage("error", 1, errorMsg);
		}

		ul.removeFollowing(utf);
		utf.removeFollower(ul);
		deleteFollowingRecord(ul, utf);
		System.out.println("User " + userLogged + " unfollowed " + userToUnfollow);
		return new NetMessage("successful", 0, null);
	}

	private void deleteFollowingRecord(User ul, User utr) {
		File file = new File(followingList + File.separator + ul.getId() + ".dec");
		File fileEncrypted = new File(followingList + File.separator + ul.getId());

		File tempFile = new File("myTempFile.txt");
		
		FileSecurityManager.getInstance().decryptFile(fileEncrypted, file);
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			writer = new BufferedWriter(new FileWriter(tempFile));
		} catch (IOException e) {
			e.printStackTrace();
		}

		String lineToRemove = utr.getId();
		String currentLine;

		try {
			while ((currentLine = reader.readLine()) != null) {
				// trim newline when comparing with lineToRemove
				String trimmedLine = currentLine.trim();
				if (trimmedLine.equals(lineToRemove))
					continue;
				writer.write(currentLine + System.getProperty("line.separator"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			writer.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		tempFile.renameTo(file);
		FileSecurityManager.getInstance().encryptFile(file, fileEncrypted);
	}

	public NetMessage getFollowers(String userLogged) {
		UserInfo[] followers = users.get(userLogged).getFollowers();
		return new NetMessage("successful", followers.length, followers);
	}

	public NetMessage getFollowing(String userLogged) {
		UserInfo[] following = users.get(userLogged).getFollowing();
		return new NetMessage("successful", following.length, following);
	}

	public Certificate getUserCertificate(String user) {
		return users.get(user).getCertificate();
	}

}
