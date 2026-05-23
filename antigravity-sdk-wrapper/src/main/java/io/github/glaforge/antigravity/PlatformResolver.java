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

/**
 * Resolves the underlying OS and architecture platform.
 */
public class PlatformResolver {
	/**
	 * Default constructor.
	 */
	public PlatformResolver() {
	}

	/**
	 * Returns the platform slice string representing the OS and architecture.
	 *
	 * @return the platform slice string
	 */
	public static String getPlatformSlice() {
		String os = System.getProperty("os.name").toLowerCase();
		String arch = System.getProperty("os.arch").toLowerCase();

		String osPart;
		if (os.contains("linux"))
			osPart = "linux";
		else if (os.contains("mac") || os.contains("darwin"))
			osPart = "osx";
		else if (os.contains("windows"))
			osPart = "windows";
		else
			throw new IllegalStateException("Unsupported OS: " + os);

		String archPart;
		if (arch.contains("amd64") || arch.contains("x86_64"))
			archPart = "x86_64";
		else if (arch.contains("aarch64") || arch.contains("arm64"))
			archPart = "aarch64";
		else
			throw new IllegalStateException("Unsupported Architecture: " + arch);

		return osPart + "-" + archPart;
	}
}
