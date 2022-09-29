package seitchizserver.domain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

public class Message {

	private byte[] text;
	private String owner;
	private String id;
	private ArrayList<String> toReceive;
	private ArrayList<String> received = new ArrayList<String>();
	private String keyId;

	@SuppressWarnings("unchecked")
	public Message(byte[] text, String owner, int id, ArrayList<String> toReceive, String keyId) {
		this.text = text;
		this.owner = owner;
		this.id = Integer.toString(id);
		this.toReceive = (ArrayList<String>) toReceive.clone();
		this.keyId = keyId;
	}

	public Message(byte[] text, String owner, String id, ArrayList<String> toReceive, ArrayList<String> received, String keyId) {
		this.text = text;
		this.owner = owner;
		this.id = id;
		this.toReceive = toReceive;
		this.received = received;
		this.keyId = keyId;
	}

	public byte[] getText() {
		return text;
	}

	public String getOwner() {
		return owner;
	}

	public String getId() {
		return id;
	}
	
	public String getKeyId() {
		return keyId;
	}

	public boolean userReceived(String userId) {
		if (toReceive.contains(userId)) {
			toReceive.remove(userId);
			received.add(userId);
			return false;
		}
		return true;
	}

	public boolean allReceived() {
		return toReceive.isEmpty();
	}

	public boolean wasRead(String userId) {
		return received.contains(userId);
	}

	public void removeToReceive(String userId) {
		toReceive.remove(userId);
	}

	public void addRegister(File file) {
		FileWriter fl;
		try {
			fl = new FileWriter(file, true);
			fl.write(this.id + ":" + this.owner + ":" + this.keyId + ":");

			fl.write(this.toReceive.size() + ":");
			for (String elem : toReceive) {
				fl.write(elem + ":");
			}

			fl.write(this.received.size() + ":");
			for (String elem : received) {
				fl.write(elem + ":");
			}

			fl.write(Base64.getEncoder().encodeToString((this.text)) + "\n");

			fl.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void removeRegister(File file) {
		File tempFile = new File("myTempFile.txt");

		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			writer = new BufferedWriter(new FileWriter(tempFile));
		} catch (IOException e) {
			e.printStackTrace();
		}

		String lineToRemove = this.id;
		String currentLine;

		try {
			while ((currentLine = reader.readLine()) != null) {
				// trim newline when comparing with lineToRemove
				String trimmedLine = currentLine.trim();
				String id = trimmedLine.split(":")[0];
				if (id.equals(lineToRemove))
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

	}

}
