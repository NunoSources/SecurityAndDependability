package seitchizcliente;

import java.io.IOException;
import java.util.Scanner;

import common.NetMessage;

public class mainCliente {

	public static void main(String[] args) throws ClassNotFoundException, IOException {

		Scanner sc = new Scanner(System.in);

		// gets first argument from user's input
		String serverAddress = args[0];

		// splits address + port into 2 separate
		String[] addressPlusPort = serverAddress.split(":");
		String address = addressPlusPort[0];
		int port = Integer.parseInt(addressPlusPort[1]);

		// gets second and third argument from user's input (clientID and password
		// respectively)
		String truststore = args[1];
		String keystore = args[2];
		String keystorePassword = args[3];
		String clientID = args[4];

		// creates new client with user's input and proceeds to connect to server
		Client client = new Client(truststore, keystore, keystorePassword);
		client.connect(address, port);
		NetMessage serverReply = client.isRegistered(clientID);
		String serverReplyStr = serverReply.getMessageStr();
		Long nonce = (Long) serverReply.getArgs()[0];
		
		if (serverReplyStr.equals("successful")) {
			System.out.println("The user exists.");
			client.authenticate(nonce);
		} else if (serverReplyStr.equals("not exist")) {
			System.out.println("The user does not exist. Creating...");
			client.register(nonce);
			System.exit(0);
		} else {
			System.out.println("Received message malformed");
			System.exit(0);
		}
		
		client.printMenu();

		boolean exit = false;

		while (!exit) {
			// input = sc.next();
			System.out.println("Operation:");
			String input = sc.nextLine();
			String[] inputArray = input.split(" ");

			switch (inputArray[0]) {

			case "exit":
				System.exit(0);

			case "follow":
			case "f":
				try {
					client.follow(inputArray[1]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("User to follow is missing");
				}
				break;

			case "unfollow":
			case "u":
				try {
					client.unfollow(inputArray[1]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("User to unfollow is missing");
				}
				break;

			case "viewfollowers":
			case "v":
				client.viewfollowers();
				break;

			case "post":
			case "p":
				try {
					client.post(inputArray[1]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Photo is missing");
				}
				break;

			case "wall":
			case "w":
				try {
					client.wall(Integer.parseInt(inputArray[1]));
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Number of photos is missing");
				}
				break;

			case "like":
			case "l":
				try {
					client.like(inputArray[1]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Photo id is missing");
				}
				break;

			case "newgroup":
			case "n":
				try {
					client.newgroup(inputArray[1]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Group id is missing");
				}
				break;

			case "addu":
			case "a":
				try {
					client.addu(inputArray[1], inputArray[2]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("User id and/or group id are/is missing");
				}
				break;

			case "removeu":
			case "r":
				try {
					client.removeu(inputArray[1], inputArray[2]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("User id and/or group id are/is missing");
				}
				break;

			case "ginfo":
			case "g":
				if (inputArray.length <= 1) {
					client.ginfo();
				} else {
					client.ginfo(inputArray[1]);
				}
				break;

			case "msg":
			case "m":
				try {
					client.msg(inputArray[1], inputArray[2]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Group id and/or message are/is missing");
				}
				break;

			case "collect":
			case "c":
				try {
					client.collect(inputArray[1]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Group id is missing");
				}
				break;

			case "history":
			case "h":
				try {
					client.history(inputArray[1]);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Group id is missing");
				}
				break;

			default:
				System.out.println("Command not correct.");
			}
		}
		sc.close();
	}
}