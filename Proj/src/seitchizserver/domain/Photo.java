package seitchizserver.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Photo {

	private String id;
	private String owner;
	private String extension;
	private ArrayList<Like> likes;

	public Photo(String id, String userid, String extension) {
		this.id = id;
		this.extension = extension;
		this.owner = userid;
		this.likes = new ArrayList<Like>();
	}

	public int getLikes() {
		return likes.size();
	}

	public String getExtension() {
		return extension;
	}

	public String getId() {
		return id;
	}

	public byte[] getBytes(String photosPath) {
		byte[] result = null;
		try {
			result = Files.readAllBytes(Paths.get(photosPath + id + "." + extension));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public String getOwner() {
		return this.owner;
	}

	public boolean like(String owner) {
		for (int i = 0; i < likes.size(); i++) {
			if (likes.get(i).getOwner().equals(owner)) {
				return false;
			}
		}
		likes.add(new Like(owner));
		return true;
	}

}
