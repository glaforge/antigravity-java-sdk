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
 * Configuration for client-side logging and server-side distributed tracing
 * across connection strategies.
 *
 * @param enableServerSideTracing
 *            whether to enable server-side distributed tracing in the backend
 * @param loggingLevel
 *            logging level to apply across SDK modules (e.g. "DEBUG", "INFO",
 *            "WARN", "ERROR")
 */
public record DebugConfig(boolean enableServerSideTracing, String loggingLevel) {

	/**
	 * Default constructor enabling server-side tracing and DEBUG logging level.
	 */
	public DebugConfig() {
		this(true, "DEBUG");
	}

	/**
	 * Creates a default DebugConfig instance with default parameters.
	 *
	 * @return default DebugConfig
	 */
	public static DebugConfig defaults() {
		return new DebugConfig(true, "DEBUG");
	}
}
