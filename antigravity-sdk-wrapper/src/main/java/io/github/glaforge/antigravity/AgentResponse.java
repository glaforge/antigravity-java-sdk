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

import io.github.glaforge.antigravity.tools.SchemaGenerator;

/**
 * Represents the final response from the agent.
 *
 * @param text
 *            the text response
 * @param thoughts
 *            the thoughts of the agent
 * @param usageMetadata
 *            the usage metadata
 * @param stopReason
 *            the reason the execution turn stopped
 */
public record AgentResponse(String text, String thoughts, UsageMetadata usageMetadata, StopReason stopReason) {

	/**
	 * Compact constructor ensuring non-null defaults.
	 */
	public AgentResponse {
		text = text != null ? text : "";
		thoughts = thoughts != null ? thoughts : "";
		stopReason = stopReason != null ? stopReason : StopReason.UNSPECIFIED;
	}

	/**
	 * Backward-compatible constructor without explicit stop reason.
	 *
	 * @param text
	 *            the text response
	 * @param thoughts
	 *            the thoughts of the agent
	 * @param usageMetadata
	 *            the usage metadata
	 */
	public AgentResponse(String text, String thoughts, UsageMetadata usageMetadata) {
		this(text, thoughts, usageMetadata, StopReason.UNSPECIFIED);
	}

	/**
	 * Convenience alias for {@link #usageMetadata()}.
	 *
	 * @return the usage metadata
	 */
	public UsageMetadata usage() {
		return usageMetadata;
	}

	/**
	 * Parses the text response as JSON and maps it to the specified class.
	 *
	 * @param <T>
	 *            the type to return
	 * @param type
	 *            the class to map to
	 * @return the mapped object
	 * @throws Exception
	 *             if mapping fails
	 */
	public <T> T getStructuredOutput(Class<T> type) throws Exception {
		return SchemaGenerator.getMapper().readValue(text, type);
	}

	@Override
	public String toString() {
		return text;
	}
}
