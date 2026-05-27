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
import java.util.Set;
import java.util.Arrays;
import java.util.function.BiPredicate;

/**
 * Pre-packaged policies to control autonomous agent behavior and simplify
 * setup.
 */
public final class Policies {

	private Policies() {
		// Prevent instantiation
	}

	/**
	 * A policy that unconditionally allows all tool calls. WARNING: Use with
	 * caution. This gives the agent completely unrestricted access to the file
	 * system and command execution.
	 *
	 * @return a policy that allows all tool calls
	 */
	public static Policy allowAll() {
		return (toolName, arguments) -> Policy.Decision.ALLOW;
	}

	/**
	 * A policy that unconditionally denies all tool calls.
	 *
	 * @return a policy that denies all tool calls
	 */
	public static Policy denyAll() {
		return (toolName, arguments) -> Policy.Decision.DENY;
	}

	/**
	 * A policy that unconditionally passes evaluation to the next policy.
	 *
	 * @return a policy that defers the decision
	 */
	public static Policy passAll() {
		return (toolName, arguments) -> Policy.Decision.PASS;
	}

	/**
	 * Creates a policy that allows a specific tool.
	 * 
	 * @param targetToolName
	 *            the name of the tool to allow (e.g. "run_command")
	 * @return a policy allowing the specified tool
	 */
	public static Policy allowTool(String targetToolName) {
		return (toolName, arguments) -> targetToolName.equals(toolName) ? Policy.Decision.ALLOW : Policy.Decision.PASS;
	}

	/**
	 * Creates a policy that denies a specific tool.
	 * 
	 * @param targetToolName
	 *            the name of the tool to deny (e.g. "run_command")
	 * @return a policy denying the specified tool
	 */
	public static Policy denyTool(String targetToolName) {
		return (toolName, arguments) -> targetToolName.equals(toolName) ? Policy.Decision.DENY : Policy.Decision.PASS;
	}

	/**
	 * Creates a policy that allows a list of specific tools.
	 * 
	 * @param targetToolNames
	 *            the names of the tools to allow
	 * @return a policy allowing the specified tools
	 */
	public static Policy allowTools(String... targetToolNames) {
		Set<String> tools = Set.copyOf(Arrays.asList(targetToolNames));
		return (toolName, arguments) -> tools.contains(toolName) ? Policy.Decision.ALLOW : Policy.Decision.PASS;
	}

	/**
	 * Creates a policy that denies a list of specific tools.
	 * 
	 * @param targetToolNames
	 *            the names of the tools to deny
	 * @return a policy denying the specified tools
	 */
	public static Policy denyTools(String... targetToolNames) {
		Set<String> tools = Set.copyOf(Arrays.asList(targetToolNames));
		return (toolName, arguments) -> tools.contains(toolName) ? Policy.Decision.DENY : Policy.Decision.PASS;
	}

	/**
	 * Creates a policy that allows tool execution if a condition is met.
	 * 
	 * @param condition
	 *            a predicate evaluating the tool name and its arguments
	 * @return a policy conditionally allowing tool execution
	 */
	public static Policy allowIf(PolicyCondition condition) {
		return (toolName,
				arguments) -> condition.test(toolName, arguments) ? Policy.Decision.ALLOW : Policy.Decision.PASS;
	}

	/**
	 * Creates a policy that denies tool execution if a condition is met.
	 * 
	 * @param condition
	 *            a predicate evaluating the tool name and its arguments
	 * @return a policy conditionally denying tool execution
	 */
	public static Policy denyIf(PolicyCondition condition) {
		return (toolName,
				arguments) -> condition.test(toolName, arguments) ? Policy.Decision.DENY : Policy.Decision.PASS;
	}

	/**
	 * Functional interface for evaluating custom policy logic.
	 */
	@FunctionalInterface
	public interface PolicyCondition {
		/**
		 * Evaluates the policy condition.
		 *
		 * @param toolName
		 *            the name of the tool
		 * @param arguments
		 *            the arguments provided to the tool
		 * @return true if the condition is met
		 */
		boolean test(String toolName, JsonNode arguments);
	}

	/**
	 * Creates a policy that delegates the decision to the user via a callback.
	 *
	 * @param prompter
	 *            a BiPredicate taking the tool name and arguments, and returning
	 *            true to allow or false to deny.
	 * @return a policy that asks the user for confirmation
	 */
	public static Policy askUser(BiPredicate<String, JsonNode> prompter) {
		return (toolName,
				arguments) -> prompter.test(toolName, arguments) ? Policy.Decision.ALLOW : Policy.Decision.DENY;
	}

	/**
	 * Creates a policy that asks the user for confirmation only when the agent
	 * tries to run a command.
	 *
	 * @param prompter
	 *            a BiPredicate taking the tool name and arguments, and returning
	 *            true to allow or false to deny.
	 * @return a policy that asks the user before running a command
	 */
	public static Policy confirmRunCommand(BiPredicate<String, JsonNode> prompter) {
		return (toolName, arguments) -> {
			if ("run_command".equals(toolName)) {
				return prompter.test(toolName, arguments) ? Policy.Decision.ALLOW : Policy.Decision.DENY;
			}
			return Policy.Decision.PASS;
		};
	}

	/**
	 * Creates a policy that asks the user for confirmation when the agent tries to
	 * run a command or edit a file.
	 *
	 * @param prompter
	 *            a BiPredicate taking the tool name and arguments, and returning
	 *            true to allow or false to deny.
	 * @return a policy that asks the user before running a command or editing a
	 *         file
	 */
	public static Policy confirmRunCommandOrFileEdit(BiPredicate<String, JsonNode> prompter) {
		return (toolName, arguments) -> {
			if ("run_command".equals(toolName) || "file_edit".equals(toolName)
					|| "replace_file_content".equals(toolName) || "multi_replace_file_content".equals(toolName)
					|| "write_to_file".equals(toolName)) {
				return prompter.test(toolName, arguments) ? Policy.Decision.ALLOW : Policy.Decision.DENY;
			}
			return Policy.Decision.PASS;
		};
	}
}
