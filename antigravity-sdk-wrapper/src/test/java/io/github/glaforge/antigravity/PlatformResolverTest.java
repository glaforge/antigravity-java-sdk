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
import java.nio.file.Files;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class PlatformResolverTest {

	@Test
	public void testGetPlatformSlice() {
		String slice = PlatformResolver.getPlatformSlice();
		assertNotNull(slice);
		assertTrue(slice.contains("-"), "Platform slice should contain hyphen: " + slice);
		assertTrue(slice.startsWith("osx-") || slice.startsWith("linux-") || slice.startsWith("windows-"),
				"Slice should match a known OS prefix: " + slice);
	}

	@Test
	public void testCustomHarnessPathOverride() throws IOException {
		File tempBinary = File.createTempFile("mock-harness-", ".sh");
		tempBinary.deleteOnExit();
		Files.writeString(tempBinary.toPath(), "#!/bin/sh\necho mock\n");
		assertTrue(tempBinary.setExecutable(true));

		String originalProp = System.getProperty("antigravity.harness.path");
		try {
			System.setProperty("antigravity.harness.path", tempBinary.getAbsolutePath());
			File resolved = PlatformResolver.resolveBinary();
			assertNotNull(resolved);
			assertEquals(tempBinary.getAbsolutePath(), resolved.getAbsolutePath());
		} finally {
			if (originalProp != null) {
				System.setProperty("antigravity.harness.path", originalProp);
			} else {
				System.clearProperty("antigravity.harness.path");
			}
		}
	}

	@Test
	public void testCustomHarnessPathNonExistent() {
		String originalProp = System.getProperty("antigravity.harness.path");
		try {
			System.setProperty("antigravity.harness.path", "/non/existent/path/to/harness");
			assertThrows(FileNotFoundException.class, PlatformResolver::resolveBinary);
		} finally {
			if (originalProp != null) {
				System.setProperty("antigravity.harness.path", originalProp);
			} else {
				System.clearProperty("antigravity.harness.path");
			}
		}
	}

	@Test
	public void testResolveBinaryClasspath() throws IOException {
		File binary = PlatformResolver.resolveBinary();
		assertNotNull(binary);
		assertTrue(binary.exists(), "Resolved binary must exist on disk");
		assertTrue(binary.canExecute(), "Resolved binary must be executable");
	}
}
