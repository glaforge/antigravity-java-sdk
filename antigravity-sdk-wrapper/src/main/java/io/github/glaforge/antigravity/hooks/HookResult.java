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
 * @param allow
 *            true if the action is allowed, false if denied
 */
public record HookResult(boolean allow, String reason, String modifiedArgumentsJson) {
	public HookResult(boolean allow) {
		this(allow, null, null);
	}

	public HookResult(boolean allow, String reason) {
		this(allow, reason, null);
	}

	/**
	 * Returns a HookResult indicating the action is allowed.
	 *
	 * @return an allowed HookResult
	 */
	public static HookResult allowed() {
		return new HookResult(true, null, null);
	}

	/**
	 * Returns a HookResult indicating the action is allowed with modified
	 * arguments.
	 *
	 * @param modifiedArgumentsJson
	 *            the modified tool arguments JSON string
	 * @return an allowed HookResult with modified arguments
	 */
	public static HookResult allowedWithModifiedArguments(String modifiedArgumentsJson) {
		return new HookResult(true, null, modifiedArgumentsJson);
	}

	/**
	 * Returns a HookResult indicating the action is denied.
	 *
	 * @return a denied HookResult
	 */
	public static HookResult denied() {
		return new HookResult(false, null, null);
	}

	/**
	 * Returns a HookResult indicating the action is denied with a specific reason.
	 *
	 * @param reason
	 *            the reason for denial
	 * @return a denied HookResult with reason
	 */
	public static HookResult denied(String reason) {
		return new HookResult(false, reason, null);
	}

	/**
	 * Creates a new Builder for HookResult.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link HookResult}.
	 */
	public static class Builder {
		private boolean allow = true;
		private String reason;
		private String modifiedArgumentsJson;

		/** Default constructor. */
		public Builder() {
		}

		/**
		 * Sets whether the action is allowed.
		 *
		 * @param allow
		 *            true to allow, false to deny
		 * @return this builder
		 */
		public Builder allow(boolean allow) {
			this.allow = allow;
			return this;
		}

		/**
		 * Sets the reason for the decision.
		 *
		 * @param reason
		 *            the reason string
		 * @return this builder
		 */
		public Builder reason(String reason) {
			this.reason = reason;
			return this;
		}

		/**
		 * Sets modified tool arguments JSON.
		 *
		 * @param modifiedArgumentsJson
		 *            the modified JSON string
		 * @return this builder
		 */
		public Builder modifiedArgumentsJson(String modifiedArgumentsJson) {
			this.modifiedArgumentsJson = modifiedArgumentsJson;
			return this;
		}

		/**
		 * Builds and returns a new {@link HookResult}.
		 *
		 * @return the new HookResult instance
		 */
		public HookResult build() {
			return new HookResult(allow, reason, modifiedArgumentsJson);
		}
	}
}
