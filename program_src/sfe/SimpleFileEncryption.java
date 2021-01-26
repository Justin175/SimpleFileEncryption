package sfe;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.NoSuchPaddingException;

import sfe.command.ConsoleCommand;
import sfe.commands.CommandEncrypt;
import sfe.io.CryptedInputStream;
import sfe.utils.FileNameGenerator;
import sfe.utils.FlagProcessor;
import sfe.utils.TextUtils;

public class SimpleFileEncryption {

	private static final HashMap<String, ConsoleCommand> COMMANDS = new HashMap<>();
	private static final HashMap<String, ConsoleCommand> COMMANDS_PRINT = new HashMap<>();
	private static String[] errorString;
	
	public static void main(String... args) {
		loadCommands();
		
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
		addCommand(new ConsoleCommand("decrypt", "dc", SimpleFileEncryption::decrypt, "[password-flag] [File/Folder]"));
		addCommand(new ConsoleCommand("createPasswordFile", "cpf", SimpleFileEncryption::createPasswordFile, "[<create-password-flag>...] [File/Folder]"));
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
		
		System.out.println("SFE-Password-Flags");
		System.out.println("  -p [password]     (Password as Plain-Text.)");
		System.out.println("  -f [file]         (Password in a File and not hashed.)");
		System.out.println("  -h [file]         (Password in a File and it is hashed.)");
		
		System.out.println();
		
		System.out.println("CPF-Flags");
		System.out.println("  (-l or -t must be inlcuded. -h and -l are voluntary.)");
		System.out.println("  -l [Length]       (Length-Flag: Length of Password (number of Bytes)");
		System.out.println("  -t [Password]     (Create Password-File from <Password>)");
		System.out.println("  -h                (Hashing Flag: Hashing the password)");
		System.out.println("  -p                (Plain-Flag: Password as Plain Text <Alphapbet=[a-zA-Z0-9!.+-?]>)");
		System.out.println("  -s                (Salt Password <Number of Bytes: 6>)");
		System.out.println("  -sl [Length]      (Salt Password with a specific number of Bytes)");
		System.out.println("  -o                (Open the file after Creation)");
	}
	
