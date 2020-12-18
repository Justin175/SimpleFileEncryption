package sfe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;

import javax.crypto.NoSuchPaddingException;

import sfe.command.ConsoleCommand;
import sfe.io.CryptedInputStream;
import sfe.io.CryptedOutputStream;

public class SimpleFileEncryption {

	private static final HashMap<String, ConsoleCommand> COMMANDS = new HashMap<>();
	private static String[] errorString;
	
	public static void main(String[] args) {
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
				System.err.println(" > " + cmd.getCommandName());
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
		addCommand(new ConsoleCommand("encrypt", SimpleFileEncryption::encrypt, "[password-flag] [File/Folder]"));
		addCommand(new ConsoleCommand("decrypt", SimpleFileEncryption::decrypt, "[password-flag] [File/Folder]"));
	}
	
	private static void addCommand(ConsoleCommand cmd) {
		COMMANDS.put(cmd.getCommandName().toLowerCase(), cmd);
	}
	
	private static void printCommands() {
		System.out.println("SFE-Commands:");
		for(ConsoleCommand a : COMMANDS.values()) //Print commands
			System.out.println("  " + a.getCommandName() + " " + a.getUsage());
		
		System.out.println();
		
		System.out.println("SFE-Password-Flags");
		System.out.println("  -p [password]     (Password as Plain-Text.)");
		System.out.println("  -f [file]         (Password in a File and not hashed.)");
		System.out.println("  -h [file]         (Password in a File and it is hashed.)");
	}
	
	private static final boolean encrypt(String[] args) {
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
		File toEncrypt = new File(args[2]);
		
		if(!toEncrypt.exists()) {
			setErrorString("Encryption-File '" + toEncrypt.getAbsolutePath() + "' not found.");
			return false;
		}
		
		if(!toEncrypt.isFile()) {
			setErrorString("Encryption-File-Path '" + toEncrypt.getAbsolutePath() + "' is not linking to a File.", "Expected Type: File, Given: Directory");
			return false;
		}
		
		//Check dataFile
		File toWriteIn = new File(args.length >= 4 ? args[3] : toEncrypt.getAbsolutePath() + ".crypted");
		
		if(toWriteIn.exists() && args.length >= 4) {
			setErrorString("Goal-File '" + toWriteIn.getAbsolutePath() + "' is already existing.", "Use a other File-Name");
			return false;
		}
		else {
			//generate next Name
			int nameAdd = 0;
			String name = toEncrypt.getName();
			int firstPoint = name.indexOf('.');
			String ending = name.substring(firstPoint + 1);
			name = name.substring(0, firstPoint);
			
			while(toWriteIn.exists()) {
				toWriteIn = new File(toWriteIn.getAbsoluteFile().getParentFile().getPath() + "/" + name + nameAdd + "." + ending + ".crypted");
				nameAdd += 1;
			}
		}
		
		
		//Create AES-Crypter
		try {
			AESCrypter crypter = new AESCrypter(Crypter.MODE_ENCRYPT, password, isHashed);
			CryptedOutputStream os = new CryptedOutputStream(new FileOutputStream(toWriteIn), crypter);
			FileInputStream is = new FileInputStream(toEncrypt);
			
			int last = is.read();
			while(last != -1) {
				os.write(last);
				last = is.read();
			}
			
			os.close();
			is.close();
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
			setErrorString(e.getMessage());
			return false;
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
			int nameAdd = 0;
			String name = toDecrypt.getName();
			int firstPoint = name.indexOf('.');
			String ending = name.substring(firstPoint + 1);
			name = name.substring(0, firstPoint);
			
			while(toWriteIn.exists()) {
				toWriteIn = new File(toWriteIn.getAbsoluteFile().getParentFile().getPath() + "/" + name + nameAdd + "." + ending + ".crypted");
				nameAdd += 1;
			}
		}
		
		
		//Create AES-Crypter
		try {
			AESCrypter crypter = new AESCrypter(Crypter.MODE_DECRYPT, password, isHashed);
			CryptedInputStream is = new CryptedInputStream(new FileInputStream(toDecrypt), crypter);
			FileOutputStream os = new FileOutputStream(toWriteIn);
			
			int last = is.read();
			while(last != -1) {
				os.write(last);
				last = is.read();
			}
			
			os.close();
			is.close();
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
}
