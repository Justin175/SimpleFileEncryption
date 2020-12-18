package sfe;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import sfe.exception.ModeNotActiveExcetption;

public class AESCrypter implements Crypter {
	
	private final int mode;
	
	private final Cipher encryptCipher;
	private final Cipher decryptCipher;
	
	public AESCrypter() {
		this.mode = MODE_NONE;
		encryptCipher = null;
		decryptCipher = null;
	}
	
	public AESCrypter(int mode, byte[] password) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		this(mode, password, false);
	}
	
	public AESCrypter(int mode, byte[] password, boolean hashed) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		this.mode = mode;
		
		MessageDigest sha = hashed ? null : MessageDigest.getInstance("SHA-256");
		//---> Encrypt
		
		if(isModeActive(MODE_ENCRYPT))
			encryptCipher = initCipher(
					Cipher.getInstance("AES"), Cipher.ENCRYPT_MODE, hashed ? password : sha.digest(password)
			);
		else
			encryptCipher = null;
		
		//---> Decrypt
		
		if(isModeActive(MODE_DECRYPT))
			decryptCipher = initCipher(
					Cipher.getInstance("AES"), Cipher.DECRYPT_MODE, hashed ? password : sha.digest(password)
			);
		else
			decryptCipher = null;
	}
	
	@Override
	public int getMode() {
		return mode;
	}
	
	public int sizeAfterEncryption(int dataSize) {
		return dataSize + (16 - (dataSize % 16));
	}

	public long sizeAfterEncryption(long dataSize) {
		return dataSize + (16 - (dataSize % 16));
	}
	
	private Cipher initCipher(Cipher c, int opmode, byte[] password) throws InvalidKeyException {
		c.init(opmode, new SecretKeySpec(Arrays.copyOf(password, 16), "AES"));
		return c;
	}

	@Override
	public byte[] encrypt(byte[] buffer) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		return encrypt(buffer, 0, buffer.length);
	}

	@Override
	public byte[] encrypt(byte[] buffer, int offset, int length) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		byte[] encrypted = new byte[sizeAfterEncryption(length)];
		encrypt(buffer, offset, length, encrypted, 0);
		return encrypted;
	}

	@Override
	public void encrypt(byte[] buffer, int offset, int length, byte[] to, int offsetTo) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		//Encrypt
		if(!isModeActive(MODE_ENCRYPT))
			throw new ModeNotActiveExcetption("Encryption-Mode is not active.");
		
		encryptCipher.doFinal(buffer, offset, length, to, offsetTo);
	}

	@Override
	public byte[] decrypt(byte[] buffer) throws IllegalBlockSizeException, BadPaddingException {
		return decrypt(buffer, 0, buffer.length);
	}

	@Override
	public byte[] decrypt(byte[] buffer, int offset, int length) throws IllegalBlockSizeException, BadPaddingException {
		return decryptCipher.doFinal(buffer, offset, length);
	}

	@Override
	public void decrypt(byte[] buffer, int offset, int length, byte[] to, int offsetTo) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		//Encrypt
		if(!isModeActive(MODE_DECRYPT))
			throw new ModeNotActiveExcetption("Decryption-Mode is not active.");
		
		decryptCipher.doFinal(buffer, offset, length, to, offsetTo);
	}

	@Override
	public byte[] updateMultipartEncryption(byte[] buffer) {
		return updateMultipartEncryption(buffer, 0, buffer.length);
	}

	@Override
	public byte[] updateMultipartEncryption(byte[] buffer, int offset, int length) {
		return encryptCipher.update(buffer, offset, length);
	}

	@Override
	public byte[] endMultipartEncryption() throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		return encryptCipher.doFinal();
	}

	@Override
	public byte[] endMutipartEncryption(byte[] buffer) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		return endMutipartEncryption(buffer, 0, buffer.length);
	}

	@Override
	public byte[] endMutipartEncryption(byte[] buffer, int offset, int length) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		return encryptCipher.doFinal(buffer, offset, length);
	}

	@Override
	public byte[] updateMultipartDecryption(byte[] buffer) {
		return updateMultipartDecryption(buffer, 0, buffer.length);
	}

	@Override
	public byte[] updateMultipartDecryption(byte[] buffer, int offset, int length) {
		return decryptCipher.update(buffer, offset, length);
	}

	@Override
	public byte[] endMultipartDecryption() throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		return decryptCipher.doFinal();
	}

	@Override
	public byte[] endMultipartDecryption(byte[] buffer)
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		return decryptCipher.doFinal(buffer);
	}

	@Override
	public byte[] endMultipartDecryption(byte[] buffer, int offset, int length)
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
		return decryptCipher.doFinal(buffer, offset, length);
	}

}
