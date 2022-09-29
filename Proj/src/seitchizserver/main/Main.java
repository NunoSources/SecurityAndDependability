package seitchizserver.main;

import java.util.Scanner;

import seitchizserver.facade.SeiTchizServer;

public class Main {

	public static void main(String[] args) {
		System.out.println("Server opening on port " + args[0]);
		SeiTchizServer server = new SeiTchizServer();
		// 0 - port
		// 1 - keystore
		// 2 - keystore-password
		server.startServer(Integer.parseInt(args[0]), args[1], args[2]);
		Scanner sc = new Scanner(System.in);
		if (sc.nextLine().equals("exit")) {
			server.stopServer();
			System.exit(0);
		}
		sc.close();

	}

}
