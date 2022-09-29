package seitchizserver.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import common.NetMessage;

public class Skeleton {

	ObjectOutputStream outStream = null;
	ObjectInputStream inStream = null;

	public Skeleton(Socket inSoc) {
		outStream = null;
		inStream = null;
		try {
			outStream = new ObjectOutputStream(inSoc.getOutputStream());
			inStream = new ObjectInputStream(inSoc.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public NetMessage receiveOperation() {
		NetMessage op = null;
		try {
			String command = (String) inStream.readObject();
			int nArguments = (Integer) inStream.readInt();
			Object[] arguments = new Object[nArguments];
			for (int i = 0; i < nArguments; i++) {
				arguments[i] = inStream.readObject();
			}
			op = new NetMessage(command, nArguments, arguments);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Client left");
		}

		return op;
	}

	public void sendResponse(String response, int n_args, Object[] args) {
		try {
			outStream.writeObject(response);
			outStream.writeInt(n_args);
			for (int i = 0; i < n_args; i++) {
				outStream.writeObject(args[i]);
			}
			outStream.flush();
		} catch (IOException e) {
			System.err.println("Client left");
		}
	}

	public void sendResponse(NetMessage response) {
		try {
			outStream.writeObject(response.getMessageStr());
			outStream.writeInt(response.getN());
			for (int i = 0; i < response.getN(); i++) {
				outStream.writeObject(response.getArgs()[i]);
			}
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
