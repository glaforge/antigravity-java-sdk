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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;

/**
 * Represents the result of an authorization or validation hook.
 *
 * @param allow
 *            true if the action is allowed, false if denied
 * @param reason
 *            optional reason for denial or authorization note
 * @param modifiedArgumentsJson
 *            deprecated legacy modified tool call arguments JSON string
 * @param modifiedArgs
 *            optional structured modified tool arguments map
 */
public record HookResult(boolean allow, String reason, @Deprecated String modifiedArgumentsJson,
		Map<String, Object> modifiedArgs) {

	/**
	 * Canonical constructor with defensive map copying.
	 */
	public HookResult {
		modifiedArgs = modifiedArgs != null ? Map.copyOf(modifiedArgs) : null;
	}

	/**
	 * Convenience constructor setting allow status with no reason or modified
	 * arguments.
	 *
	 * @param allow
	 *            true if allowed
	 */
	public HookResult(boolean allow) {
		this(allow, null, null, null);
	}

	/**
	 * Convenience constructor setting allow status and reason.
	 *
	 * @param allow
	 *            true if allowed
	 * @param reason
	 *            reason string
	 */
	public HookResult(boolean allow, String reason) {
		this(allow, reason, null, null);
	}

	/**
	 * Compatibility constructor for legacy JSON string argument modification.
	 *
	 * @param allow
	 *            true if allowed
	 * @param reason
	 *            reason string
	 * @param modifiedArgumentsJson
	 *            legacy JSON string
	 * @deprecated Use {@link #HookResult(boolean, String, Map)} instead.
	 */
	@Deprecated
	public HookResult(boolean allow, String reason, String modifiedArgumentsJson) {
		this(allow, reason, modifiedArgumentsJson, null);
	}

	/**
	 * Constructor with structured modified arguments map.
	 *
	 * @param allow
	 *            true if allowed
	 * @param reason
	 *            reason string
	 * @param modifiedArgs
	 *            structured modified arguments
	 */
	public HookResult(boolean allow, String reason, Map<String, Object> modifiedArgs) {
		this(allow, reason, null, modifiedArgs);
	}

	/**
	 * Returns an unmodifiable view of modified arguments map.
	 *
	 * @return modified arguments map, or null if unmodified
	 */
	@Override
	public Map<String, Object> modifiedArgs() {
		return modifiedArgs != null ? Collections.unmodifiableMap(modifiedArgs) : null;
	}

	/**
	 * Returns a HookResult indicating the action is allowed.
	 *
	 * @return an allowed HookResult
	 */
	public static HookResult allowed() {
		return new HookResult(true, null, null, null);
	}

	/**
	 * Returns a HookResult indicating the action is allowed with structured
	 * modified arguments.
	 *
	 * @param modifiedArgs
	 *            the modified tool arguments map
	 * @return an allowed HookResult with modified arguments
	 */
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/**
	 * Returns a HookResult indicating the action is allowed with structured
	 * modified arguments.
	 *
	 * @param modifiedArgs
	 *            the modified tool arguments map
	 * @return an allowed HookResult with modified arguments
	 */
	public static HookResult allowedWithModifiedArgs(Map<String, Object> modifiedArgs) {
		return new HookResult(true, null, null, modifiedArgs);
	}

	/**
	 * Returns a HookResult indicating the action is allowed with structured
	 * modified arguments represented as a {@link JsonNode}.
	 *
	 * @param modifiedArgs
	 *            the modified tool arguments JsonNode
	 * @return an allowed HookResult with modified arguments
	 */
	public static HookResult allowedWithModifiedArgs(JsonNode modifiedArgs) {
		if (modifiedArgs == null) {
			return new HookResult(true, null, null, null);
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = OBJECT_MAPPER.convertValue(modifiedArgs, Map.class);
			return new HookResult(true, null, null, map);
		} catch (Exception e) {
			return new HookResult(true, null, modifiedArgs.toString(), null);
		}
	}

	/**
	 * Returns a HookResult indicating the action is allowed with modified
	 * arguments.
	 *
	 * @param modifiedArgumentsJson
	 *            the modified tool arguments JSON string
	 * @return an allowed HookResult with modified arguments
	 * @deprecated Use {@link #allowedWithModifiedArgs(Map)} or
	 *             {@link #allowedWithModifiedArgs(JsonNode)} instead.
	 */
	@Deprecated
	public static HookResult allowedWithModifiedArguments(String modifiedArgumentsJson) {
		return new HookResult(true, null, modifiedArgumentsJson, null);
	}

	/**
	 * Returns a HookResult indicating the action is denied.
	 *
	 * @return a denied HookResult
	 */
	public static HookResult denied() {
		return new HookResult(false, null, null, null);
	}

	/**
	 * Returns a HookResult indicating the action is denied with a specific reason.
	 *
	 * @param reason
	 *            the reason for denial
	 * @return a denied HookResult with reason
	 */
	public static HookResult denied(String reason) {
		return new HookResult(false, reason, null, null);
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
		private Map<String, Object> modifiedArgs;

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
		 * @deprecated Use {@link #modifiedArgs(Map)} instead.
		 */
		@Deprecated
		public Builder modifiedArgumentsJson(String modifiedArgumentsJson) {
			this.modifiedArgumentsJson = modifiedArgumentsJson;
			return this;
		}

		/**
		 * Sets structured modified tool arguments map.
		 *
		 * @param modifiedArgs
		 *            modified arguments map
		 * @return this builder
		 */
		public Builder modifiedArgs(Map<String, Object> modifiedArgs) {
			this.modifiedArgs = modifiedArgs;
			return this;
		}

		/**
		 * Sets structured modified tool call arguments from a {@link JsonNode}.
		 *
		 * @param modifiedArgs
		 *            modified arguments JsonNode
		 * @return this builder
		 */
		public Builder modifiedArgs(JsonNode modifiedArgs) {
			if (modifiedArgs == null) {
				this.modifiedArgs = null;
			} else {
				try {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = OBJECT_MAPPER.convertValue(modifiedArgs, Map.class);
					this.modifiedArgs = map;
				} catch (Exception e) {
					this.modifiedArgumentsJson = modifiedArgs.toString();
				}
			}
			return this;
		}

		/**
		 * Builds and returns a new {@link HookResult}.
		 *
		 * @return the new HookResult instance
		 */
		public HookResult build() {
			return new HookResult(allow, reason, modifiedArgumentsJson, modifiedArgs);
		}
	}
}
