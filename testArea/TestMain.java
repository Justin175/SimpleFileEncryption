import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import sfe.AESCrypter;
import sfe.Crypter;
import sfe.SimpleFileEncryption;
import sfe.io.CryptedInputStream;
import sfe.io.CryptedOutputStream;

public class TestMain {
	
	private static final boolean IS_TESTING = false;;
	
	public static void main(String[] args) throws Exception {
		if(!IS_TESTING) {
			SimpleFileEncryption.main(new String[] {"encrypt", "-f", "test/Test_Password.txt", "test/Test.txt", "test/t_en.encrypted"});
			SimpleFileEncryption.main(new String[] {"decrypt", "-f", "test/Test_Password.txt", "test/t_en.encrypted", "test/t_de.txt"});
			return;
		}
		
//		testEncryption("HELLOMYFDU");
//		testDecryption("HELLOMYFDU");
		
		String str = "Hallo.txt";
		String toWriteInFile;
		
		if(str.endsWith(".crypted"))
			toWriteInFile = str.substring(0, str.length() - ".crypted".length());
		else
			toWriteInFile = str + ".decrypted";
		
		System.out.println(toWriteInFile);
	}

	private static void testDecryption(String pw) throws Exception {
		AESCrypter crypter = new AESCrypter(Crypter.MODE_EN_DE_CRYPT, pw.getBytes(StandardCharsets.UTF_8));
		
		CryptedInputStream iss = new CryptedInputStream(new FileInputStream("test/en_test.txt"), crypter);
		FileOutputStream oss = new FileOutputStream("test/de_test.txt");
		
		byte[] buffer = new byte[1 << 7];
		int size;
		
		while((size = iss.read(buffer)) != -1) {
			oss.write(buffer, 0, size);
		}
		
		iss.close();
		oss.close();
	}
	
	private static void testEncryption(String pw) throws Exception {
		AESCrypter crypter = new AESCrypter(Crypter.MODE_EN_DE_CRYPT, pw.getBytes(StandardCharsets.UTF_8));
		
		FileInputStream is = new FileInputStream("test/Test.txt");
		CryptedOutputStream os = new CryptedOutputStream(new FileOutputStream("test/en_test.txt"), crypter);
		
		byte[] buffer = new byte[1 << 7];
		int size;
		
		while((size = is.read(buffer)) != -1) {
			os.write(buffer, 0, size);
		}
		
		is.close();
		os.close();
	}
}
