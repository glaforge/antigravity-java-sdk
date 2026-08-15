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
 * Configuration for session-level budget limits and caps.
 *
 * @param maxModelCalls
 *            maximum number of model invocations permitted across the session
 * @param maxToolCalls
 *            maximum number of tool invocations permitted across the session
 * @param maxInputTokens
 *            maximum net uncached input tokens permitted across the session
 * @param maxOutputTokens
 *            maximum output tokens permitted across the session (candidates +
 *            thoughts)
 * @param maxTotalTokens
 *            maximum total net tokens permitted across the session
 */
public record BudgetConfig(Integer maxModelCalls, Integer maxToolCalls, Long maxInputTokens, Long maxOutputTokens,
		Long maxTotalTokens) {

	/**
	 * Creates a new builder for {@link BudgetConfig}.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link BudgetConfig}.
	 */
	public static class Builder {
		private Integer maxModelCalls;
		private Integer maxToolCalls;
		private Long maxInputTokens;
		private Long maxOutputTokens;
		private Long maxTotalTokens;

		/**
		 * Default constructor.
		 */
		public Builder() {
		}

		/**
		 * Sets the maximum number of model invocations permitted across the session.
		 *
		 * @param maxModelCalls
		 *            the maximum model calls
		 * @return this builder
		 */
		public Builder maxModelCalls(int maxModelCalls) {
			this.maxModelCalls = maxModelCalls;
			return this;
		}

		/**
		 * Sets the maximum number of tool invocations permitted across the session.
		 *
		 * @param maxToolCalls
		 *            the maximum tool calls
		 * @return this builder
		 */
		public Builder maxToolCalls(int maxToolCalls) {
			this.maxToolCalls = maxToolCalls;
			return this;
		}

		/**
		 * Sets the maximum net uncached input tokens permitted across the session.
		 *
		 * @param maxInputTokens
		 *            the maximum input tokens
		 * @return this builder
		 */
		public Builder maxInputTokens(long maxInputTokens) {
			this.maxInputTokens = maxInputTokens;
			return this;
		}

		/**
		 * Sets the maximum output tokens permitted across the session (candidates +
		 * thoughts).
		 *
		 * @param maxOutputTokens
		 *            the maximum output tokens
		 * @return this builder
		 */
		public Builder maxOutputTokens(long maxOutputTokens) {
			this.maxOutputTokens = maxOutputTokens;
			return this;
		}

		/**
		 * Sets the maximum total net tokens permitted across the session.
		 *
		 * @param maxTotalTokens
		 *            the maximum total tokens
		 * @return this builder
		 */
		public Builder maxTotalTokens(long maxTotalTokens) {
			this.maxTotalTokens = maxTotalTokens;
			return this;
		}

		/**
		 * Builds the {@link BudgetConfig} instance.
		 *
		 * @return a new BudgetConfig
		 */
		public BudgetConfig build() {
			return new BudgetConfig(maxModelCalls, maxToolCalls, maxInputTokens, maxOutputTokens, maxTotalTokens);
		}
	}
}
