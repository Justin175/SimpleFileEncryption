package sfe.utils;

import java.io.IOException;

@FunctionalInterface
public interface CryptingRunnable {
	public void run() throws IOException;
}
