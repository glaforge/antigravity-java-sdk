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
 * Behavior mode of the agent.
 */
public enum AgentBehavior {
	/**
	 * Agent behaves autonomously.
	 */
	AUTONOMOUS,

	/**
	 * Agent operates in interactive mode.
	 */
	INTERACTIVE;

	/**
	 * Parses a string value into an {@link AgentBehavior}.
	 *
	 * @param value
	 *            the string behavior value (e.g. "AUTONOMOUS", "INTERACTIVE")
	 * @return the matching AgentBehavior, or null if unknown
	 */
	public static AgentBehavior fromString(String value) {
		if (value == null) {
			return null;
		}
		for (AgentBehavior behavior : AgentBehavior.values()) {
			if (behavior.name().equalsIgnoreCase(value)) {
				return behavior;
			}
		}
		return null;
	}
}
