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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
	private static final Duration METADATA_TIMEOUT = Duration.ofSeconds(15);
	private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(3);

	/**
	 * Default upstream package version matching current protocol definitions.
	 */
	public static final String DEFAULT_UPSTREAM_VERSION = "0.1.20";

	/**
	 * Set of supported platform slices.
	 */
	public static final Set<String> SUPPORTED_SLICES = Set.of("linux-x86_64", "linux-aarch64", "osx-aarch64",
			"osx-x86_64", "windows-x86_64", "windows-aarch64");

	static {
		// Prefer OS-level DNS address ordering on dual-stack IPv4/IPv6 networks
		if (System.getProperty("java.net.preferIPv6Addresses") == null) {
			System.setProperty("java.net.preferIPv6Addresses", "system");
		}
	}

	private final HttpClient httpClient;
	private final String upstreamVersion;
	private final Map<String, String> wheelUrlCache = new ConcurrentHashMap<>();

	/**
	 * Creates a new downloader using default HTTP client and upstream version.
	 */
	public HarnessDownloader() {
		this(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(METADATA_TIMEOUT)
				.build(), DEFAULT_UPSTREAM_VERSION);
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
		if (!SUPPORTED_SLICES.contains(platformSlice)) {
			throw new IllegalArgumentException("Unsupported platform slice: " + platformSlice);
		}

		String cached = wheelUrlCache.get(platformSlice);
		if (cached != null) {
			return cached;
		}

		String versionUrl = "https://pypi.org/pypi/google-antigravity/" + upstreamVersion + "/json";
		queryWheelUrls(versionUrl);
		cached = wheelUrlCache.get(platformSlice);
		if (cached != null) {
			return cached;
		}

		// Fallback to latest release endpoint if specific version metadata is not found
		String latestUrl = "https://pypi.org/pypi/google-antigravity/json";
		queryWheelUrls(latestUrl);
		cached = wheelUrlCache.get(platformSlice);
		if (cached != null) {
			return cached;
		}

		throw new FileNotFoundException("No upstream wheel found on PyPI for platform slice: " + platformSlice
				+ " (version: " + upstreamVersion + ")");
	}

	private void queryWheelUrls(String metadataUrl) throws IOException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(metadataUrl)).timeout(METADATA_TIMEOUT).GET()
				.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				log.debug("PyPI API returned status {} for URL {}", response.statusCode(), metadataUrl);
				return;
			}

			JsonNode root = MAPPER.readTree(response.body());
			JsonNode urlsNode = root.path("urls");
			if (!urlsNode.isArray()) {
				return;
			}

			for (JsonNode fileNode : urlsNode) {
				String filename = fileNode.path("filename").asText("");
				String url = fileNode.path("url").asText(null);
				if (url == null || !filename.endsWith(".whl")) {
					continue;
				}

				if (filename.contains("manylinux") && filename.contains("x86_64")) {
					wheelUrlCache.putIfAbsent("linux-x86_64", url);
				} else if (filename.contains("manylinux") && filename.contains("aarch64")) {
					wheelUrlCache.putIfAbsent("linux-aarch64", url);
				} else if (filename.contains("macosx") && filename.contains("arm64")) {
					wheelUrlCache.putIfAbsent("osx-aarch64", url);
				} else if (filename.contains("macosx") && filename.contains("x86_64")) {
					wheelUrlCache.putIfAbsent("osx-x86_64", url);
				} else if (filename.contains("win") && filename.contains("amd64")) {
					wheelUrlCache.putIfAbsent("windows-x86_64", url);
				} else if (filename.contains("win") && filename.contains("arm64")) {
					wheelUrlCache.putIfAbsent("windows-aarch64", url);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while querying PyPI for wheel metadata", e);
		}
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

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(wheelUrl)).timeout(DOWNLOAD_TIMEOUT).GET()
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
			boolean completed = false;
			try {
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
					throw new FileNotFoundException(
							"Entry '" + binaryEntryName + "' not found inside downloaded wheel: " + wheelUrl);
				}

				if (!tempFile.setExecutable(true)) {
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

				completed = true;
				log.info("Successfully installed localharness {} to {}", upstreamVersion,
						targetBinary.getAbsolutePath());
			} finally {
				if (!completed && tempFile.exists()) {
					tempFile.delete();
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while downloading native localharness binary", e);
		}
	}
}
