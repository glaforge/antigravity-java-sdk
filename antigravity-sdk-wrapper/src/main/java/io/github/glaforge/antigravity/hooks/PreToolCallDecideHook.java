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

import java.util.concurrent.CompletableFuture;

/**
 * A hook executed before a tool call is decided upon.
 */
@FunctionalInterface
public interface PreToolCallDecideHook extends AgentHook {
	/**
	 * Called before a tool is executed to decide if it should proceed.
	 *
	 * @param toolCall
	 *            the tool call to evaluate
	 * @return a CompletableFuture containing the HookResult for the decision
	 */
	CompletableFuture<HookResult> onPreToolCallDecide(ToolCall toolCall);
}
