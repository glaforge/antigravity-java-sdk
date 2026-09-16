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
 * OS command sandbox (exebox) status reported by the harness.
 *
 * @param available
 *            whether the sandbox actually enforces isolation. When
 *            {@code false}, {@code run_command} executes unsandboxed even if
 *            {@code enableSandbox} was requested.
 * @param unavailableReason
 *            human-readable explanation when {@code available} is
 *            {@code false}; {@code null} when the sandbox is available.
 */
public record SandboxStatus(boolean available, String unavailableReason) {

	/**
	 * Convenience constructor for available status without reason.
	 *
	 * @param available
	 *            whether the sandbox is available
	 */
	public SandboxStatus(boolean available) {
		this(available, null);
	}
}
