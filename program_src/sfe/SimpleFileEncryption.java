package sfe;

import java.util.Arrays;
import java.util.HashMap;

import sfe.command.ConsoleCommand;
import sfe.commands.CommandCreatePasswordFile;
import sfe.commands.CommandDecrypt;
import sfe.commands.CommandEncrypt;

public class SimpleFileEncryption {

	private static final HashMap<String, ConsoleCommand> COMMANDS = new HashMap<>();
	private static final HashMap<String, ConsoleCommand> COMMANDS_PRINT = new HashMap<>();
	private static String[] errorString;
	
	public static void main(String... args) {
		loadCommands();
		run(args.length == 0 ? new String[] { "help" } : args);
	}
	
	public static void run(String... args) {
		if(args.length == 0) {
			printCommands();
		}
		else {
			ConsoleCommand cmd = COMMANDS.get(args[0].toLowerCase());
			
			if(cmd == null) {
				System.err.println("'" + args[0] + "' is a unknown Command. Use 'help' to get a list of all Commands.");
				return;
			}
			
			if(!cmd.runRaw(args)) {
				System.err.println("An error has occurred.");
				System.err.println("------------------------------------------------------------------------------------------");
				System.err.println("Command: ");
				System.err.println(" > " + cmd.getCommandName() + (cmd.hasAlias() ? " (" + cmd.getAlias() + ")" : ""));
				System.err.println(" >     " + cmd.getUsage());
				System.err.println("Given Command:");
				System.err.println(" > " + Arrays.toString(args));
				System.err.println("ERROR-Message: ");
				for(String a : errorString)
					System.err.println(" > " + a);
				System.err.println("------------------------------------------------------------------------------------------");
			}
		}
	}
	
	public static void setErrorString(String... errorString) {
		SimpleFileEncryption.errorString = errorString;
	}
	
	public static String[] getErrorString() {
		return errorString;
	}
	
	private static void loadCommands() {
		addCommand(new ConsoleCommand("help", (x) -> { printCommands(); return true; }, ""));
		
		addCommand(new CommandEncrypt());
		addCommand(new CommandDecrypt());
		addCommand(new CommandCreatePasswordFile());
	}
	
	private static void addCommand(ConsoleCommand cmd) {
		COMMANDS_PRINT.put(cmd.getCommandName().toLowerCase(), cmd);
		COMMANDS.put(cmd.getCommandName().toLowerCase(), cmd);
		
		if(cmd.hasAlias())
			COMMANDS.put(cmd.getAlias().toLowerCase(), cmd);
	}
	
	private static void printCommands() {
		System.out.println("SFE-Commands:");
		for(ConsoleCommand a : COMMANDS_PRINT.values()) //Print commands
			System.out.println("  " + a.getCommandName() + " " + a.getUsage() + (a.hasAlias() ? "\n    Alias: " + a.getAlias() : ""));
		
		System.out.println();
		System.out.println("Use: '[Command] --help' for detailed Informations.");
		System.out.println();
	}
	
	public static void printCommandInfo(ConsoleCommand cmd, String[] allowedFlags, String[] flagsDescription) {
		printCommandInfo(cmd.getCommandName(), cmd.getAlias(), cmd.getUsage(), allowedFlags, flagsDescription);
	}
	
	public static void printCommandInfo(String cmdName, String alias, String usage, String[] allowedFlags, String[] flagsDescription) {
		int maxLength = 0;
		
		for(String a : allowedFlags)
			if(a.length() > maxLength)
				maxLength = a.length();
		
		printCommandInfo(cmdName, alias, usage, allowedFlags, maxLength, flagsDescription);
	}
	
	public static void printCommandInfo(String cmdName, String alias, String usage, String[] allowedFlags, int maxFlagsLength, String[] flagsDescription) {
		System.out.println("Help for '" + cmdName  + "'");
		System.out.println("Name:   " + cmdName);
		System.out.println("Alias:  " + (alias == null ? "-" : alias));
		System.out.println("Usage:  " + usage);
		System.out.println("Allowed Flags:");
		
		for(int i = 0; i < allowedFlags.length; i++) {
			System.out.printf("  %-" + (maxFlagsLength + 3) + "s: %s\n", allowedFlags[i], flagsDescription[i]);
		}
	}
}
