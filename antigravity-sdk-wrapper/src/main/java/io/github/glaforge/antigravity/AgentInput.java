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
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents input to the agent.
 */
public sealed interface AgentInput permits AgentInput.Text, AgentInput.Media, AgentInput.SlashCommand {

	/**
	 * Represents a slash command input.
	 *
	 * @param name
	 *            the name of the slash command
	 */
	record SlashCommand(String name) implements AgentInput {
		/**
		 * Creates a new slash command input.
		 *
		 * @param name
		 *            the name of the slash command
		 * @return a SlashCommand instance
		 */
		public static SlashCommand of(String name) {
			return new SlashCommand(name);
		}
	}

	/**
	 * Represents a text input.
	 *
	 * @param text
	 *            the text content
	 */
	record Text(String text) implements AgentInput {
		/**
		 * Creates a new text input.
		 *
		 * @param text
		 *            the text content
		 * @return a Text instance
		 */
		public static Text of(String text) {
			return new Text(text);
		}
	}

	/**
	 * Represents a media input (e.g. image, document, audio, video).
	 */
	sealed interface Media extends AgentInput permits Image, Document, Audio, Video {
		/**
		 * Returns the MIME type of the media.
		 *
		 * @return the MIME type of the media
		 */
		String mimeType();
		/**
		 * Returns the byte data of the media.
		 *
		 * @return the byte data of the media
		 */
		byte[] data();
		/**
		 * Returns an optional description of the media.
		 *
		 * @return an optional description of the media
		 */
		String description();

		/**
		 * Normalizes a MIME type string by trimming whitespace and lowercasing.
		 *
		 * @param mimeType
		 *            raw MIME type string
		 * @return normalized MIME type string
		 */
		static String normalizeMimeType(String mimeType) {
			return mimeType != null ? mimeType.trim().toLowerCase(Locale.ROOT) : "";
		}

		/**
		 * Expands a leading {@code ~} user home prefix on the given path.
		 *
		 * @param path
		 *            raw path
		 * @return path with user home expanded if applicable
		 */
		static Path expandHome(Path path) {
			Objects.requireNonNull(path, "path must not be null");
			String pathStr = path.toString();
			if ("~".equals(pathStr)) {
				return Paths.get(System.getProperty("user.home"));
			}
			if (pathStr.startsWith("~/") || pathStr.startsWith("~\\")) {
				return Paths.get(System.getProperty("user.home"), pathStr.substring(2));
			}
			return path;
		}

		/**
		 * Creates a {@link Media} instance ({@link Image}, {@link Document},
		 * {@link Audio}, or {@link Video}) from raw bytes and a MIME type.
		 *
		 * @param data
		 *            raw media bytes
		 * @param mimeType
		 *            MIME type string (case-insensitive, whitespace-trimmed)
		 * @return matching {@link Media} instance
		 */
		static Media fromData(byte[] data, String mimeType) {
			return fromData(data, mimeType, null);
		}

		/**
		 * Creates a {@link Media} instance ({@link Image}, {@link Document},
		 * {@link Audio}, or {@link Video}) from raw bytes, a MIME type, and an optional
		 * description.
		 *
		 * @param data
		 *            raw media bytes
		 * @param mimeType
		 *            MIME type string (case-insensitive, whitespace-trimmed)
		 * @param description
		 *            optional description
		 * @return matching {@link Media} instance
		 */
		static Media fromData(byte[] data, String mimeType, String description) {
			Objects.requireNonNull(data, "data must not be null");
			Objects.requireNonNull(mimeType, "mimeType must not be null");
			String normalized = normalizeMimeType(mimeType);
			if (normalized.startsWith("image/")) {
				return new Image(normalized, data, description);
			}
			if (normalized.startsWith("audio/")) {
				return new Audio(normalized, data, description);
			}
			if (normalized.startsWith("video/")) {
				return new Video(normalized, data, description);
			}
			if (normalized.equals("application/pdf") || normalized.startsWith("text/")) {
				return new Document(normalized, data, description);
			}
			throw new IllegalArgumentException("Unsupported MIME type: '" + mimeType + "'");
		}
	}

	/**
	 * Represents an image input.
	 *
	 * @param mimeType
	 *            the MIME type
	 * @param data
	 *            the byte data
	 * @param description
	 *            the description
	 */
	record Image(String mimeType, byte[] data, String description) implements Media {
		/**
		 * Canonical constructor normalizing the MIME type.
		 */
		public Image {
			if (mimeType != null) {
				mimeType = Media.normalizeMimeType(mimeType);
			}
		}

