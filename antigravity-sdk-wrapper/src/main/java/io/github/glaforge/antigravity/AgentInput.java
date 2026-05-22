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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public sealed interface AgentInput permits AgentInput.Text, AgentInput.Media {

	record Text(String text) implements AgentInput {
		public static Text of(String text) {
			return new Text(text);
		}
	}

	sealed interface Media extends AgentInput permits Image, Document, Audio, Video {
		String mimeType();
		byte[] data();
		String description();
	}

	record Image(String mimeType, byte[] data, String description) implements Media {
		public static Image fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		public static Image fromFile(Path path, String description) throws IOException {
			String mimeType = Files.probeContentType(path);
			if (mimeType == null)
				mimeType = "image/jpeg";
			return new Image(mimeType, Files.readAllBytes(path), description);
		}
	}

	record Document(String mimeType, byte[] data, String description) implements Media {
		public static Document fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		public static Document fromFile(Path path, String description) throws IOException {
			String mimeType = Files.probeContentType(path);
			if (mimeType == null)
				mimeType = "application/pdf";
			return new Document(mimeType, Files.readAllBytes(path), description);
		}
	}

	record Audio(String mimeType, byte[] data, String description) implements Media {
		public static Audio fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		public static Audio fromFile(Path path, String description) throws IOException {
			String mimeType = Files.probeContentType(path);
			if (mimeType == null)
				mimeType = "audio/mpeg";
			return new Audio(mimeType, Files.readAllBytes(path), description);
		}
	}

	record Video(String mimeType, byte[] data, String description) implements Media {
		public static Video fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		public static Video fromFile(Path path, String description) throws IOException {
			String mimeType = Files.probeContentType(path);
			if (mimeType == null)
				mimeType = "video/mp4";
			return new Video(mimeType, Files.readAllBytes(path), description);
		}
	}
}
