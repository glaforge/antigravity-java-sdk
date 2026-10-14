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
package io.github.glaforge.antigravity.beta;

import io.github.glaforge.antigravity.AgentResponse;
import io.github.glaforge.antigravity.UsageMetadata;

/**
 * Final result returned by {@link BetaOperations#runWorkflow}.
 *
 * Provides workflow-specific attributes ({@code scriptPath}, {@code script},
 * {@code description}, {@code output}, {@code responseText}) alongside the
 * turn-level {@link AgentResponse} accessors.
 *
 * @param scriptPath
 *            path to the workflow script executed (when invoked with a script
 *            file path)
 * @param script
 *            inline workflow script source (when invoked with inline script
 *            source)
 * @param description
 *            brief description of the workflow run
 * @param output
 *            formatted workflow log and result text produced by the workflow
 *            script
 * @param responseText
 *            final assistant response text from the workflow turn
 * @param response
 *            underlying turn-level {@link AgentResponse}
 */
@Beta
public record WorkflowResult(String scriptPath, String script, String description, String output, String responseText,
		AgentResponse response) {

	/**
	 * Compact constructor normalizing null fields.
	 */
	public WorkflowResult {
		scriptPath = scriptPath != null ? scriptPath : "";
		script = script != null ? script : "";
		description = description != null ? description : "";
		output = output != null ? output : "";
		responseText = responseText != null ? responseText : (response != null ? response.text() : "");
		if (response == null) {
			response = new AgentResponse(responseText, "", null);
		}
	}

	/**
	 * Convenience constructor without an explicit {@link AgentResponse}.
	 *
	 * @param scriptPath
	 *            executed script path
	 * @param script
	 *            inline script source
	 * @param description
	 *            workflow description
	 * @param output
	 *            formatted workflow output
	 * @param responseText
	 *            assistant response text
	 */
	public WorkflowResult(String scriptPath, String script, String description, String output, String responseText) {
		this(scriptPath, script, description, output, responseText, new AgentResponse(responseText, "", null));
	}

	/**
	 * Returns the final assistant text response.
	 *
	 * @return assistant response text
	 */
	public String text() {
		return response.text();
	}

	/**
	 * Returns the thoughts from the workflow turn.
	 *
	 * @return thoughts text
	 */
	public String thoughts() {
		return response.thoughts();
	}

	/**
	 * Returns the turn-level usage metadata.
	 *
	 * @return usage metadata, or null if unavailable
	 */
	public UsageMetadata usageMetadata() {
		return response.usageMetadata();
	}

	/**
	 * Convenience alias for {@link #usageMetadata()}.
	 *
	 * @return usage metadata
	 */
	public UsageMetadata usage() {
		return response.usage();
	}

	/**
	 * Parses the response text as JSON and maps it to the specified class.
	 *
	 * @param <T>
	 *            target type
	 * @param type
	 *            class to deserialize into
	 * @return deserialized structured output
	 * @throws Exception
	 *             if parsing fails
	 */
	public <T> T getStructuredOutput(Class<T> type) throws Exception {
		return response.getStructuredOutput(type);
	}
}
