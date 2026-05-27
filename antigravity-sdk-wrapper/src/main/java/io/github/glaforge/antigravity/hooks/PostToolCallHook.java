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
package io.github.glaforge.antigravity.hooks;

import io.github.glaforge.antigravity.SessionContext;
import java.util.concurrent.CompletableFuture;

/**
 * A hook executed after a tool call completes.
 */
@FunctionalInterface
public interface PostToolCallHook extends AgentHook {
	/**
	 * Called after a tool call is executed.
	 *
	 * @param toolCall
	 *            the tool call that was executed
	 * @param result
	 *            the result returned by the tool execution
	 * @param context
	 *            the session context for the current turn
	 * @return a CompletableFuture representing the asynchronous execution
	 */
	CompletableFuture<Void> onPostToolCall(ToolCall toolCall, Object result, SessionContext context);
}
