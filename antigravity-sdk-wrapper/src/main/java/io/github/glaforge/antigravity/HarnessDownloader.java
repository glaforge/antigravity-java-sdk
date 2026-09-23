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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads and extracts the native localharness Go binary on-demand from
 * upstream PyPI wheels using standard Java 21 HTTP and streaming ZIP utilities.
 */
public class HarnessDownloader {

	private static final Logger log = LoggerFactory.getLogger(HarnessDownloader.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	/**
	 * Default upstream package version matching current protocol definitions.
	 */
	public static final String DEFAULT_UPSTREAM_VERSION = "0.1.18";

	private final HttpClient httpClient;
	private final String upstreamVersion;

	/**
	 * Creates a new downloader using default HTTP client and upstream version.
	 */
	public HarnessDownloader() {
		this(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(TIMEOUT).build(),
				DEFAULT_UPSTREAM_VERSION);
	}

	/**
	 * Creates a new downloader with customized HTTP client and upstream version.
	 *
	 * @param httpClient
	 *            the HTTP client to use
	 * @param upstreamVersion
	 *            the upstream version
	 */
	public HarnessDownloader(HttpClient httpClient, String upstreamVersion) {
		this.httpClient = httpClient;
		this.upstreamVersion = upstreamVersion;
	}

	/**
	 * Resolves the PyPI wheel URL for the specified platform slice.
	 *
	 * @param platformSlice
	 *            the platform slice (e.g. "osx-aarch64", "linux-x86_64")
	 * @return the resolved wheel download URL
	 * @throws IOException
	 *             if API lookup fails or no matching wheel is found
	 */
	public String resolveWheelUrl(String platformSlice) throws IOException {
		String versionUrl = "https://pypi.org/pypi/google-antigravity/" + upstreamVersion + "/json";
		String wheelUrl = queryWheelUrl(versionUrl, platformSlice);
		if (wheelUrl != null) {
			return wheelUrl;
		}

		// Fallback to latest release endpoint if specific version metadata is not found
		String latestUrl = "https://pypi.org/pypi/google-antigravity/json";
		wheelUrl = queryWheelUrl(latestUrl, platformSlice);
		if (wheelUrl != null) {
			return wheelUrl;
		}

		throw new FileNotFoundException("No upstream wheel found on PyPI for platform slice: " + platformSlice
				+ " (version: " + upstreamVersion + ")");
	}

	private String queryWheelUrl(String metadataUrl, String platformSlice) throws IOException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(metadataUrl)).timeout(TIMEOUT).GET().build();

		try {
			HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				log.debug("PyPI API returned status {} for URL {}", response.statusCode(), metadataUrl);
				return null;
			}

			JsonNode root = MAPPER.readTree(response.body());
			JsonNode urlsNode = root.path("urls");
			if (!urlsNode.isArray()) {
				return null;
			}

			String targetPlatform;
			String targetArch;
			switch (platformSlice) {
				case "linux-x86_64" -> {
					targetPlatform = "manylinux";
					targetArch = "x86_64";
				}
				case "linux-aarch64" -> {
					targetPlatform = "manylinux";
					targetArch = "aarch64";
				}
				case "osx-aarch64" -> {
					targetPlatform = "macosx";
					targetArch = "arm64";
				}
				case "osx-x86_64" -> {
					targetPlatform = "macosx";
					targetArch = "x86_64";
				}
				case "windows-x86_64" -> {
					targetPlatform = "win";
					targetArch = "amd64";
				}
				case "windows-aarch64" -> {
					targetPlatform = "win";
					targetArch = "arm64";
				}
				default -> throw new IllegalArgumentException("Unsupported platform slice: " + platformSlice);
			}

			for (JsonNode fileNode : urlsNode) {
				String filename = fileNode.path("filename").asText("");
				if (filename.endsWith(".whl") && filename.contains(targetPlatform) && filename.contains(targetArch)) {
					return fileNode.path("url").asText(null);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while querying PyPI for wheel metadata", e);
		}

		return null;
	}

	/**
	 * Downloads the wheel for the given platform slice, extracts the localharness
	 * binary into targetBinary, and marks it executable.
	 *
	 * @param platformSlice
	 *            the platform slice string
	 * @param targetBinary
	 *            the destination executable file
	 * @param baseDir
	 *            the directory containing the binary
	 * @throws IOException
	 *             if download or extraction fails
	 */
	public void downloadAndExtract(String platformSlice, File targetBinary, File baseDir) throws IOException {
		String wheelUrl = resolveWheelUrl(platformSlice);
		log.info("Downloading native localharness binary for {} from upstream wheel...", platformSlice);

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(wheelUrl)).timeout(Duration.ofMinutes(3)).GET()
				.build();

		try {
			HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				throw new IOException(
						"Failed to download wheel from " + wheelUrl + ", HTTP status: " + response.statusCode());
			}

			boolean isWindows = platformSlice.startsWith("windows");
			String ext = isWindows ? ".exe" : "";
			String binaryEntryName = "google/antigravity/bin/localharness" + ext;

			File tempFile = File.createTempFile("localharness-dl-", ext, baseDir);
			boolean found = false;

			try (ZipInputStream zis = new ZipInputStream(response.body())) {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					if (entry.getName().equals(binaryEntryName)) {
						Files.copy(zis, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						found = true;
						break;
					}
				}
			}

			if (!found) {
				tempFile.delete();
				throw new FileNotFoundException(
						"Entry '" + binaryEntryName + "' not found inside downloaded wheel: " + wheelUrl);
			}

			if (!tempFile.setExecutable(true)) {
				tempFile.delete();
				throw new IllegalStateException(
						"Failed to grant execution rights to downloaded binary: " + tempFile.getAbsolutePath());
			}

			try {
				Files.move(tempFile.toPath(), targetBinary.toPath(), StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tempFile.toPath(), targetBinary.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}

			// Write version stamp file
			File versionFile = new File(baseDir, ".version");
			Files.writeString(versionFile.toPath(), upstreamVersion);

			log.info("Successfully installed localharness {} to {}", upstreamVersion, targetBinary.getAbsolutePath());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while downloading native localharness binary", e);
		}
	}
}
