package sfe.commands;

import static sfe.SimpleFileEncryption.printCommandInfo;
import static sfe.SimpleFileEncryption.setErrorString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.NoSuchPaddingException;

import sfe.AESCrypter;
import sfe.Crypter;
import sfe.command.ConsoleCommand;
import sfe.io.CryptedOutputStream;
import sfe.utils.CryptingRunnable;
import sfe.utils.FlagProcessor;

public class CommandEncrypt extends ConsoleCommand {
	
	private static CommandEncrypt commandEncrypt;
	
	public static final String[] ALLOWED_FLAGS = {
			"--help",
			"-p [Password]",
			"-f [File]",
			"-o [Out-Name]",
			"-h",
			"-d",
			"-r",
			"-z"
	};
	
	public static final String[] FLAGS_DESCRYPTION = {
			"Prints Command-Help-Information",
			"Password given as plain-text",
			"Password stored in a file",
			"Define the name/path of the output-file <Use * in name/path for adding autogenerated and checked number>",
			"Password is hashed",
			"Path is a directory",
			"Recursive-Mode (process main directories and sub-directories",
			"Ouput files as zip"
	};

	public CommandEncrypt() {
		super("encrypt", "en", CommandEncrypt::process, "encrypt  [<flags>...] [path]");
		
		commandEncrypt = this;
	}
	
