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

import java.util.concurrent.ConcurrentMap;

/**
 * Context object injected into tool executions providing state and session
 * communication.
 */
public interface ToolContext {
	/**
	 * Returns the active conversation ID.
	 *
	 * @return the conversation ID
	 */
	String getConversationId();

	/**
	 * Checks if the agent is idle.
	 *
	 * @return true if idle
	 */
	boolean isIdle();

	/**
	 * Sends a message back to the active conversation.
	 *
	 * @param message
	 *            the message to send
	 */
	void send(String message);

	/**
	 * Gets a state value or returns a default.
	 *
	 * @param key
	 *            the state key
	 * @param defaultValue
	 *            the default value if not found
	 * @return the state value
	 */
	Object getState(String key, Object defaultValue);

	/**
	 * Sets a state value for the session.
	 *
	 * @param key
	 *            the state key
	 * @param value
	 *            the value
	 */
	void setState(String key, Object value);

	/**
	 * Returns the full state map.
	 *
	 * @return the state map
	 */
	ConcurrentMap<String, Object> getStateMap();
}
