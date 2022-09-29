package seitchizserver.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeSet;

import common.NetMessage;
import common.dto.PhotoInfo;
import common.dto.UserInfo;

public class PhotosCat {

	private static final String PHOTOS_PATH = "data" + File.separator + "photos" + File.separator;

	private static final PhotosCat instance = new PhotosCat();

	private HashMap<String, ArrayList<Photo>> photos = new HashMap<String, ArrayList<Photo>>();
	private HashMap<String, String> idToUser = new HashMap<String, String>();
	private int currentId;

	private PhotosCat() {
		File photos_folder = new File(PHOTOS_PATH);
		photos_folder.mkdir();
		File[] userDirs = photos_folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		for (File userDir : userDirs) {
			photos.put(userDir.getName(), new ArrayList<Photo>());
			for (File file : userDir.listFiles()) {
				if (file.isFile()) {
					attachPhoto(userDir.getName(), file.getName());
					currentId++;
				}
			}
			try {
				File likesDir = new File(userDir.getCanonicalPath() + File.separator + "likes");
				likesDir.mkdir();
				for (File likesFile : likesDir.listFiles()) {
					Scanner likesSc = new Scanner(likesFile);
					while (likesSc.hasNextLine()) {
						String line = likesSc.nextLine();
						attachLike(likesFile.getName(), line);
					}
					likesSc.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static PhotosCat getInstance() {
		return instance;
	}

	public static void load() {
		// Does nothing, only loads class
	}

	public NetMessage post(String userid, PhotoInfo photoInfo) {
		addPhoto(Integer.toString(currentId), userid, photoInfo);
		System.out.println("User " + userid + " posted a photo with the id " + currentId);
		currentId++;
		return new NetMessage("successful", 0, null);
	}

	private void addPhoto(String photoid, String userid, PhotoInfo photoInfo) {
		File dir = new File(PHOTOS_PATH + userid);
		dir.mkdir();

		File photoFile = new File(photoInfo.getPath());
		photoFile.renameTo(new File(PHOTOS_PATH + userid + File.separator + photoid + "." + photoInfo.getExtension()));

		photos.computeIfAbsent(userid, s -> newAlbum(s));
		ArrayList<Photo> userAlbum = photos.get(userid);
		Photo newPhoto = new Photo(photoid, userid, photoInfo.getExtension());
		userAlbum.add(newPhoto);
		idToUser.put(photoid, userid);

		savePhotoDigest(newPhoto);
	}

	private void savePhotoDigest(Photo newPhoto) {
		// Create a DigestInputStream with the photo
		// photoPath/userid/photoid.ext
		File photoFile = new File(
				PHOTOS_PATH + newPhoto.getOwner() + File.separator + newPhoto.getId() + "." + newPhoto.getExtension());
		

		// Write the digest
		// photosPath/userid/photoid.sha1
		File photoHashFile = new File(PHOTOS_PATH + newPhoto.getOwner() + File.separator + "digests" + File.separator
				+ newPhoto.getId() + ".sha1");
		photoHashFile.getParentFile().mkdirs();
		FileOutputStream hashStream;
		try {
			hashStream = new FileOutputStream(photoHashFile);
			hashStream.write(getDigest(photoFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private ArrayList<Photo> newAlbum(String userid) {
		Path path = Paths.get(PHOTOS_PATH + userid);
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ArrayList<Photo>();
	}

	public void attachPhoto(String userid, String fileName) {
		String[] nameSplit = fileName.split("\\.");
		// Check if the hash store corresponds to the one generated with the photo
		File photoHashFile = new File(PHOTOS_PATH + userid + File.separator + "digests" + File.separator + nameSplit[0] + ".sha1");
		File photoFile = new File(PHOTOS_PATH + userid + File.separator + fileName);

		FileInputStream fis;
		try {
			fis = new FileInputStream(photoHashFile);
			byte[] storedDigest = new byte[20];
			while (fis.read(storedDigest) != -1) {};
			byte[] photoDigest = getDigest(photoFile);
			if (Arrays.equals(storedDigest, photoDigest)) {
				photos.get(userid).add(new Photo(nameSplit[0], userid, nameSplit[1]));
				idToUser.put(nameSplit[0], userid);
				System.out.println("The integrity of the photo file " + fileName + " was checked and it matches the hash");
			} else {
				System.out.println("The photo file " + fileName + " is corrupted and will not be loaded");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private byte[] getDigest(File file) {
		byte[] digest = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			try (InputStream is = new FileInputStream(file);
			     DigestInputStream dis = new DigestInputStream(is, md)) 
			{}
			digest = md.digest();
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return digest;
	}

	private void attachLike(String photoId, String userId) {
		for (Photo p : photos.get(idToUser.get(photoId))) {
			if (p.getId().equals(photoId)) {
				p.like(userId);
			}
		}
	}

	public NetMessage wall(UserInfo[] usersFollowing, Integer n) {
		TreeSet<PhotoInfo> result = new TreeSet<PhotoInfo>();

		for (UserInfo u : usersFollowing) {
			ArrayList<Photo> currentAlbum = photos.get(u.getId());
			if (currentAlbum != null) {
				for (int i = 0; i < currentAlbum.size(); i++) {
					Photo currentPhoto = currentAlbum.get(i);
					result.add(new PhotoInfo(
							PHOTOS_PATH + currentPhoto.getOwner() + File.separator + currentPhoto.getId() + "."
									+ currentPhoto.getExtension(),
							currentPhoto.getExtension(), currentPhoto.getLikes(), currentPhoto.getId()));
				}

			}
		}

		PhotoInfo[] resultArr = new PhotoInfo[n];
		int photosRead = 0;
		for (PhotoInfo p : result.descendingSet()) {
			if (!(photosRead < n)) {
				break;
			}
			resultArr[photosRead] = p;
			photosRead++;
		}

		return new NetMessage("successful", photosRead, resultArr);
	}

	public NetMessage like(String userLogged, String photoId) {
		String owner = idToUser.get(photoId);
		if (owner == null) {
			String[] errorStr = { "No photo with the id given" };
			return new NetMessage("error", 1, errorStr);
		}

		boolean succ = false;
		ArrayList<Photo> album = photos.get(owner);
		for (int i = 0; i < album.size(); i++) {
			if (album.get(i).getId().equals(photoId)) {
				succ = album.get(i).like(userLogged);
				File likesFile = new File(PHOTOS_PATH + File.separator + idToUser.get(photoId) + File.separator
						+ "likes" + File.separator + photoId);
				try {
					likesFile.createNewFile();
					FileWriter fw = new FileWriter(likesFile, true);
					fw.append(userLogged + "\n");
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
		}

		if (!succ) {
			String[] errorStr = { "You already liked this photo" };
			return new NetMessage("error", 1, errorStr);
		}
		System.out.println(userLogged + " liked photo " + photoId);
		return new NetMessage("successful", 0, null);
	}

}
