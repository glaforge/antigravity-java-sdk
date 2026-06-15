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

import io.github.glaforge.antigravity.hooks.ToolCall;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

/**
 * A reactive stream representing an active agent response. This provides
 * distinct publishers for chunks, thoughts, and tool calls, allowing UI layers
 * to bind precisely to the data they need.
 *
 * @param chunks
 *            the publisher of combined text and thought deltas
 * @param thoughts
 *            the publisher of just the model's internal reasoning
 * @param toolCalls
 *            the publisher of tool call dispatch events
 * @param result
 *            a future containing the final, aggregated agent response
 */
public record AgentStream(Publisher<AgentResponseChunk> chunks, Publisher<String> thoughts,
		Publisher<ToolCall> toolCalls, CompletableFuture<AgentResponse> result) {
}
