package sfe.command;

import java.util.function.Function;

public class ConsoleCommand {

	private final String commandName;
	private final Function<String[], Boolean> cmd;
	private final String usage;
	private final String alias;
	
	public ConsoleCommand(String commandName, Function<String[], Boolean> cmd, String usage) {
		this(commandName, null, cmd, usage);
	}
	
	public ConsoleCommand(String commandName, String alias, Function<String[], Boolean> cmd, String usage) {
		this.commandName = commandName;
		this.cmd = cmd;
		this.usage = usage;
		this.alias = alias;
	}
	
	public String getUsage() {
		return usage;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public String getCommandName() {
		return commandName;
	}
	
	public boolean hasAlias() {
		return alias != null;
	}
	
	public boolean runRaw(String[] rawArgs) {
		String[] args = new String[rawArgs.length - 1];
		System.arraycopy(rawArgs, 1, args, 0, args.length);
		
		return run(args);
	}
	
	public boolean run(String[] args) {
		return cmd.apply(args);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this)
			return true;
		
		if(obj instanceof String)
			return obj.equals(commandName) || (hasAlias() && obj.equals(alias));
		
		return false;
	}
}
