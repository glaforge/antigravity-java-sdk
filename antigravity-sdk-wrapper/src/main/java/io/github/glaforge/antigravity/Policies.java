package io.github.glaforge.antigravity;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.Arrays;

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
	 */
	public static Policy allowAll() {
		return (toolName, arguments) -> Policy.Decision.ALLOW;
	}

	/**
	 * A policy that unconditionally denies all tool calls.
	 */
	public static Policy denyAll() {
		return (toolName, arguments) -> Policy.Decision.DENY;
	}

	/**
	 * A policy that unconditionally passes evaluation to the next policy.
	 */
	public static Policy passAll() {
		return (toolName, arguments) -> Policy.Decision.PASS;
	}

	/**
	 * Creates a policy that allows a specific tool.
	 * 
	 * @param targetToolName
	 *            the name of the tool to allow (e.g. "run_command")
	 */
	public static Policy allowTool(String targetToolName) {
		return (toolName, arguments) -> targetToolName.equals(toolName) ? Policy.Decision.ALLOW : Policy.Decision.PASS;
	}

	/**
	 * Creates a policy that denies a specific tool.
	 * 
	 * @param targetToolName
	 *            the name of the tool to deny (e.g. "run_command")
	 */
	public static Policy denyTool(String targetToolName) {
		return (toolName, arguments) -> targetToolName.equals(toolName) ? Policy.Decision.DENY : Policy.Decision.PASS;
	}

	/**
	 * Creates a policy that allows a list of specific tools.
	 * 
	 * @param targetToolNames
	 *            the names of the tools to allow
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
		boolean test(String toolName, JsonNode arguments);
	}
}
