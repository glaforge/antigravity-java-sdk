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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the underlying OS and architecture platform and manages native
 * binary extraction.
 */
public class PlatformResolver {

	private static final Logger log = LoggerFactory.getLogger(PlatformResolver.class);

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

	/**
	 * Resolves and extracts the native localharness binary for the current platform
	 * into a cached directory. If the binary is already extracted and valid, it is
	 * reused.
	 *
	 * @return the File handle to the executable binary
	 * @throws IOException
	 *             if extraction fails or asset is missing
	 */
	public static synchronized File resolveBinary() throws IOException {
		// 1. Check for explicit path override via system property or environment
		// variable
		String customPath = System.getProperty("antigravity.harness.path");
		if (customPath == null || customPath.isBlank()) {
			customPath = System.getenv("ANTIGRAVITY_HARNESS_PATH");
		}
		if (customPath != null && !customPath.isBlank()) {
			File customBinary = new File(customPath);
			if (customBinary.exists() && customBinary.canExecute()) {
				log.debug("Using custom localharness binary from: {}", customBinary.getAbsolutePath());
				return customBinary;
			}
			throw new FileNotFoundException(
					"Configured localharness binary not found or not executable at: " + customPath);
		}

		String platformSlice = getPlatformSlice();
		boolean isWindows = platformSlice.startsWith("windows");
		String ext = isWindows ? ".exe" : "";
		String binaryFileName = "localharness" + ext;
		String resourcePath = "/google/antigravity/bin/" + platformSlice + "/" + binaryFileName;

		File baseDir;
		String userHome = System.getProperty("user.home");
		if (userHome != null && !userHome.isBlank()) {
			baseDir = new File(userHome, ".antigravity/bin/" + platformSlice);
		} else {
			baseDir = new File(System.getProperty("java.io.tmpdir"), "antigravity-bin/" + platformSlice);
		}
		if (!baseDir.exists() && !baseDir.mkdirs()) {
			baseDir = new File(System.getProperty("java.io.tmpdir"), "antigravity-bin/" + platformSlice);
			baseDir.mkdirs();
		}

		File targetBinary = new File(baseDir, binaryFileName);

		try (InputStream binaryStream = PlatformResolver.class.getResourceAsStream(resourcePath)) {
			if (binaryStream == null) {
				// If embedded resource is missing from classpath, check if an existing cached
				// binary is present
				if (targetBinary.exists() && targetBinary.canExecute()) {
					log.warn(
							"Embedded Go harness asset missing for slice: {}, but found existing cached binary at: {}. Reusing cached binary.",
							platformSlice, targetBinary.getAbsolutePath());
					return targetBinary;
				}
				throw new FileNotFoundException("Embedded Go harness engine asset missing for slice: " + platformSlice
						+ ". Please ensure antigravity-sdk-wrapper was packaged with native binaries, or set the ANTIGRAVITY_HARNESS_PATH environment variable (or 'antigravity.harness.path' system property).");
			}

			byte[] resourceBytes = binaryStream.readAllBytes();
			if (!targetBinary.exists() || targetBinary.length() != resourceBytes.length) {
				File tempFile = File.createTempFile("localharness-extract-", ext, baseDir);
				Files.write(tempFile.toPath(), resourceBytes);
				if (!tempFile.setExecutable(true)) {
					throw new IllegalStateException(
							"Failed to grant execution rights to binary: " + tempFile.getAbsolutePath());
				}
				try {
					Files.move(tempFile.toPath(), targetBinary.toPath(), StandardCopyOption.REPLACE_EXISTING,
							StandardCopyOption.ATOMIC_MOVE);
				} catch (AtomicMoveNotSupportedException e) {
					Files.move(tempFile.toPath(), targetBinary.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}

		if (!targetBinary.setExecutable(true)) {
			throw new IllegalStateException(
					"Failed to grant execution rights to binary: " + targetBinary.getAbsolutePath());
		}

		return targetBinary;
	}
}
