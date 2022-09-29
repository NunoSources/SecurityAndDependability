package seitchizserver.facade;

import seitchizserver.server.Server;

public class SeiTchizServer {

	private Server server;

	public SeiTchizServer() {

	}

	public void startServer(int port, String keystore, String keystorePassword) {
		this.server = new Server(port, keystore, keystorePassword);
		this.server.start();
	}

	public void stopServer() {
		this.server.stopServer();
	}

}