		/**
		 * Creates an Image input from a file path string (expanding {@code ~}).
		 *
		 * @param path
		 *            the path string to the image
		 * @return an Image instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Image fromFile(String path) throws IOException {
			return fromFile(Paths.get(path), null);
		}

		/**
		 * Creates an Image input from a file path string (expanding {@code ~}) with a
		 * description.
		 *
		 * @param path
		 *            the path string to the image
		 * @param description
		 *            the description
		 * @return an Image instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Image fromFile(String path, String description) throws IOException {
			return fromFile(Paths.get(path), description);
		}

		/**
		 * Creates an Image input from a file path.
		 *
		 * @param path
		 *            the path to the image
		 * @return an Image instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Image fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		/**
		 * Creates an Image input from a file path with a description.
		 *
		 * @param path
		 *            the path to the image
		 * @param description
		 *            the description
		 * @return an Image instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Image fromFile(Path path, String description) throws IOException {
			Path resolved = Media.expandHome(path);
			String mimeType = Files.probeContentType(resolved);
			if (mimeType == null)
				mimeType = "image/jpeg";
			return new Image(mimeType, Files.readAllBytes(resolved), description);
		}
	}

	/**
	 * Represents a document input.
	 *
	 * @param mimeType
	 *            the MIME type
	 * @param data
	 *            the byte data
	 * @param description
	 *            the description
	 */
	record Document(String mimeType, byte[] data, String description) implements Media {
		/**
		 * Canonical constructor normalizing the MIME type.
		 */
		public Document {
			if (mimeType != null) {
				mimeType = Media.normalizeMimeType(mimeType);
			}
		}

		/**
		 * Creates a Document input from a file path string (expanding {@code ~}).
		 *
		 * @param path
		 *            the path string to the document
		 * @return a Document instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Document fromFile(String path) throws IOException {
			return fromFile(Paths.get(path), null);
		}

		/**
		 * Creates a Document input from a file path string (expanding {@code ~}) with a
		 * description.
		 *
		 * @param path
		 *            the path string to the document
		 * @param description
		 *            the description
		 * @return a Document instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Document fromFile(String path, String description) throws IOException {
			return fromFile(Paths.get(path), description);
		}

		/**
		 * Creates a Document input from a file path.
		 *
		 * @param path
		 *            the path to the document
		 * @return a Document instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Document fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		/**
		 * Creates a Document input from a file path with a description.
		 *
		 * @param path
		 *            the path to the document
		 * @param description
		 *            the description
		 * @return a Document instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Document fromFile(Path path, String description) throws IOException {
			Path resolved = Media.expandHome(path);
			String mimeType = Files.probeContentType(resolved);
			if (mimeType == null)
				mimeType = "application/pdf";
			return new Document(mimeType, Files.readAllBytes(resolved), description);
		}
	}

	/**
	 * Represents an audio input.
	 *
	 * @param mimeType
	 *            the MIME type
	 * @param data
	 *            the byte data
	 * @param description
	 *            the description
	 */
	record Audio(String mimeType, byte[] data, String description) implements Media {
		/**
		 * Canonical constructor normalizing the MIME type.
		 */
		public Audio {
			if (mimeType != null) {
				mimeType = Media.normalizeMimeType(mimeType);
			}
		}

		/**
		 * Creates an Audio input from a file path string (expanding {@code ~}).
		 *
		 * @param path
		 *            the path string to the audio
		 * @return an Audio instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Audio fromFile(String path) throws IOException {
			return fromFile(Paths.get(path), null);
		}

		/**
		 * Creates an Audio input from a file path string (expanding {@code ~}) with a
		 * description.
		 *
		 * @param path
		 *            the path string to the audio
		 * @param description
		 *            the description
		 * @return an Audio instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Audio fromFile(String path, String description) throws IOException {
			return fromFile(Paths.get(path), description);
		}

		/**
		 * Creates an Audio input from a file path.
		 *
		 * @param path
		 *            the path to the audio
		 * @return an Audio instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Audio fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		/**
		 * Creates an Audio input from a file path with a description.
		 *
		 * @param path
		 *            the path to the audio
		 * @param description
		 *            the description
		 * @return an Audio instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Audio fromFile(Path path, String description) throws IOException {
			Path resolved = Media.expandHome(path);
			String mimeType = Files.probeContentType(resolved);
			if (mimeType == null)
				mimeType = "audio/mpeg";
			return new Audio(mimeType, Files.readAllBytes(resolved), description);
		}
	}

	/**
	 * Represents a video input.
	 *
	 * @param mimeType
	 *            the MIME type
	 * @param data
	 *            the byte data
	 * @param description
	 *            the description
	 */
	record Video(String mimeType, byte[] data, String description) implements Media {
		/**
		 * Canonical constructor normalizing the MIME type.
		 */
		public Video {
			if (mimeType != null) {
				mimeType = Media.normalizeMimeType(mimeType);
			}
		}

		/**
		 * Creates a Video input from a file path string (expanding {@code ~}).
		 *
		 * @param path
		 *            the path string to the video
		 * @return a Video instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Video fromFile(String path) throws IOException {
			return fromFile(Paths.get(path), null);
		}

		/**
		 * Creates a Video input from a file path string (expanding {@code ~}) with a
		 * description.
		 *
		 * @param path
		 *            the path string to the video
		 * @param description
		 *            the description
		 * @return a Video instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Video fromFile(String path, String description) throws IOException {
			return fromFile(Paths.get(path), description);
		}

		/**
		 * Creates a Video input from a file path.
		 *
		 * @param path
		 *            the path to the video
		 * @return a Video instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Video fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		/**
		 * Creates a Video input from a file path with a description.
		 *
		 * @param path
		 *            the path to the video
		 * @param description
		 *            the description
		 * @return a Video instance
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public static Video fromFile(Path path, String description) throws IOException {
			Path resolved = Media.expandHome(path);
			String mimeType = Files.probeContentType(resolved);
			if (mimeType == null)
				mimeType = "video/mp4";
			return new Video(mimeType, Files.readAllBytes(resolved), description);
		}
	}
}
