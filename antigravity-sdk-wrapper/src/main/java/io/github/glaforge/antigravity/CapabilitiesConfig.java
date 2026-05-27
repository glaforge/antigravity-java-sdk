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
public record CapabilitiesConfig(boolean enableSubagents, boolean allowUserQuestions) {
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
		 * Builds the CapabilitiesConfig.
		 *
		 * @return the config
		 */
		public CapabilitiesConfig build() {
			return new CapabilitiesConfig(enableSubagents, allowUserQuestions);
		}
	}
}
