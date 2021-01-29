package sfe.io;

import java.io.IOException;
import java.io.OutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import sfe.Crypter;
import sfe.exception.ModeNotActiveExcetption;

public class CryptedOutputStream extends OutputStream {

	protected OutputStream os;
	protected final Crypter crypter;
	private byte[] toEncryptBuffer = new byte[1];
	
	public CryptedOutputStream(OutputStream os, Crypter crypter) {
		if(!crypter.isModeActive(Crypter.MODE_ENCRYPT))
			throw new ModeNotActiveExcetption("Encryption-Mode is not active!");

		if(os == null || crypter == null)
			throw new NullPointerException((os == null ? "OutputStream" : "Crypter") + " is null.");
		
		this.os = os;
		this.crypter = crypter;
	}
	
	@Override
	public void write(int b) throws IOException {
		byte bb = (byte) b;
		toEncryptBuffer[0] = bb;
		byte[] buffer = crypter.updateMultipartEncryption(toEncryptBuffer);
		
		if(buffer != null) {
			os.write(buffer);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			byte[] buffer = crypter.endMultipartEncryption();

			if(buffer != null && buffer.length != 0)
				os.write(buffer);
		} catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		
		os.close();
	}
	
	@Override
	public void flush() throws IOException {
		//end mulitipart...
		try {
			byte[] buffer = crypter.endMultipartEncryption();
			
			if(buffer != null && buffer.length != 0)
				os.write(buffer);
		} catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		
		os.flush();
	}
	
	public void finish() throws IOException {
		try {
			byte[] buffer = crypter.endMultipartEncryption();
			
			if(buffer != null && buffer.length != 0)
				os.write(buffer);
		} catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
	}
}
