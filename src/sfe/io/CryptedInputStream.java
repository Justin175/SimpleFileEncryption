package sfe.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import sfe.Crypter;
import sfe.exception.ModeNotActiveExcetption;

public class CryptedInputStream extends InputStream {

	private InputStream is;
	private Crypter crypter;
	
	private byte[] toDecryptBuffer = new byte[1];
	private byte[] buffer = null;
	private int bufferIndex = -1;
	private boolean hasError = false;
	private Consumer<Exception> printErrorStackTrace;
	
	public CryptedInputStream(InputStream is, Crypter crypter) {
		this.is = is;
		this.crypter = crypter;
		
		if(!crypter.isModeActive(Crypter.MODE_DECRYPT))
			throw new ModeNotActiveExcetption("Decryption-Mode is not active!");
		
		printErrorStackTrace = (x) -> x.printStackTrace();
	}
	
	@Override
	public int read() throws IOException {
		if(hasError)
			return -1;
		
		if(bufferIndex != -1)
		{
			if(bufferIndex >= buffer.length) {
				bufferIndex = -1;
			}
			else
				return buffer[bufferIndex++];
		}
		
		while(bufferIndex == -1) {
			int r = is.read();
			
			if(r == -1)
				try {
					buffer = crypter.endMultipartDecryption();
					bufferIndex = 0;
				} catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
					printErrorStackTrace.accept(e);
					hasError = true;
					return -1;
				}
			else {
				toDecryptBuffer[0] = (byte) r;
				buffer = crypter.updateMultipartDecryption(toDecryptBuffer);
				bufferIndex = buffer.length == 0 ? -1 : 0;
			}
		}
		
		int ret = -1;
		if(bufferIndex != -1 && bufferIndex < buffer.length)
			ret = buffer[bufferIndex++];

		return ret;
	}

	public boolean isHasError() {
		return hasError;
	}
	
	public Consumer<Exception> getPrintErrorStackTrace() {
		return printErrorStackTrace;
	}
	
	public void setPrintErrorStackTrace(Consumer<Exception> printErrorStackTrace) {
		this.printErrorStackTrace = printErrorStackTrace;
	}
}
