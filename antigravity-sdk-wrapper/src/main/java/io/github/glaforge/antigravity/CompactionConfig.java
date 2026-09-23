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
 * Configuration for conversation trajectory compaction and context limits.
 *
 * Antigravity manages context by compacting older conversation history when the
 * active trajectory exceeds {@code tokenThreshold}.
 *
 * @param tokenThreshold
 *            token ceiling allowed for conversation history before compaction
 *            occurs
 * @param checkpointIntervalTokens
 *            optional checkpoint interval in tokens between incremental
 *            compaction passes
 * @param maxContextTokens
 *            optional absolute maximum context token limit
 */
public record CompactionConfig(Integer tokenThreshold, Integer checkpointIntervalTokens, Integer maxContextTokens) {

	/**
	 * Creates a CompactionConfig with only a token threshold.
	 *
	 * @param tokenThreshold
	 *            token ceiling allowed before compaction
	 */
	public CompactionConfig(Integer tokenThreshold) {
		this(tokenThreshold, null, null);
	}

	/**
	 * Creates a new CompactionConfig with the specified token threshold.
	 *
	 * @param tokenThreshold
	 *            token threshold limit
	 * @return a new CompactionConfig instance
	 */
	public static CompactionConfig of(int tokenThreshold) {
		return new CompactionConfig(tokenThreshold, null, null);
	}

	/**
	 * Creates a new builder for {@link CompactionConfig}.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link CompactionConfig}.
	 */
	public static class Builder {
		private Integer tokenThreshold;
		private Integer checkpointIntervalTokens;
		private Integer maxContextTokens;

		/**
		 * Default constructor.
		 */
		public Builder() {
		}

		/**
		 * Sets the token threshold before compaction is triggered.
		 *
		 * @param tokenThreshold
		 *            token threshold
		 * @return this builder
		 */
		public Builder tokenThreshold(int tokenThreshold) {
			this.tokenThreshold = tokenThreshold;
			return this;
		}

		/**
		 * Sets the checkpoint interval tokens.
		 *
		 * @param checkpointIntervalTokens
		 *            checkpoint interval tokens
		 * @return this builder
		 * @deprecated Upstream v0.1.18 simplified CompactionConfig to a single
		 *             tokenThreshold dial.
		 */
		@Deprecated
		public Builder checkpointIntervalTokens(int checkpointIntervalTokens) {
			this.checkpointIntervalTokens = checkpointIntervalTokens;
			return this;
		}

		/**
		 * Sets the maximum context tokens.
		 *
		 * @param maxContextTokens
		 *            max context tokens
		 * @return this builder
		 * @deprecated Upstream v0.1.18 simplified CompactionConfig to a single
		 *             tokenThreshold dial.
		 */
		@Deprecated
		public Builder maxContextTokens(int maxContextTokens) {
			this.maxContextTokens = maxContextTokens;
			return this;
		}

		/**
		 * Builds the {@link CompactionConfig} instance.
		 *
		 * @return a new CompactionConfig
		 */
		public CompactionConfig build() {
			return new CompactionConfig(tokenThreshold, checkpointIntervalTokens, maxContextTokens);
		}
	}
}