	private static final boolean createPasswordFile(String[] args) {
		int length = -1;
		byte[] givenPassword = null;
		long time = System.currentTimeMillis();
		
		boolean hashing = false;
		boolean plain = false;
		boolean salt = false;
		boolean open = false;
		int saltLength = 6;
		
		if(args.length < 3) {
			setErrorString("Wrong number of Arguments!", "Expected: > 2, Given: " + args.length);
			return false;
		}
		
		@SuppressWarnings("unchecked")
		FlagProcessor fp = new FlagProcessor().
		setFlagsPredicate(
				(x) -> { return x.equalsIgnoreCase("-l"); },
				(x) -> { return x.equalsIgnoreCase("-t"); },
				(x) -> { return x.equalsIgnoreCase("-h"); },
				(x) -> { return x.equalsIgnoreCase("-p"); },
				(x) -> { return x.equalsIgnoreCase("-s"); },
				(x) -> { return x.equalsIgnoreCase("-sl"); },
				(x) -> { return x.equalsIgnoreCase("-o"); }
		).
		setFlagProcess(
				/*-l  */ (flag, arguments, index, flagsData) -> { flagsData.put("password_length", arguments[index + 1]); },
				/*-t  */ (flag, arguments, index, flagsData) -> { flagsData.put("password_text", arguments[index + 1]); },
				/*-h  */ (flag, arguments, index, flagsData) -> { flagsData.put("hashing", true); },
				/*-p  */ (flag, arguments, index, flagsData) -> { flagsData.put("plain", true); },
				/*-s  */ (flag, arguments, index, flagsData) -> { flagsData.put("salt", true); flagsData.put("salt_length", "6"); },
				/*-sl */ (flag, arguments, index, flagsData) -> { flagsData.put("salt", true); flagsData.put("salt_length", arguments[index + 1]); },
				/*-o  */ (flag, arguments, index, flagsData) -> { flagsData.put("open", true); }
		);
		
		//process Arguments
		fp.process(args, 0, args.length - 2);
		
		//check for must flags
		if(fp.containsFlagData("password_length")) {
			String checkingLength = fp.getFlagsData().get("password_length").toString();
		
			//check for number
			if(!checkingLength.matches("0|([1-9][0-9]*)")) {
				setErrorString("Argument '" + checkingLength + "' from the flag '-l' is no a number!");
				return false;
			}
			
			length = Integer.parseInt(checkingLength);

			if(length <= 0) {
				setErrorString("Password-Length is too short.", "Expected: > 0", "Given: " + checkingLength);
				return false;
			}
		}
		
		if(fp.containsFlagData("password_text") && length != -1) { //Error
			setErrorString("-l and -t are not allowed at the same time.");
			return false;
		}
		
		if(fp.containsFlagData("password_text")) {
			String pw = fp.getFlagsData().get("password_text").toString();
			
			if(pw.length() <= 0) {
				setErrorString("Password is to smal.", "Expeteced Length: > 0", "Given: 0");
				return false;
			}
			
			givenPassword = pw.getBytes(StandardCharsets.UTF_8);
		}
		
		if(length == -1 && givenPassword == null) { //Error
			setErrorString("Flags -l or -t are missing.");
			return false;
		}
		
		if(fp.containsFlagData("hashing"))
			hashing = (boolean) fp.getFlagsData().get("hashing");
		
		if(fp.containsFlagData("plain"))
			plain = (boolean) fp.getFlagsData().get("plain");
		
		if(fp.containsFlagData("open"))
			plain = (boolean) fp.getFlagsData().get("open");
		
		if(fp.containsFlagData("salt")) {
			salt = (boolean) fp.getFlagsData().get("salt");
			
			if(fp.containsFlagData("salt_length")) {
				String checkingSaltLength = fp.getFlagsData().get("salt_length").toString();
				
				if(!checkingSaltLength.matches("0|([1-9][0-9]*)")) {
					setErrorString("Argument '" + checkingSaltLength + "' from the flag '-l' is no a number!");
					return false;
				}
				
				saltLength= Integer.parseInt(checkingSaltLength);
				if(length <= 0) {
					setErrorString("Password-Length is too short.", "Expected: > 0", "Given: " + checkingSaltLength);
					return false;
				}
			}
			else {
				setErrorString("No Salt-Length is given.");
				return false;
			}
		}
		
		//Start creating Password
		Random random = new Random();
		final char[] ALLOWED_CHARS = TextUtils.allowedPlainPasswordCharacters();
		
		//create password from length
		if(length != -1 && !plain) {
			givenPassword = new byte[length];
			random.nextBytes(givenPassword);
		}
		else if(length != -1 && plain) {
			//create password in ascii
			//a-zA-Z0-9!.+
			final char[] password = new char[length];
			
			for(int i = 0; i < password.length; i++) {
				password[i] = ALLOWED_CHARS[(int) ((ALLOWED_CHARS.length - 1) * random.nextFloat())];
			}
			
			givenPassword = new String(password).getBytes(StandardCharsets.UTF_8);
		}
			
		if(salt) {
			byte[] saltetPasswordBytes = new byte[givenPassword.length + saltLength];
			byte[] saltBytes = new byte[saltLength];
			
			//create Salt
			SecureRandom srand = new SecureRandom();
			if(plain) {
				for(int i = 0; i < saltLength; i++)
					saltBytes[i] = (byte) (ALLOWED_CHARS[(int) ((ALLOWED_CHARS.length - 1) * srand.nextFloat())] & 0xFF);
			}
			else
				srand.nextBytes(saltBytes);
			
			System.arraycopy(saltBytes, 0, saltetPasswordBytes, 0, saltLength);
			System.arraycopy(givenPassword, 0, saltetPasswordBytes, saltLength, givenPassword.length);
			
			givenPassword = saltetPasswordBytes;
		}
		
		if(hashing) {
			MessageDigest sha;
			try {
				sha = MessageDigest.getInstance("SHA-256");
			} catch (NoSuchAlgorithmException e) {
				setErrorString("SHA-256 is not supportet on your running System.");
				return false;
			}
			
			givenPassword = sha.digest(givenPassword);
			Arrays.copyOf(givenPassword, 128);
		}
		
		//create file
		String filestr = args[args.length - 1];
		File out = new File(filestr);
		
		if(out.exists() && out.isFile()) {
			//generate new name
			out = FileNameGenerator.generateValidFile(out);
			System.out.println("File allready exists. The name was automaticly changed to: '" + out.getName() + "'");
		}
		
		try {
			FileOutputStream os = new FileOutputStream(out);
			os.write(givenPassword);
			os.close();
		} catch (IOException e) {
			setErrorString("Unable to write a file in following path.", "Path: " + out.getName());
			return false;
		}
		
		time = System.currentTimeMillis() - time;
		System.out.println("Password-File created in " + (time < 1000 ? time + " ms" : (time / 1000) + " s") + ".");
		
		if(open) {
			try {
				Desktop.getDesktop().open(out);
			} catch (IOException e) {
				System.err.println("Unable to open file.");
			}
		}
		
		return true;
	}
	
