package sfe.utils;

public class TextUtils {

	public static final char[] allowedPlainPasswordCharacters() {
		char[] ALLOWED = new char[26 * 2 + 10 + 5];
		int index = 0;

		//numbers
		for(int i = 0; i < 10; i++) {
			ALLOWED[index++] = (char) ('0' + i);
		}
		
		//normal chars
		for(int i = 0; i < 26; i++)	{
			ALLOWED[index++] = (char) ('a' + i);
			ALLOWED[index++] = (char) ('A' + i);
		}
		
		//Special chars
		ALLOWED[index++] = '!';
		ALLOWED[index++] = '+';
		ALLOWED[index++] = '.';
		ALLOWED[index++] = '-';
		ALLOWED[index++] = '?';
		
		return ALLOWED;
	}
}
