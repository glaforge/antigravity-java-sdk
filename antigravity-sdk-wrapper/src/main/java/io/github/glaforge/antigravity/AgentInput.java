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
