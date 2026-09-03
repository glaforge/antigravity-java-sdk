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
 * Configuration for the builtin {@code run_command} tool execution behavior.
 *
 * @param enableDaemons
 *            whether long-running daemon background tasks are permitted
 * @param timeoutSeconds
 *            optional maximum timeout in seconds for command execution (must be
 *            strictly positive when specified)
 * @param enableSandbox
 *            whether commands should execute within an isolated sandbox
 *            environment
 */
public record RunCommandConfig(boolean enableDaemons, Double timeoutSeconds, boolean enableSandbox) {

	/**
	 * Secondary constructor for backward compatibility without sandbox setting.
	 *
	 * @param enableDaemons
	 *            whether long-running daemon background tasks are permitted
	 * @param timeoutSeconds
	 *            optional maximum timeout in seconds for command execution
	 */
	public RunCommandConfig(boolean enableDaemons, Double timeoutSeconds) {
		this(enableDaemons, timeoutSeconds, false);
	}

	/**
	 * Compact constructor validating timeout parameters.
	 */
	public RunCommandConfig {
		if (timeoutSeconds != null && timeoutSeconds <= 0) {
			throw new IllegalArgumentException("timeoutSeconds must be strictly greater than 0");
		}
	}

	/**
	 * Creates a default RunCommandConfig instance with daemons and sandbox disabled
	 * and no custom timeout override.
	 *
	 * @return default configuration
	 */
	public static RunCommandConfig defaults() {
		return new RunCommandConfig(false, null, false);
	}

	/**
	 * Creates a new builder for configuring {@link RunCommandConfig}.
	 *
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Fluent builder for {@link RunCommandConfig}.
	 */
	public static final class Builder {
		private boolean enableDaemons = false;
		private Double timeoutSeconds;
		private boolean enableSandbox = false;

		/**
		 * Creates a new builder instance.
		 */
		public Builder() {
		}

		/**
		 * Configures whether long-running daemon commands are allowed.
		 *
		 * @param enableDaemons
		 *            {@code true} to permit daemon tasks, {@code false} otherwise
		 * @return this builder
		 */
		public Builder enableDaemons(boolean enableDaemons) {
			this.enableDaemons = enableDaemons;
			return this;
		}

		/**
		 * Configures the maximum command timeout in seconds.
		 *
		 * @param timeoutSeconds
		 *            timeout in seconds (must be &gt; 0)
		 * @return this builder
		 */
		public Builder timeoutSeconds(Double timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
			return this;
		}

		/**
		 * Configures whether command execution runs in an isolated sandbox.
		 *
		 * @param enableSandbox
		 *            {@code true} to enable command sandboxing, {@code false} otherwise
		 * @return this builder
		 */
		public Builder enableSandbox(boolean enableSandbox) {
			this.enableSandbox = enableSandbox;
			return this;
		}

		/**
		 * Builds a new immutable {@link RunCommandConfig} instance.
		 *
		 * @return new RunCommandConfig
		 */
		public RunCommandConfig build() {
			return new RunCommandConfig(enableDaemons, timeoutSeconds, enableSandbox);
		}
	}
}
