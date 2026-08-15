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
 * Modality discriminator for multimodal token usage breakdown.
 */
public enum Modality {
	/**
	 * Textual prompt or completion tokens.
	 */
	TEXT,

	/**
	 * Image input tokens.
	 */
	IMAGE,

	/**
	 * Video input tokens.
	 */
	VIDEO,

	/**
	 * Audio input tokens.
	 */
	AUDIO,

	/**
	 * Document input tokens.
	 */
	DOCUMENT;

	/**
	 * Parses a string value into a {@link Modality}.
	 *
	 * @param value
	 *            the string modality value (e.g. "TEXT", "IMAGE", "AUDIO")
	 * @return the matching Modality, or null if unknown
	 */
	public static Modality fromString(String value) {
		if (value == null) {
			return null;
		}
		for (Modality modality : Modality.values()) {
			if (modality.name().equalsIgnoreCase(value)) {
				return modality;
			}
		}
		return null;
	}
}
