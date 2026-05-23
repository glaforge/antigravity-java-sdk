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

/**
 * Represents input to the agent.
 */
public sealed interface AgentInput permits AgentInput.Text, AgentInput.Media {

	/**
	 * Represents a text input.
	 *
	 * @param text the text content
	 */
	record Text(String text) implements AgentInput {
		/**
		 * Creates a new text input.
		 *
		 * @param text the text content
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
	}

	/**
	 * Represents an image input.
	 *
	 * @param mimeType the MIME type
	 * @param data the byte data
	 * @param description the description
	 */
	record Image(String mimeType, byte[] data, String description) implements Media {
		/**
		 * Creates an Image input from a file path.
		 *
		 * @param path the path to the image
		 * @return an Image instance
		 * @throws IOException if an I/O error occurs
		 */
		public static Image fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		/**
		 * Creates an Image input from a file path with a description.
		 *
		 * @param path the path to the image
		 * @param description the description
		 * @return an Image instance
		 * @throws IOException if an I/O error occurs
		 */
		public static Image fromFile(Path path, String description) throws IOException {
			String mimeType = Files.probeContentType(path);
			if (mimeType == null)
				mimeType = "image/jpeg";
			return new Image(mimeType, Files.readAllBytes(path), description);
		}
	}

	/**
	 * Represents a document input.
	 *
	 * @param mimeType the MIME type
	 * @param data the byte data
	 * @param description the description
	 */
	record Document(String mimeType, byte[] data, String description) implements Media {
		/**
		 * Creates a Document input from a file path.
		 *
		 * @param path the path to the document
		 * @return a Document instance
		 * @throws IOException if an I/O error occurs
		 */
		public static Document fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		/**
		 * Creates a Document input from a file path with a description.
		 *
		 * @param path the path to the document
		 * @param description the description
		 * @return a Document instance
		 * @throws IOException if an I/O error occurs
		 */
		public static Document fromFile(Path path, String description) throws IOException {
			String mimeType = Files.probeContentType(path);
			if (mimeType == null)
				mimeType = "application/pdf";
			return new Document(mimeType, Files.readAllBytes(path), description);
		}
	}

	/**
	 * Represents an audio input.
	 *
	 * @param mimeType the MIME type
	 * @param data the byte data
	 * @param description the description
	 */
	record Audio(String mimeType, byte[] data, String description) implements Media {
		/**
		 * Creates an Audio input from a file path.
		 *
		 * @param path the path to the audio
		 * @return an Audio instance
		 * @throws IOException if an I/O error occurs
		 */
		public static Audio fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		/**
		 * Creates an Audio input from a file path with a description.
		 *
		 * @param path the path to the audio
		 * @param description the description
		 * @return an Audio instance
		 * @throws IOException if an I/O error occurs
		 */
		public static Audio fromFile(Path path, String description) throws IOException {
			String mimeType = Files.probeContentType(path);
			if (mimeType == null)
				mimeType = "audio/mpeg";
			return new Audio(mimeType, Files.readAllBytes(path), description);
		}
	}

	/**
	 * Represents a video input.
	 *
	 * @param mimeType the MIME type
	 * @param data the byte data
	 * @param description the description
	 */
	record Video(String mimeType, byte[] data, String description) implements Media {
		/**
		 * Creates a Video input from a file path.
		 *
		 * @param path the path to the video
		 * @return a Video instance
		 * @throws IOException if an I/O error occurs
		 */
		public static Video fromFile(Path path) throws IOException {
			return fromFile(path, null);
		}
		/**
		 * Creates a Video input from a file path with a description.
		 *
		 * @param path the path to the video
		 * @param description the description
		 * @return a Video instance
		 * @throws IOException if an I/O error occurs
		 */
		public static Video fromFile(Path path, String description) throws IOException {
			String mimeType = Files.probeContentType(path);
			if (mimeType == null)
				mimeType = "video/mp4";
			return new Video(mimeType, Files.readAllBytes(path), description);
		}
	}
}
