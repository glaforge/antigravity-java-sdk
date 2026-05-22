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

public class AgentResponse {
	private final String text;
	private final String thoughts;
	private final UsageMetadata usageMetadata;

	public AgentResponse(String text, String thoughts, UsageMetadata usageMetadata) {
		this.text = text != null ? text : "";
		this.thoughts = thoughts != null ? thoughts : "";
		this.usageMetadata = usageMetadata;
	}

	public String getText() {
		return text;
	}
	public String getThoughts() {
		return thoughts;
	}
	public UsageMetadata getUsageMetadata() {
		return usageMetadata;
	}

	public <T> T getStructuredOutput(Class<T> type) throws Exception {
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		return mapper.readValue(text, type);
	}

	@Override
	public String toString() {
		return text;
	}
}
