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
import io.github.glaforge.antigravity.localharness.StopArgs;

/**
 * A hook that is triggered when the agent's execution is externally requested
 * to stop. Enables custom cleanup, telemetry logging, or resource reclamation
 * upon termination.
 */
@FunctionalInterface
public interface OnStopHook extends InspectHook {
	/**
	 * Called when a stop event occurs.
	 *
	 * @param stopArgs
	 *            arguments detailing why the agent was stopped
	 * @param context
	 *            the session context for the current turn
	 * @return a CompletableFuture that completes when the hook is finished
	 */
	CompletableFuture<Void> onStop(StopArgs stopArgs, SessionContext context);
}
