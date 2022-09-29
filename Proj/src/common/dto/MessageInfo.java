package common.dto;

import java.io.Serializable;

public class MessageInfo implements Serializable {

	private static final long serialVersionUID = -5656953459821070929L;
	private String owner;
	private String text;
	private byte[] encryptedText;
	private byte[] key;

	public MessageInfo(String owner, byte[] encryptedText, byte[] key) {
		this.owner = owner;
		this.encryptedText = encryptedText;
		this.key = key;
	}

	public String getOwner() {
		return owner;
	}

	public String getText() {
		return text;
	}

	public byte[] getEncryptedText() {
		return this.encryptedText;
	}
	
	public byte[] getKey() {
		return key;
	}

	public void setText(String newText) {
		this.text = newText;
	}

}
