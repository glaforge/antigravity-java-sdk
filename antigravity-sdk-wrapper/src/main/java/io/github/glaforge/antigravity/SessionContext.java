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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents the context of an ongoing agent session, allowing for state
 * storage and retrieval during the lifecycle of the session.
 */
public class SessionContext {
	/**
	 * Default constructor.
	 */
	public SessionContext() {
	}
	private final ConcurrentMap<String, Object> state = new ConcurrentHashMap<>();

	/**
	 * Gets a state value or returns a default.
	 *
	 * @param key
	 *            the state key
	 * @param defaultValue
	 *            the default value if not found
	 * @return the state value
	 */
	public Object get(String key, Object defaultValue) {
		return state.getOrDefault(key, defaultValue);
	}

	/**
	 * Sets a state value for the session.
	 *
	 * @param key
	 *            the state key
	 * @param value
	 *            the value
	 */
	public void set(String key, Object value) {
		state.put(key, value);
	}

	/**
	 * Returns the full state map.
	 *
	 * @return the state map
	 */
	public ConcurrentMap<String, Object> getStateMap() {
		return state;
	}
}
