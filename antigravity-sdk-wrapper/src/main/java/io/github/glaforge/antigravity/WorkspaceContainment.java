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
 * Workspace containment policy enforcing filesystem boundary constraints.
 */
public enum WorkspaceContainment {

	/**
	 * Unspecified workspace containment policy (uses backend default).
	 */
	UNSPECIFIED,

	/**
	 * Enforces that file access and command executions remain strictly within the
	 * configured workspace boundaries.
	 */
	ENABLED,

	/**
	 * Disables workspace boundary restrictions, allowing unrestricted filesystem
	 * operations.
	 */
	DISABLED;

	/**
	 * Parses a string into a {@link WorkspaceContainment} enum value,
	 * case-insensitively.
	 *
	 * @param value
	 *            the string to parse
	 * @return matching WorkspaceContainment, or {@code null} if null or
	 *         unrecognized
	 */
	public static WorkspaceContainment fromString(String value) {
		if (value == null) {
			return null;
		}
		try {
			return WorkspaceContainment.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
