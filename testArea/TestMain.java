import sfe.SimpleFileEncryption;

public class TestMain {
	
	private static final boolean IS_TESTING = false;;
	
	public static void main(String[] args) throws Exception {
//		if(!IS_TESTING) {
////			SimpleFileEncryption.main(new String[] {"help"});
////			SimpleFileEncryption.main(new String[] {"createPasswordFile", "-s", "-p", "-l", "128", "test/test_pw_autogen.key"});
//			SimpleFileEncryption.main(new String[] {"encrypt", "-f", "test/test_pw_autogen.key", "test/Geheim.txt", "test/Geheim.txt.encrypted"});
//			SimpleFileEncryption.main(new String[] {"decrypt", "-f", "test/test_pw_autogen.key", "test/Geheim.txt.encrypted", "test/t_de.txt"});
//			return;
//		}
		
//		SimpleFileEncryption.main("encrypt", "-d", "-z", "-o", "test/out/encrypted/out*.zip", "-f", "test/test_pw_autogen.key", "test/in/");
//		SimpleFileEncryption.main("encrypt", "-d", "-o", "test/out/encrypted/out*/", "-f", "test/test_pw_autogen.key", "test/in/");
//		SimpleFileEncryption.main("decrypt", "-d", "-o", "test/out/decrypted/out*", "-f", "test/test_pw_autogen.key", "test/out/encrypted/out/");
	
		SimpleFileEncryption.main("encrypt", "-zc", "-o", "test/out/encrypted/out_zc*.zip", "-f", "test/test_pw_autogen.key", "test/in/toEncrypt.zip");
	}
}
