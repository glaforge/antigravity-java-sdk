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

/**
 * Represents the result of an authorization or validation hook.
 *
 * @param allow true if the action is allowed, false if denied
 */
public record HookResult(boolean allow) {
	/**
	 * Returns a HookResult indicating the action is allowed.
	 *
	 * @return an allowed HookResult
	 */
	public static HookResult allowed() {
		return new HookResult(true);
	}
	/**
	 * Returns a HookResult indicating the action is denied.
	 *
	 * @return a denied HookResult
	 */
	public static HookResult denied() {
		return new HookResult(false);
	}
}