	private static boolean process(String[] args) {
		//--help-Flag is processed lonly
		if(args[0].equalsIgnoreCase("--help")) {
			printCommandInfo(commandEncrypt, ALLOWED_FLAGS, FLAGS_DESCRYPTION);
			return true;
		}
		
		if(args.length < 3) {
			setErrorString("Wrong number of Arguments!", "Expected: > 2, Given: " + args.length);
			return false;
		}
		
		//check to encryt file for existence
		File toEncrypt = new File(args[args.length - 1]);
		if(!toEncrypt.exists()) {
			setErrorString("The target-file/directory for encryption not exists.", "Given: " + args[args.length - 1]);
			return false;
		}
		
		FlagProcessor fp = createFlagProcessor();
		fp.process(args, 0, args.length - 1);
		
		//check flags
		if(!fp.containsFlagData("password_text") && !fp.containsFlagData("password_file")) {
			setErrorString("Missing Flags.", "The flags -p or -f not given.", "Expected: 1", "Given: 0");
			return false;
		}
		
		if(fp.containsFlagData("password_text") && fp.containsFlagData("password_file")) {
			setErrorString("To many flags.", "The flags -p and -f are given at the same time.", "Expected: 1", "Given: 2");
			return false;
		}
		
		//process-Flags
		boolean isHashed = false;
		boolean isDirectory = false;
		boolean isRecursive = false;
		boolean isZipOutput = false;
		File output;
		
		if(fp.containsFlagData("hashed"))
			isHashed = true;
		
		if(fp.containsFlagData("directory"))
			isDirectory = true;
		
		if(fp.containsFlagData("recursive"))
			isRecursive = true;
		
		if(fp.containsFlagData("zip"))
			isZipOutput = true;
		
		//check target-file
		if(isDirectory && !toEncrypt.isDirectory()) {
			setErrorString("Given Path is not a directory.", "Given: " + args[args.length - 1]);
			return false;
		}
		
		//check out_name flag
		if(fp.containsFlagData("out_name")) {
			String name = (String) fp.getData("out_name");
			if(name.contains("*")) {
				//count
				int count = 0;
				for(char a : name.toCharArray())
					if(a == '*') count++;
				
				if(count > 1) {
					setErrorString("Only 1 '*' is allowed.", "Expected: 1", "Given: " + count);
					return false;
				}
				
				int currentNumber = 0;
				output = new File(name.replace("*", "")); 
				
				while(output.exists() && (isDirectory ? output.isDirectory() : output.isFile()))
					output = new File(name.replace("*", "" + (currentNumber++)));
			}
			else
				output = new File(name);
			
			if(output.exists() && (isDirectory ? output.isDirectory() : output.isFile())) {
				setErrorString(
						"The choosen Output-Name already existing in this directory.",
						"Given: " + name,
						"Given <Absulute-Path>: " + output.getAbsolutePath(),
						"Given <Sub-Directory>:" + output.getAbsoluteFile().getParent()
				);
						
				return false;
			}
		}
		else { //generate output-file
			String name = toEncrypt.getName() + (isZipOutput ? ".zip" : ".en");
			output = new File(name);
			
			if(output.exists() && (isDirectory ? output.isDirectory() : output.isFile())) {
				int i = toEncrypt.getName().indexOf('.');
				String firstHalf = i != -1 ? toEncrypt.getName().substring(0, i) : toEncrypt.getName();
				int currentNumber = 0;
				String end = (i != -1 ? toEncrypt.getName().substring(i) : "") + (isZipOutput ? ".zip" : ".en");
				
				while(output.exists() && (isDirectory ? output.isDirectory() : output.isFile()))
					output = new File(firstHalf + (currentNumber++) + end);
				
			}
		}
		
		//read-password
		byte[] password = readPassword((String) fp.getData("password"), fp.containsFlagData("password_file"));
		if(password == null)
			return false;
		
		//start encrypting
		try {
			AESCrypter crypter = new AESCrypter(Crypter.MODE_ENCRYPT, password, isHashed);
			
			if(isDirectory) {
				List<File> files = new LinkedList<>();
				System.out.println("Loading Files...");
				getFiles(toEncrypt, files, isRecursive);;
				
				if(isZipOutput) {
					ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output));
					OutputStream os = new CryptedOutputStream(zos, crypter);
					
					encrypt(output, () -> {
						boolean first = true;
						for(File a : files) {
							if(zos != null) {
								if(!first)
									zos.closeEntry();
								
								zos.putNextEntry(new ZipEntry(a.getPath()));
								first = false;
							}
							
							readAndWrite(os, a);
						}
					});
					
					os.close();
				}
				else {
					OutputStream os = new CryptedOutputStream(new FileOutputStream(output), crypter);
					/*TODO*/
				}
				
				//Start enrypting files
			}
			else {
				OutputStream os;
				
				if(isZipOutput) {
					ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output));
					os = new CryptedOutputStream(zos, crypter);
					zos.putNextEntry(new ZipEntry(toEncrypt.getName()));
				}
				else {
					os = new CryptedOutputStream(new FileOutputStream(output), crypter);
				}
				
				encrypt(output, () -> readAndWrite(os, toEncrypt));
				os.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			setErrorString("An error corrupted while writing the encrypted-file/directory.", "Java-Error-Message: " + e.getMessage());
			return false;
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			setErrorString("An error corrupted while encrypting.", "Java-Error-Message: " + e.getMessage());
			return false;
		}
		
		return true;
	}
	
	private static void getFiles(File currentDir, List<File> files, boolean recursive) {
		for(File a : currentDir.listFiles()) {
			if(a.isFile())
				files.add(a);
			
			if(recursive && a.isDirectory())
				getFiles(a, files, recursive);
		}
	}
	
	private static void encrypt(File output, CryptingRunnable run) throws IOException {
		System.out.println("Start encryption....");
		long time = System.currentTimeMillis();
		run.run();
		time = System.currentTimeMillis() - time;
		System.out.println("End encrypting...");
		
		printEncryptionInfo(output, time);
	}
	
	private static void printEncryptionInfo(File output, long time) {
		System.out.println("Encryption end.");
		System.out.println("Output-Info:");
		System.out.println("  Path <Relative>:  " + output.getPath());
		System.out.println("  Path <Absolute>:  " + output.getAbsolutePath());
		System.out.println("  Name:             " + output.getName());
		System.out.println("  Time took:        " + (time < 1000 ? time + " ms" : (time / 1000) + " s"));
	}
	
	private static void readAndWrite(OutputStream os, File toEncrypt) throws IOException {
		FileInputStream is = new FileInputStream(toEncrypt);
		readAndWrite(os, is);
		is.close();
	}
	
	private static void readAndWrite(OutputStream os, InputStream is) throws IOException {
		int b = is.read();
		while(b != -1) {
			os.write(b);
			b = is.read();
		}
	}
	
	private static byte[] readPassword(String pwLink, boolean isFile) {
		if(!isFile)
			return pwLink.getBytes(StandardCharsets.UTF_8);
		
		//read file
		File pwFile = new File(pwLink);
		
		//check file
		if(!pwFile.exists() || !pwFile.isFile()) {
			setErrorString("Password-File not found.", "Given: " + pwLink);
			return null;
		}
		
		//check file-size
		if(pwFile.length() > (1024 * 1024 * 20)) /*20mbytes*/ {
			int mb = 1024 * 1024;
			setErrorString("Password-File is to big.", "Expected: 20 Mbytes", "Given: round > " + (pwFile.length() / mb) + "mb");
			return null;
		}
		
		//Read File
		byte[] password = new byte[(int) pwFile.length()];
		
		try {
			FileInputStream is = new FileInputStream(pwFile);
			int index = 0;
			
			while(index != password.length)
				password[index++] = (byte) is.read(); 
			
			is.close();
		} catch (Exception e) {
			setErrorString("An error corrupted while reading the password-file.", "Given: " + pwLink);
		}
		
		return password;
	}
	
	@SuppressWarnings("unchecked")
	private static final FlagProcessor createFlagProcessor() {
		FlagProcessor fp = new FlagProcessor();
		fp.setFlagsPredicate(
				"-p"::equalsIgnoreCase,
				"-f"::equalsIgnoreCase,
				"-o"::equalsIgnoreCase,
				"-h"::equalsIgnoreCase,
				"-d"::equalsIgnoreCase,
				"-r"::equalsIgnoreCase,
				"-z"::equalsIgnoreCase
		);
		fp.setFlagProcess(
				/*-p  */ (flag, arguments, index, flagsData) -> { flagsData.put("password_text", true); flagsData.put("password", arguments.length > (index + 1) ? arguments[index + 1] : null); },
				/*-f  */ (flag, arguments, index, flagsData) -> { flagsData.put("password_file", true); flagsData.put("password", arguments.length > (index + 1) ? arguments[index + 1] : null); },
				/*-o  */ (flag, arguments, index, flagsData) -> { flagsData.put("out_name", arguments.length > (index + 1) ? arguments[index + 1] : null); },
				/*-h  */ (flag, arguments, index, flagsData) -> { flagsData.put("hashed", true); },
				/*-d  */ (flag, arguments, index, flagsData) -> { flagsData.put("directory", true); },
				/*-r  */ (flag, arguments, index, flagsData) -> { flagsData.put("recursive", true); },
				/*-z  */ (flag, arguments, index, flagsData) -> { flagsData.put("zip", true); }
		);
		
		return fp;
	}
}