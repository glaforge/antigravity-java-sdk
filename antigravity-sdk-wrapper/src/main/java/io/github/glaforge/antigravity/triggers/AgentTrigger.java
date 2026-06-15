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
package io.github.glaforge.antigravity.triggers;

import io.github.glaforge.antigravity.TriggerContext;

/**
 * Represents a background trigger that can asynchronously supply information to
 * an active agent session.
 */
public interface AgentTrigger {
	/**
	 * Called when the agent session begins. The trigger should start its background
	 * work here and use the provided context to fire events.
	 *
	 * @param context
	 *            the context to use for firing triggers
	 */
	void start(TriggerContext context);

	/**
	 * Called when the agent session is closing. The trigger should halt all
	 * background execution and release resources.
	 */
	void stop();
}
