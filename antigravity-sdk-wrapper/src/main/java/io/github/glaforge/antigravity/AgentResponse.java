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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the final response from the agent.
 */
public class AgentResponse {
	private final String text;
	private final String thoughts;
	private final UsageMetadata usageMetadata;

	/**
	 * Constructs an AgentResponse.
	 *
	 * @param text
	 *            the text response
	 * @param thoughts
	 *            the thoughts of the agent
	 * @param usageMetadata
	 *            the usage metadata
	 */
	public AgentResponse(String text, String thoughts, UsageMetadata usageMetadata) {
		this.text = text != null ? text : "";
		this.thoughts = thoughts != null ? thoughts : "";
		this.usageMetadata = usageMetadata;
	}

	/**
	 * Returns the text response.
	 *
	 * @return the text response
	 */
	public String getText() {
		return text;
	}
	/**
	 * Returns the thoughts of the agent.
	 *
	 * @return the thoughts of the agent
	 */
	public String getThoughts() {
		return thoughts;
	}
	/**
	 * Returns the usage metadata.
	 *
	 * @return the usage metadata
	 */
	public UsageMetadata getUsageMetadata() {
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
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(text, type);
	}

	@Override
	public String toString() {
		return text;
	}
}
