package server;

import java.io.Serializable;

import core.application.Application;
import core.command.Instruction;

public class Kick implements Instruction, Serializable {

	private static final long serialVersionUID = 1L;
	
	public void run(Application application, String[] args) {
		application.disconnect();
	}

}
