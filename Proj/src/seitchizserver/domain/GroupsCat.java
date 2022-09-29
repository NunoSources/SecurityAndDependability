package seitchizserver.domain;

import java.util.HashMap;
import java.util.Scanner;

import common.NetMessage;
import common.dto.MessageInfo;
import seitchizserver.security.FileSecurityManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class GroupsCat {

	private static final String GROUPS_PATH = "data" + File.separator + "groups" + File.separator;

	private static GroupsCat instance = new GroupsCat();

	private HashMap<String, Group> groups = new HashMap<>();
	private HashMap<String, ArrayList<Group>> userToOwner = new HashMap<String, ArrayList<Group>>();
	private HashMap<String, ArrayList<Group>> userToMember = new HashMap<String, ArrayList<Group>>();

	private GroupsCat() {
		File groupsDir = new File(GROUPS_PATH);
		groupsDir.mkdir();
		for (File groupDir : groupsDir.listFiles()) {
			Group newGroup = persistGroup(groupDir);

			try {
				File inboxFile = groupDir.listFiles(file -> file.getName().equals("inbox"))[0];
				newGroup.persist_messages(newGroup, inboxFile, true);
			} catch (ArrayIndexOutOfBoundsException e) {
				/* Does nothing */}

			try {
				File historyFile = groupDir.listFiles((file, name) -> name.equals("history"))[0];
				newGroup.persist_messages(newGroup, historyFile, false);
			} catch (ArrayIndexOutOfBoundsException e) {
				/* Does nothing */
			}
			
			try {
				File keyFile = groupDir.listFiles((file, name) -> name.equals("lastKey"))[0];
				newGroup.persist_key(keyFile);
			} catch (ArrayIndexOutOfBoundsException e) {
				/* Does nothing */
			}

			groups.put(newGroup.getId(), newGroup);

			userToOwner.put(newGroup.getOwner(), new ArrayList<Group>());
			userToOwner.get(newGroup.getOwner()).add(newGroup);

			userToMember.put(newGroup.getOwner(), new ArrayList<Group>());
			for (String member : newGroup.getMembers()) {
				userToMember.get(member).add(newGroup);
			}
		}
	}

	private Group persistGroup(File groupDir) {
		File membersFileEncrypted = null;
		File membersFile = null;
		try {
			membersFileEncrypted = new File(groupDir.getCanonicalPath() + File.separator + "members");
			membersFile = new File(groupDir.getCanonicalPath() + File.separator + "members.dec");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		FileSecurityManager.getInstance().decryptFile(membersFileEncrypted, membersFile);
		Scanner membFileSc = null;
		try {
			membFileSc = new Scanner(membersFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String owner = membFileSc.nextLine();
		String groupId = groupDir.getName();
		Group newGroup = new Group(owner, groupId);
		userToOwner.putIfAbsent(owner, new ArrayList<Group>());
		userToOwner.get(owner).add(newGroup);

		String member;
		while (membFileSc.hasNextLine()) {
			member = membFileSc.nextLine();
			newGroup.addUser(member);
			userToMember.putIfAbsent(member, new ArrayList<Group>());
			userToMember.get(member).add(newGroup);
		}
		membFileSc.close();
		FileSecurityManager.getInstance().encryptFile(membersFile, membersFileEncrypted);
		
		return newGroup;
	}

	public ArrayList<String> getGroupsList() {
		ArrayList<String> groupsList = new ArrayList<>();
		for (String group : groups.keySet())
			groupsList.add(group);
		return groupsList;
	}

	public static GroupsCat getInstance() {
		return instance;
	}

	public boolean exist(String groupid) {
		return this.groups.containsKey(groupid);
	}

	public static void load() {
		// Does nothing, only makes the class load and initialize
	}

	public NetMessage newGroup(String owner, String groupId) {
		boolean exists = groups.containsKey(groupId);
		if (exists) {
			String[] errorStr = { "A group with the id given already exists" };
			return new NetMessage("error", 1, errorStr);
		}

		Group newGroup = new Group(owner, groupId);
		groups.put(groupId, newGroup);

		userToOwner.putIfAbsent(owner, new ArrayList<Group>());
		userToOwner.get(owner).add(newGroup);

		userToMember.putIfAbsent(owner, new ArrayList<Group>());
		userToMember.get(owner).add(newGroup);

		File groupFile = new File(GROUPS_PATH + File.separator + groupId);
		groupFile.mkdir();

		File members = new File(GROUPS_PATH + File.separator + groupId + File.separator + "members.dec");
		FileWriter fl;
		try {
			fl = new FileWriter(members);
			fl.write(owner + "\n");
			fl.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		File membersEncrypted = new File(GROUPS_PATH + File.separator + groupId + File.separator + "members");
		FileSecurityManager.getInstance().encryptFile(members, membersEncrypted);
		
		newGroup.newKey(GROUPS_PATH);

		System.out.println("Group " + groupId + " created by " + owner);
		return new NetMessage("successful", 0, null);
	}

	public NetMessage addUser(String userLogged, String userId, String groupId) {
		Group group = groups.get(groupId);
		if (group == null) {
			String[] errorStr = { "A group with the id given does not exits" };
			return new NetMessage("error", 1, errorStr);
		}

		if (!group.getOwner().equals(userLogged)) {
			String[] errorStr = { "You are not the owner of this group" };
			return new NetMessage("error", 1, errorStr);
		}

		boolean belongs = group.userBelongs(userId);
		if (belongs) {
			String[] errorStr = { "The user already belongs to the group" };
			return new NetMessage("error", 1, errorStr);
		}

		group.addUser(userId);
		group.newKey(GROUPS_PATH);

		userToMember.putIfAbsent(userId, new ArrayList<Group>());
		userToMember.get(userId).add(group);

		File membersEncrypted = new File(GROUPS_PATH + File.separator + groupId + File.separator + "members");
		File members = new File(GROUPS_PATH + File.separator + groupId + File.separator + "members.dec");
		FileSecurityManager.getInstance().decryptFile(membersEncrypted, members);
		
		try {
			FileWriter fl = new FileWriter(members, true);
			fl.write(userId + "\n");
			fl.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		FileSecurityManager.getInstance().encryptFile(members, membersEncrypted);

		System.out.println("User " + userId + " was added to " + groupId);
		return new NetMessage("successful", 0, null);
	}

	public NetMessage removeUser(String userLogged, String userId, String groupId) {
		Group group = groups.get(groupId);
		if (group == null) {
			String[] errorStr = { "A group with the id given does not exist" };
			return new NetMessage("error", 1, errorStr);
		}

		if (!group.getOwner().equals(userLogged)) {
			String[] errorStr = { "You are not the owner of this group" };
			return new NetMessage("error", 1, errorStr);
		}

		boolean belongs = group.userBelongs(userId);
		if (!belongs) {
			String[] errorStr = { "The user does not belong to the group" };
			return new NetMessage("error", 1, errorStr);
		}

		group.removeUser(userId);
		group.newKey(GROUPS_PATH);

		userToMember.get(userId).remove(group);

		deleteMemberRecord(groupId, userId);

		System.out.println("User " + userId + " was removed of " + groupId);
		return new NetMessage("successful", 0, null);
	}

	private void deleteMemberRecord(String groupId, String userId) {
		File fileEncrypted = new File(GROUPS_PATH + File.separator + groupId + File.separator + "members");
		File file = new File(GROUPS_PATH + File.separator + groupId + File.separator + "members.dec");
		FileSecurityManager.getInstance().decryptFile(fileEncrypted, file);
		
		File tempFile = new File("myTempFile.txt");

		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			writer = new BufferedWriter(new FileWriter(tempFile));
		} catch (IOException e) {
			e.printStackTrace();
		}

		String currentLine;
		try {
			while ((currentLine = reader.readLine()) != null) {
				// trim newline when comparing with lineToRemove
				String trimmedLine = currentLine.trim();
				if (trimmedLine.equals(userId))
					continue;
				writer.write(currentLine + "\n");
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

	public NetMessage groupInfo(String groupId) {
		Group group = groups.get(groupId);
		if (group == null) {
			String[] errorStr = { "A group with the id given does not exist" };
			return new NetMessage("error", 1, errorStr);
		}

		String[] groupMembers = group.getMembers();
		String[] groupInfo = new String[groupMembers.length + 1];
		groupInfo[0] = group.getOwner();
		for (int i = 0; i < groupMembers.length; i++) {
			groupInfo[i + 1] = groupMembers[i];
		}

		return new NetMessage("successful", groupInfo.length, groupInfo);
	}

	public NetMessage groupUserInfo(String userLogged) {
		ArrayList<Object> result = new ArrayList<Object>();

		ArrayList<Group> ownerIn = userToOwner.getOrDefault(userLogged, new ArrayList<Group>());
		result.add(ownerIn.size());
		for (int i = 0; i < ownerIn.size(); i++) {
			result.add(ownerIn.get(i).toString());
		}

		ArrayList<Group> memberIn = userToMember.getOrDefault(userLogged, new ArrayList<Group>());
		result.add(memberIn.size());
		for (int i = 0; i < memberIn.size(); i++) {
			result.add(memberIn.get(i).toString());
		}

		return new NetMessage("successful", result.size(), result.toArray());
	}

	public NetMessage addMessage(String userId, String groupId, byte[] bs) {
		Group group = groups.get(groupId);
		if (group == null) {
			String[] errorStr = { "A group with the id given does not exist" };
			return new NetMessage("error", 1, errorStr);
		}

		boolean belongs = group.userBelongs(userId);
		if (!belongs) {
			String[] errorStr = { "The user does not belong to the group" };
			return new NetMessage("error", 1, errorStr);
		}

		group.addMessage(userId, bs, GROUPS_PATH + File.separator + groupId + File.separator);

		System.out.println(userId + " added a message to the group " + group.getId());
		return new NetMessage("successful", 0, null);
	}
	
	public NetMessage getGroupKey(String groupId, String userId) {
		Group group = groups.get(groupId);
		if (group == null) {
			String[] errorStr = { "A group with the id given does not exist" };
			return new NetMessage("error", 1, errorStr);
		}
		
		boolean belongs = group.userBelongs(userId);
		if (!belongs) {
			String[] errorStr = { "The user does not belong to the group" };
			return new NetMessage("error", 1, errorStr);
		}
		byte[] key = group.getCurrentKey(userId);
		
		return new NetMessage("successful", 1, new Object[]{ key });
	}

	public NetMessage collect(String userId, String groupId) {
		Group group = groups.get(groupId);
		if (group == null) {
			String[] errorStr = { "A group with the id given does not exist" };
			return new NetMessage("error", 1, errorStr);
		}

		boolean belongs = group.userBelongs(userId);
		if (!belongs) {
			String[] errorStr = { "The user does not belong to the group" };
			return new NetMessage("error", 1, errorStr);
		}

		MessageInfo[] result = group.collect(userId, GROUPS_PATH + File.separator + groupId + File.separator);

		return new NetMessage("successful", result.length, result);
	}

	public NetMessage history(String userId, String groupId) {
		Group group = groups.get(groupId);
		if (group == null) {
			String[] errorStr = { "A group with the id given does not exist" };
			return new NetMessage("error", 1, errorStr);
		}

		boolean belongs = group.userBelongs(userId);
		if (!belongs) {
			String[] errorStr = { "The user does not belong to the group" };
			return new NetMessage("error", 1, errorStr);
		}

		MessageInfo[] result = group.history(userId);

		return new NetMessage("successful", result.length, result);
	}
	
}
