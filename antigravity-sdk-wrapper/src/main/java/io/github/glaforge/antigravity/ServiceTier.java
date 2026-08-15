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
 * Service tier for Gemini model inference.
 */
public enum ServiceTier {
	/**
	 * Standard latency and availability service tier.
	 */
	STANDARD("standard"),

	/**
	 * Priority processing service tier for lower latency guarantees.
	 */
	PRIORITY("priority"),

	/**
	 * Flex pricing service tier for non-urgent tasks.
	 */
	FLEX("flex");

	private final String value;

	ServiceTier(String value) {
		this.value = value;
	}

	/**
	 * Returns the wire string value for this service tier.
	 *
	 * @return the string value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Parses a string value into a {@link ServiceTier}.
	 *
	 * @param tier
	 *            the string tier name (e.g. "standard", "priority", "flex")
	 * @return the matching ServiceTier, or null if unknown
	 */
	public static ServiceTier fromString(String tier) {
		if (tier == null) {
			return null;
		}
		for (ServiceTier st : values()) {
			if (st.value.equalsIgnoreCase(tier) || st.name().equalsIgnoreCase(tier)) {
				return st;
			}
		}
		return null;
	}
}
