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
 * binary resolution via local path overrides, local cache, classpath assets
 * (from optional classifier artifacts), or pure Java on-demand downloading.
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
	 * Resolves the native localharness binary for the current platform following
	 * this resolution hierarchy:
	 * <ol>
	 * <li>Explicit path override via system property
	 * {@code antigravity.harness.path} or environment variable
	 * {@code ANTIGRAVITY_HARNESS_PATH}.</li>
	 * <li>Cached binary in {@code ~/.antigravity/bin/<slice>/localharness} matching
	 * the expected upstream version.</li>
	 * <li>Classpath resource in {@code /google/antigravity/bin/<slice>/} (from
	 * optional platform classifier JARs).</li>
	 * <li>On-demand download from PyPI directly into the cache using pure Java HTTP
	 * and streaming ZIP extraction.</li>
	 * </ol>
	 *
	 * @return the File handle to the executable binary
	 * @throws IOException
	 *             if resolution or download fails
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
		File versionFile = new File(baseDir, ".version");

		// 2. Check if cached binary already exists and matches expected version
		if (targetBinary.exists() && targetBinary.canExecute()) {
			if (versionFile.exists()) {
				try {
					String cachedVersion = Files.readString(versionFile.toPath()).trim();
					if (HarnessDownloader.DEFAULT_UPSTREAM_VERSION.equals(cachedVersion)) {
						return targetBinary;
					}
				} catch (Exception ignored) {
				}
			} else {
				// Cached binary exists without version stamp; reuse it
				return targetBinary;
			}
		}

		// 3. Check for bundled classpath resource (from optional classifier artifact)
		try (InputStream binaryStream = PlatformResolver.class.getResourceAsStream(resourcePath)) {
			if (binaryStream != null) {
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
					Files.writeString(versionFile.toPath(), HarnessDownloader.DEFAULT_UPSTREAM_VERSION);
				}
				if (targetBinary.canExecute() || targetBinary.setExecutable(true)) {
					return targetBinary;
				}
			}
		}

		// 4. Fallback to existing binary if one is present
		if (targetBinary.exists() && targetBinary.canExecute()) {
			return targetBinary;
		}

		// 5. On-demand lazy download via HarnessDownloader
		boolean allowDownload = Boolean.parseBoolean(System.getProperty("antigravity.harness.download", "true"));
		if (allowDownload) {
			try {
				HarnessDownloader downloader = new HarnessDownloader();
				downloader.downloadAndExtract(platformSlice, targetBinary, baseDir);
				if (targetBinary.exists() && targetBinary.canExecute()) {
					return targetBinary;
				}
			} catch (Exception e) {
				log.warn("Failed to download localharness on-demand for {}: {}", platformSlice, e.getMessage());
			}
		}

		throw new FileNotFoundException("Localharness Go binary not found for platform slice: " + platformSlice
				+ ". Ensure an internet connection is available to download it automatically, "
				+ "or set the ANTIGRAVITY_HARNESS_PATH environment variable (or 'antigravity.harness.path' system property), "
				+ "or include the 'antigravity-sdk-harness' platform classifier dependency.");
	}
}
