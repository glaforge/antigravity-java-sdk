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
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class HarnessDownloaderTest {

	@Test
	public void testResolveWheelUrls() throws IOException {
		HarnessDownloader downloader = new HarnessDownloader();

		String[] slices = {"linux-x86_64", "linux-aarch64", "osx-aarch64", "osx-x86_64", "windows-x86_64",
				"windows-aarch64"};

		for (String slice : slices) {
			String url = downloader.resolveWheelUrl(slice);
			assertNotNull(url, "Wheel URL for " + slice + " should not be null");
			assertTrue(url.startsWith("https://"), "Wheel URL should start with https://: " + url);
			assertTrue(url.endsWith(".whl"), "Wheel URL should end with .whl: " + url);
			assertTrue(url.contains("google_antigravity"), "Wheel URL should contain google_antigravity: " + url);
		}
	}

	@Test
	public void testResolveInvalidSlice() {
		HarnessDownloader downloader = new HarnessDownloader();
		assertThrows(IllegalArgumentException.class, () -> downloader.resolveWheelUrl("solaris-sparc"));
	}

	@Test
	@Tag("integration")
	public void testDownloadAndExtractLive() throws IOException {
		String slice = PlatformResolver.getPlatformSlice();
		File tempDir = Files.createTempDirectory("harness-dl-test-").toFile();
		tempDir.deleteOnExit();

		boolean isWindows = slice.startsWith("windows");
		String ext = isWindows ? ".exe" : "";
		File targetBinary = new File(tempDir, "localharness" + ext);

		HarnessDownloader downloader = new HarnessDownloader();
		downloader.downloadAndExtract(slice, targetBinary, tempDir);

		assertTrue(targetBinary.exists(), "Downloaded binary should exist");
		assertTrue(targetBinary.canExecute(), "Downloaded binary should be executable");
		assertTrue(targetBinary.length() > 50_000_000, "Downloaded binary should be > 50MB");

		File versionFile = new File(tempDir, ".version");
		assertTrue(versionFile.exists(), "Version file should exist");
		assertEquals(HarnessDownloader.DEFAULT_UPSTREAM_VERSION, Files.readString(versionFile.toPath()).trim());
	}
}
