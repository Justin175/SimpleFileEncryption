package sfe;

public interface Crypter {

	public byte[] encrypt(byte[] buffer);
	
	public byte[] encrypt(byte[] buffer, int offset, int length);
	
	public void encrypt(byte[] buffer, int offset, int length, byte[] to, int offsetTo, int lengthTo);
}
