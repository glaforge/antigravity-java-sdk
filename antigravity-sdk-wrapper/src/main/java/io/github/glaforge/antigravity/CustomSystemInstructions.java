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

import java.util.Objects;

/**
 * Replacement system instructions that override the default system prompt,
 * optionally specifying a custom section title.
 *
 * @param text
 *            the replacement prompt text
 * @param title
 *            optional prompt-section title; renders the text under
 *            {@code <title>} XML tags instead of the generic {@code <System>}
 *            section
 */
public record CustomSystemInstructions(String text, String title) {

	/**
	 * Compact constructor validating non-null text.
	 */
	public CustomSystemInstructions {
		Objects.requireNonNull(text, "text must not be null");
	}

	/**
	 * Creates custom system instructions without a specific title.
	 *
	 * @param text
	 *            the replacement prompt text
	 */
	public CustomSystemInstructions(String text) {
		this(text, null);
	}

	/**
	 * Factory method for custom system instructions without a custom title.
	 *
	 * @param text
	 *            the replacement prompt text
	 * @return a new CustomSystemInstructions instance
	 */
	public static CustomSystemInstructions of(String text) {
		return new CustomSystemInstructions(text, null);
	}

	/**
	 * Factory method for custom system instructions with a dedicated section title.
	 *
	 * @param text
	 *            the replacement prompt text
	 * @param title
	 *            the prompt-section title
	 * @return a new CustomSystemInstructions instance
	 */
	public static CustomSystemInstructions of(String text, String title) {
		return new CustomSystemInstructions(text, title);
	}
}
