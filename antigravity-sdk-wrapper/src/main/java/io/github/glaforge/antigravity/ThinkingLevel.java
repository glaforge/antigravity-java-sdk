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
 * Represents the thinking/reasoning severity level for model inference.
 */
public enum ThinkingLevel {
	/**
	 * Thinking disabled or off.
	 */
	OFF("off"),

	/**
	 * Minimal thinking budget.
	 */
	MINIMAL("minimal"),

	/**
	 * Low thinking budget.
	 */
	LOW("low"),

	/**
	 * Medium thinking budget.
	 */
	MEDIUM("medium"),

	/**
	 * High thinking budget.
	 */
	HIGH("high"),

	/**
	 * Extra-high thinking budget for complex reasoning tasks.
	 */
	EXTRA_HIGH("extra_high");

	private final String value;

	ThinkingLevel(String value) {
		this.value = value;
	}

	/**
	 * Returns the string value of the thinking level.
	 *
	 * @return the string value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Parses a string into a ThinkingLevel.
	 *
	 * @param text
	 *            the text value
	 * @return the matching ThinkingLevel, or null if unknown
	 */
	public static ThinkingLevel fromValue(String text) {
		if (text == null) {
			return null;
		}
		for (ThinkingLevel level : ThinkingLevel.values()) {
			if (level.value.equalsIgnoreCase(text) || level.name().equalsIgnoreCase(text)) {
				return level;
			}
		}
		return null;
	}
}
