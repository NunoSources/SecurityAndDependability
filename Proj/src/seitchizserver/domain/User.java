package seitchizserver.domain;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import common.dto.UserInfo;

public class User {

	private String id;
	private Certificate publicCert;
	private ArrayList<User> usersFollowing = new ArrayList<User>();
	private ArrayList<User> userFollowers = new ArrayList<User>();

	public User(String id, Certificate publicKey) {
		this.id = id;
		this.publicCert = publicKey;
	}

	public User(String id, String certPath) {
		try {
			CertificateFactory fact = CertificateFactory.getInstance("X.509");
			FileInputStream is = new FileInputStream (certPath);
			this.id = id;
			this.publicCert = (X509Certificate) fact.generateCertificate(is);
		} catch (CertificateException | FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<User> getUsersFollowing(String id) {
		return usersFollowing;
	}

	public ArrayList<User> getUsersFollowed(String id) {
		return userFollowers;
	}

	public String getId() {
		return id;
	}

	public void addFollowing(User user) {
		usersFollowing.add(user);
	}

	public void addFollower(User user) {
		userFollowers.add(user);
	}

	public void removeFollowing(User user) {
		usersFollowing.remove(user);
	}

	public void removeFollower(User user) {
		userFollowers.remove(user);
	}

	public UserInfo[] getFollowers() {
		UserInfo[] result = new UserInfo[userFollowers.size()];
		User u;
		for (int i = 0; i < userFollowers.size(); i++) {
			u = userFollowers.get(i);
			result[i] = new UserInfo(u.getId());
		}
		return result;
	}

	public UserInfo[] getFollowing() {
		UserInfo[] result = new UserInfo[usersFollowing.size()];
		User u;
		for (int i = 0; i < usersFollowing.size(); i++) {
			u = usersFollowing.get(i);
			result[i] = new UserInfo(u.getId());
		}
		return result;
	}

	public Certificate getCertificate() {
		return publicCert;
	}

}
