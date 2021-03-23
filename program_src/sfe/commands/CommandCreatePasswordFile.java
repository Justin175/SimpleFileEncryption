package sfe.commands;

import static sfe.SimpleFileEncryption.printCommandInfo;
import static sfe.SimpleFileEncryption.setErrorString;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import sfe.command.ConsoleCommand;
import sfe.utils.FileNameGenerator;
import sfe.utils.FlagProcessor;
import sfe.utils.TextUtils;

public class CommandCreatePasswordFile extends ConsoleCommand {
	
	private static CommandCreatePasswordFile commandCreatePasswordFile;
	
	public static final String[] ALLOWED_FLAGS = {
			"--help",
			"-l [Length]",
			"-t [Password]",
			"-h",
			"-p",
			"-s",
			"-sl [Length]",
			"-o"
	};
	
	public static final String[] FLAGS_DESCRYPTION = {
			/**/	"Prints Command-Help-Information",
			/*-l */	"Length of Password (number of Bytes)",
			/*-t */	"Create Password-File from <Password>",
			/*-h */	"Hashing the password",
			/*-p */	"Password as Plain Text <Alphapbet=[a-zA-Z0-9!.+-?]>",
			/*-s */	"Salt Password <Number of Bytes: 6>",
			/*-sl*/	"Salt Password with a specific number of Bytes",
			/*-o */	"Open the file after Creation",
	};

	public CommandCreatePasswordFile() {
		super("createPasswordFile", "cpf", CommandCreatePasswordFile::process, "[<create-password-flag>...] [File/Folder]");
		
		commandCreatePasswordFile = this;
	}

	private static boolean process(String[] args) {
		if(args[0].equalsIgnoreCase("--help")) {
			printCommandInfo(commandCreatePasswordFile, ALLOWED_FLAGS, FLAGS_DESCRYPTION);
			return true;
		}
		
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
		fp.process(args, 0, args.length - 1);
		
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
			open = (boolean) fp.getFlagsData().get("open");
		
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
}
