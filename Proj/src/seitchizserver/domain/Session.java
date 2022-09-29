package seitchizserver.domain;

import java.security.cert.Certificate;

import common.NetMessage;
import common.dto.PhotoInfo;
import common.dto.UserInfo;

public class Session {

	private String userLogged;

	public Session() {
		// Does nothing
	}
	
	public void setUser(String userId) {
		this.userLogged = userId;
		System.out.println("User " + userId +  " authenticated");
	}
	
	public boolean userExists(String userid) {
		return UsersCat.getInstance().exist(userid);
	}
	
	public void registerUser(String userId, Certificate publicKey) {
		UsersCat.getInstance().registerUser(userId, publicKey);
	}

	public String getUserLogged() {
		return userLogged;
	}

	public NetMessage follow(String userToFollow) {
		NetMessage response = UsersCat.getInstance().follow(userLogged, userToFollow);
		if (response.getMessageStr().equals("successful")) {
			System.out.println("User " + userLogged + " followed " + userToFollow);
		}
		return response;
	}

	public NetMessage unfollow(String userToUnfollow) {
		NetMessage response = UsersCat.getInstance().unfollow(userLogged, userToUnfollow);
		return response;
	}

	public NetMessage viewfollowers() {
		NetMessage response = UsersCat.getInstance().getFollowers(userLogged);
		return response;
	}

	public NetMessage post(PhotoInfo photoInfo) {
		NetMessage response = PhotosCat.getInstance().post(userLogged, photoInfo);
		return response;
	}

	public NetMessage wall(Integer n) {
		UserInfo[] usersFollowing = (UserInfo[]) UsersCat.getInstance().getFollowing(userLogged).getArgs();
		NetMessage response = PhotosCat.getInstance().wall(usersFollowing, n);
		return response;
	}

	public NetMessage like(String photoId) {
		NetMessage response = PhotosCat.getInstance().like(userLogged, photoId);
		return response;
	}

	public NetMessage newgroup(String groupId) {
		NetMessage response = GroupsCat.getInstance().newGroup(userLogged, groupId);
		return response;
	}

	public NetMessage addu(String userId, String groupId) {
		boolean exist = UsersCat.getInstance().exist(userId);
		if (!exist) {
			String[] errorStr = { "A user with the id given does not exits" };
			return new NetMessage("error", 1, errorStr);
		}

		NetMessage response = GroupsCat.getInstance().addUser(userLogged, userId, groupId);
		return response;
	}

	public NetMessage removeu(String userId, String groupId) {
		boolean exist = UsersCat.getInstance().exist(userId);
		if (!exist) {
			String[] errorStr = { "A user with the id given does not exits" };
			return new NetMessage("error", 1, errorStr);
		}

		NetMessage response = GroupsCat.getInstance().removeUser(userLogged, userId, groupId);
		return response;
	}

	public NetMessage ginfo(String groupId) {
		NetMessage response = GroupsCat.getInstance().groupInfo(groupId);
		return response;
	}

	public NetMessage ginfo() {
		NetMessage response = GroupsCat.getInstance().groupUserInfo(userLogged);
		return response;
	}

	public NetMessage msg(String groupId, byte[] bs) {
		NetMessage response = GroupsCat.getInstance().addMessage(userLogged, groupId, bs);
		return response;
	}
	
	public NetMessage gKey(String groupId) {
		NetMessage response = GroupsCat.getInstance().getGroupKey(groupId, userLogged);
		return response;
	}

	public NetMessage collect(String groupId) {
		NetMessage response = GroupsCat.getInstance().collect(userLogged, groupId);
		return response;
	}

	public NetMessage history(String groupId) {
		NetMessage response = GroupsCat.getInstance().history(userLogged, groupId);
		return response;
	}

}
