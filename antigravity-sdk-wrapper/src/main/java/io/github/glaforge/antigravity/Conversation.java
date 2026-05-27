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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Represents an active stateful conversation session with the local harness.
 */
public class Conversation implements TriggerContext {
	private final Agent agent;

	Conversation(Agent agent) {
		this.agent = agent;
	}

	/**
	 * Returns the unique ID of the conversation.
	 *
	 * @return the conversation ID
	 */
	public String getConversationId() {
		return agent.getConversationId();
	}

	/**
	 * Sends a chat message and waits for the full response.
	 *
	 * @param prompt
	 *            the text prompt to send
	 * @return a CompletableFuture containing the response
	 */
	public CompletableFuture<AgentResponse> chat(String prompt) {
		return agent.chat(prompt);
	}

	/**
	 * Sends a structured chat message with multiple inputs and waits for the full
	 * response.
	 *
	 * @param prompt
	 *            the list of inputs to send
	 * @return a CompletableFuture containing the response
	 */
	public CompletableFuture<AgentResponse> chat(List<AgentInput> prompt) {
		return agent.chat(prompt);
	}

	/**
	 * Sends a structured chat message with multiple inputs and waits for the full
	 * response.
	 *
	 * @param prompt
	 *            the inputs to send
	 * @return a CompletableFuture containing the response
	 */
	public CompletableFuture<AgentResponse> chat(AgentInput... prompt) {
		return agent.chat(List.of(prompt));
	}

	/**
	 * Sends a chat message and returns a publisher for streaming response chunks.
	 *
	 * @param prompt
	 *            the text prompt to send
	 * @return a Publisher of AgentResponseChunk
	 */
	public Flow.Publisher<AgentResponseChunk> chatStream(String prompt) {
		return chatStream(List.of(AgentInput.Text.of(prompt)));
	}

	/**
	 * Sends a structured chat message and returns a publisher for streaming
	 * response chunks.
	 *
	 * @param prompt
	 *            the list of inputs to send
	 * @return a Publisher of AgentResponseChunk
	 */
	public Flow.Publisher<AgentResponseChunk> chatStream(List<AgentInput> prompt) {
		SubmissionPublisher<AgentResponseChunk> publisher = new SubmissionPublisher<>();
		agent.chatStream(prompt, publisher::submit).whenComplete((response, error) -> {
			if (error != null) {
				publisher.closeExceptionally(error);
			} else {
				publisher.close();
			}
		});
		return publisher;
	}

	/**
	 * Returns the usage metadata from the most recent turn.
	 *
	 * @return the usage metadata
	 */
	public UsageMetadata getUsageMetadata() {
		return agent.getUsageMetadata();
	}

	@Override
	public void fireTrigger(String triggerText) {
		agent.fireTrigger(triggerText);
	}
}
