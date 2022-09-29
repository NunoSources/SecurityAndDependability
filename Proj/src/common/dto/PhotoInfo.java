package common.dto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Optional;

public class PhotoInfo implements Serializable, Comparable<PhotoInfo> {

	private static final long serialVersionUID = -3952635337095873008L;

	private String path;
	private String extension;
	private String id = "-1";
	private int likes;

	public PhotoInfo(String photoPath) {
		this.extension = getExtensionByStringHandling(photoPath).orElse("");
		this.path = photoPath;
	}

	public PhotoInfo(String photoPath, String extension, int likes, String id) {
		this.path = photoPath;
		this.extension = extension;
		this.id = id;
		this.likes = likes;
	}

	private Optional<String> getExtensionByStringHandling(String filename) {
		return Optional.ofNullable(filename).filter(f -> f.contains("."))
				.map(f -> f.substring(filename.lastIndexOf(".") + 1));
	}

	public String getExtension() {
		return extension;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public int compareTo(PhotoInfo o) {
		return new Integer(Integer.parseInt(this.id)).compareTo(new Integer(Integer.parseInt(o.id)));
	}

	public int getLikes() {
		return this.likes;
	}

	public String getPath() {
		return this.path;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(extension);
		out.writeObject(id);
		out.writeInt(likes);

		File f = new File(this.path);
		FileInputStream fin = new FileInputStream(f);
		InputStream input = new BufferedInputStream(fin);

		out.writeInt((int) f.length());
		out.flush();
		byte[] buffer = new byte[1024];
		int count;
		while ((count = input.read(buffer)) > 0) {
			out.write(buffer, 0, count);
		}

		out.flush();
		fin.close();
		input.close();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		this.extension = (String) in.readObject();
		this.id = (String) in.readObject();
		this.likes = in.readInt();
		this.path = id;

		File f = new File(id);
		FileOutputStream fout = new FileOutputStream(f);
		OutputStream output = new BufferedOutputStream(fout);

		int length = in.readInt();
		byte[] buffer = new byte[length];
		int count;
		while ((count = in.read(buffer)) > 0) {
			output.write(buffer, 0, count);
		}
		output.flush();

		fout.close();
		output.close();
	}

}
