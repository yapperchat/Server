package server;

import java.io.Serializable;

import client.Client;
import client.ClientInstruction;

public class Kick implements ClientInstruction, Serializable {

	private static final long serialVersionUID = 1L;
	
	public void run(Client client, String[] args) {
		client.disconnect();
	}

}
