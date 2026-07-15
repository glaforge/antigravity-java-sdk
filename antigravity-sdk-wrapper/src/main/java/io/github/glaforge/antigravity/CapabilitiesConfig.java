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
 */
public record CapabilitiesConfig(boolean enableSubagents, boolean allowUserQuestions, boolean enableWebSearch,
		boolean enableUrlReading, boolean enableShell, boolean enableViewFile, boolean enableWriteFile,
		boolean enableFileEdit, boolean enableListDir, boolean enableGrepSearch) {

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

		public Builder enableWriteFile(boolean enableWriteFile) {
			this.enableWriteFile = enableWriteFile;
			return this;
		}

		public Builder enableFileEdit(boolean enableFileEdit) {
			this.enableFileEdit = enableFileEdit;
			return this;
		}

		public Builder enableListDir(boolean enableListDir) {
			this.enableListDir = enableListDir;
			return this;
		}

		public Builder enableGrepSearch(boolean enableGrepSearch) {
			this.enableGrepSearch = enableGrepSearch;
			return this;
		}

		/**
		 * Builds the CapabilitiesConfig.
		 *
		 * @return the config
		 */
		public CapabilitiesConfig build() {
			return new CapabilitiesConfig(enableSubagents, allowUserQuestions, enableWebSearch, enableUrlReading,
					enableShell, enableViewFile, enableWriteFile, enableFileEdit, enableListDir, enableGrepSearch);
		}
	}
}
