package seitchizserver.server;

import java.net.Socket;
import java.util.Random;

import common.NetMessage;
import common.dto.PhotoInfo;
import seitchizserver.domain.Session;

public class Connection extends Thread {

	Session session;
	Skeleton skel;

	public Connection(Socket inSoc) {
		skel = new Skeleton(inSoc);
	}

	public void run() {
		this.session = new Session();

		LoginHandler lh = new LoginHandler(skel, session);

		Random rand = new Random();
		Long nonce = rand.nextLong();

		boolean userExists = lh.start();
		if (userExists) {
			skel.sendResponse("successful", 1, new Long[] { nonce });
			if (lh.verifyChallenge(nonce))
				receiveOperations();
		} else {
			skel.sendResponse("not exist", 1, new Long[] { nonce });
			lh.registerUser(nonce);
		}

	}

	private void receiveOperations() {
		NetMessage operation = null;
		NetMessage response = null;
		while (true) {
			operation = skel.receiveOperation();
			if (operation == null) {
				skel.sendResponse("operation malformed", 0, null);
				break;
			}

			switch (operation.getMessageStr()) {
			case "follow":
				response = session.follow((String) operation.getArgs()[0]);
				break;
			case "unfollow":
				response = session.unfollow((String) operation.getArgs()[0]);
				break;
			case "viewfollowers":
				response = session.viewfollowers();
				break;
			case "post":
				response = session.post((PhotoInfo) operation.getArgs()[0]);
				break;
			case "wall":
				response = session.wall((Integer) operation.getArgs()[0]);
				break;
			case "like":
				response = session.like((String) operation.getArgs()[0]);
				break;
			case "newgroup":
				response = session.newgroup((String) operation.getArgs()[0]);
				break;
			case "addu":
				response = session.addu((String) operation.getArgs()[0], (String) operation.getArgs()[1]);
				break;
			case "removeu":
				response = session.removeu((String) operation.getArgs()[0], (String) operation.getArgs()[1]);
				break;
			case "ginfo":
				if (operation.getArgs().length > 0)
					response = session.ginfo((String) operation.getArgs()[0]);
				else
					response = session.ginfo();
				break;
			case "msg":
				response = session.msg((String) operation.getArgs()[0], (byte[]) operation.getArgs()[1]);	
				break;
			case "gKey":
				response = session.gKey((String) operation.getArgs()[0]);
				break;
			case "collect":
				response = session.collect((String) operation.getArgs()[0]);
				break;
			case "history":
				response = session.history((String) operation.getArgs()[0]);
				break;
			default:
				break;
			}
			skel.sendResponse(response);
		}
	}

}
