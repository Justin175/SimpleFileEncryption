package sfe.utils;

public class ByteArrayPrintUtils {
	
	public static final char[] HEX_ALPHABET = "0123456789ABCDEF".toCharArray();

	public static void printHexByteArray(byte[] buffer) {
		printHexByteArray(buffer, 0, buffer.length);
	}
	
	public static void printHexByteArray(int newLineAfterElements, byte[] buffer) {
		printHexByteArray(newLineAfterElements, buffer, 0, buffer.length);
	}
	
	public static void printHexByteArray(byte[] buffer, int offset, int length) {
		printHexByteArray(-1, buffer, offset, length);
	}
	
	public static void printHexByteArray(int newLineAfterElements, byte[] buffer, int offset, int length) {
		char[] hexBuffer = new char[3];
		hexBuffer[2] = ' ';
		
		for(int i = offset; i < offset + length; i++) {
			byteToHexCharacterArray(buffer[i], hexBuffer);
			
			System.out.print(new String(hexBuffer));
			if(newLineAfterElements != -1 && i % newLineAfterElements == 0)
				System.out.println();
		}
		
		System.out.println();
	}
	
	public static void byteToHexCharacterArray(byte b, char[] buf) {
		int hex = 0;
		int currentBit = 1;
		int bit = 0;
		int index = 1;
		
		while(currentBit <= 0x80) {
			if((b & currentBit) == currentBit) { //Bit is set
				hex += 1 << bit;
			}

			currentBit <<= 1;
			bit++;
			
			if(bit == 4) {
				buf[index--] = HEX_ALPHABET[hex];
				bit = 0;
				hex = 0;
			}
		}
	}
}
