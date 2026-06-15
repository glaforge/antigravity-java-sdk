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
import io.github.glaforge.antigravity.SessionContext;

/**
 * A hook that is triggered when the agent's internal context is compacted. This
 * can be used to monitor context size or perform actions when history is
 * summarized.
 */
@FunctionalInterface
public interface OnCompactionHook extends InspectHook {
	/**
	 * Called when a compaction event occurs.
	 *
	 * @param stepData
	 *            data concerning the compacted step
	 * @param context
	 *            the session context for the current turn
	 * @return a CompletableFuture that completes when the hook is finished
	 */
	CompletableFuture<Void> onCompaction(Object stepData, SessionContext context);
}
