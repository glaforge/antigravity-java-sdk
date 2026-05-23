/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
				System.err
						.println("Test failed on attempt " + (i + 1) + " due to: " + e.getMessage() + ". Retrying...");
				Thread.sleep(2000); // Give the API a moment before retrying
			}
		}
	}
}
