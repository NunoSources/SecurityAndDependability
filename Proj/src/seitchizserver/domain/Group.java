package seitchizserver.domain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import common.dto.MessageInfo;
import seitchizserver.security.GroupsSecurityManager;

public class Group {

	private String id;
	private String owner;
	private ArrayList<String> members = new ArrayList<String>();
	private ArrayList<Message> inbox = new ArrayList<Message>();
	private ArrayList<Message> history = new ArrayList<Message>();

	private int currentMessageId = 0;
	private int currentKeyId = -1;

	public Group(String owner, String id) {
		this.id = id;
		this.owner = owner;
		members.add(owner);
	}

	public ArrayList<Message> getMessages() {
		return inbox;
	}

	public String getId() {
		return id;
	}

	public String getOwner() {
		return owner;
	}

	public boolean userBelongs(String userId) {
		return members.contains(userId);
	}

	public void addUser(String userId) {
		members.add(userId);
	}

	public void removeUser(String userId) {
		members.remove(userId);
		inbox.forEach(message -> message.removeToReceive(userId));
	}

	public String[] getMembers() {
		return members.toArray(new String[0]);
	}

	public String toString() {
		return this.id;
	}

	public void addMessage(String userId, byte[] bs, String regPath) {
		Message newMsg = new Message(bs, userId, currentMessageId, members, Integer.toString(currentKeyId));
		currentMessageId++;
		inbox.add(newMsg);

		addMsgRegister(newMsg, regPath);
	}

	@SuppressWarnings("unchecked")
	public MessageInfo[] collect(String userId, String pathGroup) {
		GroupsSecurityManager gsm = GroupsSecurityManager.getInstance();
		ArrayList<MessageInfo> result = new ArrayList<MessageInfo>();
		ArrayList<Message> inboxTemp = (ArrayList<Message>) inbox.clone();
		int inboxSize = this.inbox.size();
		for (int i = 0; i < inboxSize; i++) {
			Message msg = inbox.get(i);
			if (!msg.userReceived(userId)) {
				result.add(new MessageInfo(msg.getOwner(), msg.getText(), gsm.getKey(id, userId, msg.getKeyId())));
				if (msg.allReceived()) {
					history.add(msg);
					inboxTemp.remove(msg);
					moveMsgRegister(msg, pathGroup);
				}
			}
		}
		inbox = inboxTemp;
		return result.toArray(new MessageInfo[0]);
	}

	public MessageInfo[] history(String userId) {
		GroupsSecurityManager gsm = GroupsSecurityManager.getInstance();
		ArrayList<MessageInfo> result = new ArrayList<MessageInfo>();
		for (int i = 0; i < this.history.size(); i++) {
			Message msg = history.get(i);
			if (msg.wasRead(userId)) {
				result.add(new MessageInfo(msg.getOwner(), msg.getText(), gsm.getKey(id, userId, msg.getKeyId())));
			}
		}
		return result.toArray(new MessageInfo[0]);
	}

	public void persist_messages(Group newGroup, File file, boolean isInbox) {
		Scanner sc = null;
		try {
			sc = new Scanner(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String line;
		int index = 0;
		while (sc.hasNextLine()) {
			line = sc.nextLine();
			String vals[] = line.split(":");

			String id = vals[index];
			index++;

			String owner = vals[index];
			index++;
			String keyId = vals[index];
			index++;
			int nToReceive = Integer.parseInt(vals[index]);
			index++;

			ArrayList<String> toReceive = new ArrayList<String>();
			int lastPos = index + nToReceive;
			while (index < lastPos) {
				toReceive.add(vals[index]);
				index++;
			}

			int nReceived = Integer.parseInt(vals[index]);
			index++;

			ArrayList<String> received = new ArrayList<String>();
			lastPos = index + nReceived;
			while (index < lastPos) {
				received.add(vals[index]);
				index++;
			}

			String text = vals[index];
			
			Message newMessage = new Message(Base64.getDecoder().decode(text), owner, id, toReceive, received, keyId);

			if (isInbox)
				inbox.add(newMessage);
			else
				history.add(newMessage);
		}

	}
	
	public void persist_key(File keyFile) {
		Scanner sc = null;
		try {
			sc = new Scanner(keyFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		this.currentKeyId = sc.nextInt();
		sc.close();
	}

	public void addMsgRegister(Message msg, String pathGroup) {
		File file = null;
		file = new File(pathGroup + "inbox");
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		msg.addRegister(file);
	}

	public void moveMsgRegister(Message msg, String pathGroup) {
		File histFile = new File(pathGroup + "history");
		File inboxFile = new File(pathGroup + "inbox");
		try {
			histFile.createNewFile();
			inboxFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		msg.removeRegister(inboxFile);
		msg.addRegister(histFile);
	}

	public void newKey(String groupsPath) {
		currentKeyId++;
		Key newKey = generateKey();
		
		for(String user : members) {
			Certificate userCert = UsersCat.getInstance().getUserCertificate(user);
			GroupsSecurityManager.getInstance().storeKey(newKey, id, user, Integer.toString(currentKeyId), userCert);
		}
		
		FileWriter fw = null;
		try {
			File file = new File(groupsPath + this.id +  File.separator + "lastKey");
			fw = new FileWriter(file);
			fw.write(Integer.toString(this.currentKeyId));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	private Key generateKey() {
		KeyGenerator kg = null;
		try {
			kg = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	    kg.init(128);
	    SecretKey key = kg.generateKey();
	    return key;
	}

	public byte[] getCurrentKey(String userId) {
		return GroupsSecurityManager.getInstance().getKey(id, userId, Integer.toString(currentKeyId));
	}

}
