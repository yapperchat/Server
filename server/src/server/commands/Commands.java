package server.commands;

import core.command.Command;
import core.command.CommandRegistrar;

public class Commands extends Command {

	public Commands(CommandRegistrar reg) {
		super(reg);
	}

	public void run(String[] args) {
		output("[COMMAND] COMMANDS:");
		for (int i = 1; i < commands.size(); i++) {
			output(commands.get(i).getTriggers()[0] + ": " + commands.get(i).getInfo());
		}
	}
	
	public String[] getTriggers() {
		return new String[] {
				"/help",
				"/commands",
				"/h",
		};
	}
	
	public String getInfo() {
		return "Lists all availible commands";
	}

}
