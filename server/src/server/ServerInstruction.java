package server;

import java.io.Serializable;

public interface ServerInstruction extends Serializable {
	public void run(ChatServer server, String[] args);
}
