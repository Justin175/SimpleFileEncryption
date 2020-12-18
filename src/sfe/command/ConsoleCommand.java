package sfe.command;

import java.util.function.Function;

public class ConsoleCommand {

	private final String commandName;
	private final Function<String[], Boolean> cmd;
	private final String usage;
	
	public ConsoleCommand(String commandName, Function<String[], Boolean> cmd, String usage) {
		this.commandName = commandName;
		this.cmd = cmd;
		this.usage = usage;
	}
	
	public String getUsage() {
		return usage;
	}
	
	public String getCommandName() {
		return commandName;
	}
	
	public boolean runRaw(String[] rawArgs) {
		String[] args = new String[rawArgs.length - 1];
		System.arraycopy(rawArgs, 1, args, 0, args.length);
		
		return run(args);
	}
	
	public boolean run(String[] args) {
		return cmd.apply(args);
	}
}
