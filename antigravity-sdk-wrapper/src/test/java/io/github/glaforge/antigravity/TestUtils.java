package io.github.glaforge.antigravity;

public class TestUtils {
	public interface TestRunnable {
		void run() throws Throwable;
	}

	public static void retry(int maxRetries, TestRunnable runnable) throws Exception {
		for (int i = 0; i < maxRetries; i++) {
			try {
				runnable.run();
				return;
			} catch (Throwable e) {
				if (i == maxRetries - 1) {
					if (e instanceof Exception)
						throw (Exception) e;
					if (e instanceof Error)
						throw (Error) e;
					throw new RuntimeException(e);
				}
				System.err.println("Test failed on attempt " + (i + 1) + " due to: " + e.getMessage() + ". Retrying...");
				Thread.sleep(2000); // Give the API a moment before retrying
			}
		}
	}
}
