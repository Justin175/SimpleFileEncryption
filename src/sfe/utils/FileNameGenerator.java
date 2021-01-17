package sfe.utils;

import java.io.File;

public class FileNameGenerator {

	public static File generateValidFile(File start) {
		int nameAdd = 0;
		String name = start.getName();
		int firstPoint = name.indexOf('.');
		String ending = name.substring(firstPoint + 1);
		name = name.substring(0, firstPoint);
		
		while(start.exists()) {
			start = new File(start.getAbsoluteFile().getParentFile().getPath() + "/" + name + nameAdd + "." + ending);
			nameAdd += 1;
		}
		
		return start;
	}
	
	public static File generateValidFile(String file) {
		return generateValidFile(new File(file));
	}
}
