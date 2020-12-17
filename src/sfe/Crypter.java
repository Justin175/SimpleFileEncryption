package sfe;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

/**
 * @author Justin Treulieb
 */
public interface Crypter {
	
	static final int MODE_NONE 			= 0x0;
	static final int MODE_ENCRYPT 		= 0x1;
	static final int MODE_DECRYPT 		= 0x2;
	
	static final int MODE_EN_DE_CRYPT 	= MODE_ENCRYPT | MODE_DECRYPT;
	
	public int getMode();
	
	default boolean isModeActive(int mode) {
		return (getMode() & mode) == mode;
	}

	public byte[] encrypt(byte[] buffer) 
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	
	public byte[] encrypt(byte[] buffer, int offset, int length) 
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	
	public void encrypt(byte[] buffer, int offset, int length, byte[] to, int offsetTo) 
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	
	public byte[] decrypt(byte[] buffer) 
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	
	public byte[] decrypt(byte[] buffer, int offset, int length)
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	
	public void decrypt(byte[] buffer, int offset, int length, byte[] to, int offsetTo) 
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;

	/*
	 *  Multi-part encryptions
	 */
	
	public byte[] updateMultipartEncryption(byte[] buffer);
	
	public byte[] updateMultipartEncryption(byte[] buffer, int offset, int length);
	
	public byte[] endMultipartEncryption() 
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	
	public byte[] endMutipartEncryption(byte[] buffer)
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	
	public byte[] endMutipartEncryption(byte[] buffer, int offset, int length)
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	
	/*
	 *  Multi-part decryptions
	 */
	
	public byte[] updateMultipartDecryption(byte[] buffer);
	
	public byte[] updateMultipartDecryption(byte[] buffer, int offset, int length);
	
	public byte[] endMultipartDecryption() 
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	
	public byte[] endMultipartDecryption(byte[] buffer)
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	
	public byte[] endMultipartDecryption(byte[] buffer, int offset, int length)
			throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
}
