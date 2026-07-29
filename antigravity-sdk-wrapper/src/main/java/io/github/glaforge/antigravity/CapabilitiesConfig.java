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

/**
 * Configuration options for agent capabilities.
 *
 * @param enableSubagents
 *            true if subagents are enabled
 * @param allowUserQuestions
 *            true if user questions are allowed
 * @param enableWebSearch
 *            true if web search is enabled
 * @param enableUrlReading
 *            true if URL reading is enabled
 * @param enableShell
 *            true if shell execution is enabled
 * @param enableViewFile
 *            true if file viewing is enabled
 * @param enableWriteFile
 *            true if file writing is enabled
 * @param enableFileEdit
 *            true if file editing is enabled
 * @param enableListDir
 *            true if list directory is enabled
 * @param enableGrepSearch
 *            true if grep search is enabled
 * @param enableGenerateImage
 *            true if image generation capability is enabled
 * @param imageModelName
 *            the image generation model name (defaults to
 *            "gemini-3.1-flash-lite-image")
 */
public record CapabilitiesConfig(boolean enableSubagents, boolean allowUserQuestions, boolean enableWebSearch,
		boolean enableUrlReading, boolean enableShell, boolean enableViewFile, boolean enableWriteFile,
		boolean enableFileEdit, boolean enableListDir, boolean enableGrepSearch, boolean enableGenerateImage,
		String imageModelName) {

	/**
	 * Secondary constructor for backward compatibility without image generation.
	 *
	 * @param enableSubagents
	 *            true if subagents are enabled
	 * @param allowUserQuestions
	 *            true if user questions are allowed
	 * @param enableWebSearch
	 *            true if web search is enabled
	 * @param enableUrlReading
	 *            true if URL reading is enabled
	 * @param enableShell
	 *            true if shell execution is enabled
	 * @param enableViewFile
	 *            true if file viewing is enabled
	 * @param enableWriteFile
	 *            true if file writing is enabled
	 * @param enableFileEdit
	 *            true if file editing is enabled
	 * @param enableListDir
	 *            true if list directory is enabled
	 * @param enableGrepSearch
	 *            true if grep search is enabled
	 */
	public CapabilitiesConfig(boolean enableSubagents, boolean allowUserQuestions, boolean enableWebSearch,
			boolean enableUrlReading, boolean enableShell, boolean enableViewFile, boolean enableWriteFile,
			boolean enableFileEdit, boolean enableListDir, boolean enableGrepSearch) {
		this(enableSubagents, allowUserQuestions, enableWebSearch, enableUrlReading, enableShell, enableViewFile,
				enableWriteFile, enableFileEdit, enableListDir, enableGrepSearch, false,
				AgentConfig.DEFAULT_IMAGE_GENERATION_MODEL);
	}

	/**
	 * Creates a new builder for CapabilitiesConfig.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for CapabilitiesConfig.
	 */
	public static class Builder {
		/**
		 * Default constructor.
		 */
		public Builder() {
		}

		private boolean enableSubagents = false;
		private boolean allowUserQuestions = false;
		private boolean enableWebSearch = false;
		private boolean enableUrlReading = false;
		private boolean enableShell = false;
		private boolean enableViewFile = false;
		private boolean enableWriteFile = false;
		private boolean enableFileEdit = false;
		private boolean enableListDir = false;
		private boolean enableGrepSearch = false;
		private boolean enableGenerateImage = false;
		private String imageModelName = AgentConfig.DEFAULT_IMAGE_GENERATION_MODEL;

		/**
		 * Enables or disables subagents.
		 *
		 * @param enableSubagents
		 *            true to enable
		 * @return this builder
		 */
		public Builder enableSubagents(boolean enableSubagents) {
			this.enableSubagents = enableSubagents;
			return this;
		}

		/**
		 * Enables or disables user questions.
		 *
		 * @param allowUserQuestions
		 *            true to allow
		 * @return this builder
		 */
		public Builder allowUserQuestions(boolean allowUserQuestions) {
			this.allowUserQuestions = allowUserQuestions;
			return this;
		}

		/**
		 * Enables or disables web search.
		 *
		 * @param enableWebSearch
		 *            true to enable
		 * @return this builder
		 */
		public Builder enableWebSearch(boolean enableWebSearch) {
			this.enableWebSearch = enableWebSearch;
			return this;
		}

		/**
		 * Enables or disables URL reading.
		 *
		 * @param enableUrlReading
		 *            true to enable
		 * @return this builder
		 */
		public Builder enableUrlReading(boolean enableUrlReading) {
			this.enableUrlReading = enableUrlReading;
			return this;
		}

		/**
		 * Enables or disables shell execution.
		 *
		 * @param enableShell
		 *            true to enable
		 * @return this builder
		 */
		public Builder enableShell(boolean enableShell) {
			this.enableShell = enableShell;
			return this;
		}

		/**
		 * Enables or disables file viewing.
		 *
		 * @param enableViewFile
		 *            true to enable
		 * @return this builder
		 */
		public Builder enableViewFile(boolean enableViewFile) {
			this.enableViewFile = enableViewFile;
			return this;
		}

		/**
		 * Enables or disables file writing.
		 *
		 * @param enableWriteFile
		 *            true to enable
		 * @return this builder
		 */
		public Builder enableWriteFile(boolean enableWriteFile) {
			this.enableWriteFile = enableWriteFile;
			return this;
		}

		/**
		 * Enables or disables file editing.
		 *
		 * @param enableFileEdit
		 *            true to enable
		 * @return this builder
		 */
		public Builder enableFileEdit(boolean enableFileEdit) {
			this.enableFileEdit = enableFileEdit;
			return this;
		}

		/**
		 * Enables or disables directory listing.
		 *
		 * @param enableListDir
		 *            true to enable
		 * @return this builder
		 */
		public Builder enableListDir(boolean enableListDir) {
			this.enableListDir = enableListDir;
			return this;
		}

		/**
		 * Enables or disables grep searching.
		 *
		 * @param enableGrepSearch
		 *            true to enable
		 * @return this builder
		 */
		public Builder enableGrepSearch(boolean enableGrepSearch) {
			this.enableGrepSearch = enableGrepSearch;
			return this;
		}

		/**
		 * Enables or disables image generation.
		 *
		 * @param enableGenerateImage
		 *            true to enable
		 * @return this builder
		 */
		public Builder enableGenerateImage(boolean enableGenerateImage) {
			this.enableGenerateImage = enableGenerateImage;
			return this;
		}

		/**
		 * Sets the image generation model name.
		 *
		 * @param imageModelName
		 *            the model name
		 * @return this builder
		 */
		public Builder imageModelName(String imageModelName) {
			this.imageModelName = imageModelName;
			return this;
		}

		/**
		 * Builds the CapabilitiesConfig.
		 *
		 * @return the config
		 */
		public CapabilitiesConfig build() {
			return new CapabilitiesConfig(enableSubagents, allowUserQuestions, enableWebSearch, enableUrlReading,
					enableShell, enableViewFile, enableWriteFile, enableFileEdit, enableListDir, enableGrepSearch,
					enableGenerateImage, imageModelName);
		}
	}
}