	private static final boolean decrypt(String[] args) {
		if(args.length < 3) {
			setErrorString("Wrong number of Arguments!", "Expected: > 2, Given: " + args.length);
			return false;
		}
		
		//Check if first argument is a flag
		FlagType type;
		if((type = readFlag(args[0])) == FlagType.ERROR) {
			setErrorString("The first Argument '" + args[0] + "' is not a Password-Flag.");
			return false;
		}
		
		//read password
		byte[] password = readPassword(type, args[1]);
		boolean isHashed = type == FlagType.FILE_HASHED;
		if(password == null) {
			return false;
		}
		
		//Check file to encrypt
		File toDecrypt = new File(args[2]);
		
		if(!toDecrypt.exists()) {
			setErrorString("Decryption-File '" + toDecrypt.getAbsolutePath() + "' not found.");
			return false;
		}
		
		if(!toDecrypt.isFile()) {
			setErrorString("Decryption-File-Path '" + toDecrypt.getAbsolutePath() + "' is not linking to a File.", "Expected Type: File, Given: Directory");
			return false;
		}
		
		//Check dataFile
		String toWriteInFile;
		if(args.length >= 4)
			toWriteInFile = args[3];
		else {
			if(toDecrypt.getName().endsWith(".crypted"))
				toWriteInFile = toDecrypt.getName().substring(0, toDecrypt.getName().length() - ".crypted".length());
			else
				toWriteInFile = toDecrypt.getName() + ".decrypted";
		}
		File toWriteIn = new File(toWriteInFile);
		
		if(toWriteIn.exists() && args.length >= 4) {
			setErrorString("Goal-File '" + toWriteIn.getAbsolutePath() + "' is already existing.", "Use a other File-Name");
			return false;
		}
		else {
			//generate next Name
			toWriteIn = FileNameGenerator.generateValidFile(toWriteIn);
		}
		
		//Create AES-Crypter
		try {
			System.out.println("Setup decryption.");
			long startTime = System.currentTimeMillis();
			long lastTime = startTime;
			
			AESCrypter crypter = new AESCrypter(Crypter.MODE_DECRYPT, password, isHashed);
			CryptedInputStream is = new CryptedInputStream(new FileInputStream(toDecrypt), crypter);
			FileOutputStream os = new FileOutputStream(toWriteIn);
			
			System.out.print("Decrypting...");
			int last = is.read();
			while(last != -1) {
				os.write(last);
				last = is.read();
				
				if(System.currentTimeMillis() - lastTime > 1000) {
					System.out.print(".");
					lastTime = System.currentTimeMillis();
				}
			}
			
			os.close();
			is.close();
			
			lastTime = System.currentTimeMillis() - startTime;
			System.out.println();
			System.out.println("Decryption ends. Time needed: " + (lastTime < 1000 ? lastTime + " ms." : (lastTime / 1000) + " s."));
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
			setErrorString(e.getMessage());
			return false;
		}
		
		return true;
	}
	
	private static byte[] readPassword(FlagType pwType, String arg) {
		if(pwType == FlagType.TEXT) //Given in Console
			return arg.getBytes(StandardCharsets.UTF_8);
		
		//Given as File -> read file content
		File pwFile = new File(arg);
		if(!pwFile.exists()) {
			setErrorString("Password-File '" + arg + "' not found.");
			return null;
		}
		
		if(!pwFile.isFile()) {
			setErrorString("Password-File-Path is not linking to a File.", "Expected Type: File, Given: Directory");
			return null;
		}
		
		if(pwFile.length() >= Integer.MAX_VALUE - 100) { //ERROR
			setErrorString("The Password-File is too big.", "Allowed Bytes: < " + (Integer.MAX_VALUE - 100) + ", Given: " + pwFile.length());
			return null;
		}
		
		byte[] bytes = new byte[(int) pwFile.length()];
		int bytesIndex = 0;
		
		try {
			FileInputStream is = new FileInputStream(pwFile);
			
			while(bytesIndex != bytes.length) {
				bytesIndex += is.read(bytes, bytesIndex, bytes.length - bytesIndex);
			}
			
			is.close();
		} catch (IOException e) {
			setErrorString("Failed to read Password-File.", "Path=" + pwFile.getAbsolutePath());
			return null;
		}
		
		return bytes;
	}
	
	private static FlagType readFlag(String fStr) {
		if(fStr.equals("-p"))
			return FlagType.TEXT;
		else if(fStr.equals("-f"))
			return FlagType.FILE_TEXT;
		else if(fStr.equals("-h"))
			return FlagType.FILE_HASHED;
		else
			return FlagType.ERROR;
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
		System.out.println("Alias:  " + alias);
		System.out.println("Usage:  " + usage);
		System.out.println("Allowed Flags:");
		for(int i = 0; i < allowedFlags.length; i++) {
			System.out.printf("  %-" + (maxFlagsLength + 3) + "s: %s\n", allowedFlags[i], flagsDescription[i]);
		}
	}
}