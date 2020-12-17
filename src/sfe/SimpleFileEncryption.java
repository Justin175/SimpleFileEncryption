package sfe;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import sfe.io.CryptedInputStream;
import sfe.io.CryptedOutputStream;

public class SimpleFileEncryption {

	public static void main(String[] args) throws Exception {
		testEncryption("HELLOMYFDU");
		testDecryption("HELLOMYFDU");
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
