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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Defines a security policy for tool execution.
 * Policies can inspect the tool name and arguments to decide whether execution is allowed.
 */
public interface Policy {
	/**
	 * The decision reached by the policy.
	 */
	enum Decision {
		/** Indicates the tool is allowed. */
		ALLOW, 
		/** Indicates the tool is denied. */
		DENY, 
		/** Indicates the policy defers to other policies. */
		PASS
	}

	/**
	 * Evaluates whether a tool call should be allowed based on its name and arguments.
	 *
	 * @param toolName the name of the tool being called
	 * @param arguments the arguments passed to the tool
	 * @return the policy decision
	 */
	Decision evaluate(String toolName, JsonNode arguments);
}
