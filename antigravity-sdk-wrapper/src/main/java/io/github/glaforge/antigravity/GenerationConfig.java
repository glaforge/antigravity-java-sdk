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

import java.util.List;

/**
 * Configuration options for text generation.
 *
 * @param temperature
 *            the temperature
 * @param topP
 *            the topP value
 * @param topK
 *            the topK value
 * @param maxOutputTokens
 *            max output tokens
 * @param stopSequences
 *            the stop sequences
 */
public record GenerationConfig(Double temperature, Double topP, Integer topK, Integer maxOutputTokens,
		List<String> stopSequences) {
	/**
	 * Creates a new builder for GenerationConfig.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for GenerationConfig.
	 */
	public static class Builder {
		/**
		 * Default constructor.
		 */
		public Builder() {
		}
		private Double temperature;
		private Double topP;
		private Integer topK;
		private Integer maxOutputTokens;
		private List<String> stopSequences;

		/**
		 * Sets the temperature.
		 *
		 * @param temperature
		 *            the temperature
		 * @return this builder
		 */
		public Builder temperature(Double temperature) {
			this.temperature = temperature;
			return this;
		}

		/**
		 * Sets topP.
		 *
		 * @param topP
		 *            the topP value
		 * @return this builder
		 */
		public Builder topP(Double topP) {
			this.topP = topP;
			return this;
		}

		/**
		 * Sets topK.
		 *
		 * @param topK
		 *            the topK value
		 * @return this builder
		 */
		public Builder topK(Integer topK) {
			this.topK = topK;
			return this;
		}

		/**
		 * Sets maxOutputTokens.
		 *
		 * @param maxOutputTokens
		 *            max tokens
		 * @return this builder
		 */
		public Builder maxOutputTokens(Integer maxOutputTokens) {
			this.maxOutputTokens = maxOutputTokens;
			return this;
		}

		/**
		 * Sets stop sequences.
		 *
		 * @param stopSequences
		 *            the stop sequences
		 * @return this builder
		 */
		public Builder stopSequences(List<String> stopSequences) {
			this.stopSequences = stopSequences;
			return this;
		}

		/**
		 * Builds the GenerationConfig.
		 *
		 * @return the config
		 */
		public GenerationConfig build() {
			return new GenerationConfig(temperature, topP, topK, maxOutputTokens, stopSequences);
		}
	}
}
