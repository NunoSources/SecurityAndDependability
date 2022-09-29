package common;

public class NetMessage {

	String messageStr;
	int nArguments;
	Object[] arguments;

	public NetMessage(String command, int nArguments, Object[] arguments) {
		this.messageStr = command;
		this.nArguments = nArguments;
		this.arguments = arguments;
	}

	public Object[] getArgs() {
		return arguments;
	}

	public String getMessageStr() {
		return this.messageStr;
	}

	public int getN() {
		return nArguments;
	}

}
