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

/**
 * Exception thrown when a tool fails during execution, surfacing structured
 * error details.
 */
public class ToolExecutionError extends RuntimeException {

	/**
	 * Name of the failing tool.
	 */
	private final String toolName;

	/**
	 * Raw arguments passed to the tool as JSON.
	 */
	private final String argumentsJson;

	/**
	 * Optional server name for MCP tools.
	 */
	private final String serverName;

	/**
	 * Optional backend tool call ID.
	 */
	private final String callId;

	/**
	 * Optional trajectory step ID correlating with the step.
	 */
	private final String stepId;

	/**
	 * Creates a new ToolExecutionError with full structured details.
	 *
	 * @param toolName
	 *            the name of the tool that failed
	 * @param argumentsJson
	 *            the JSON representation of arguments passed to the tool
	 * @param serverName
	 *            optional MCP server name
	 * @param callId
	 *            optional tool call ID
	 * @param stepId
	 *            optional step ID correlating with the trajectory step
	 * @param message
	 *            descriptive error message
	 * @param cause
	 *            the underlying cause of the failure
	 */
	public ToolExecutionError(String toolName, String argumentsJson, String serverName, String callId, String stepId,
			String message, Throwable cause) {
		super(message != null ? message : "Tool execution failed for tool: " + toolName, cause);
		this.toolName = toolName;
		this.argumentsJson = argumentsJson;
		this.serverName = serverName;
		this.callId = callId;
		this.stepId = stepId;
	}

	/**
	 * Creates a new ToolExecutionError with tool name, arguments, message, and
	 * cause.
	 *
	 * @param toolName
	 *            the name of the tool that failed
	 * @param argumentsJson
	 *            the JSON representation of arguments passed to the tool
	 * @param message
	 *            descriptive error message
	 * @param cause
	 *            the underlying cause of the failure
	 */
	public ToolExecutionError(String toolName, String argumentsJson, String message, Throwable cause) {
		this(toolName, argumentsJson, null, null, null, message, cause);
	}

	/**
	 * Creates a new ToolExecutionError with tool name, arguments, and cause.
	 *
	 * @param toolName
	 *            the name of the tool that failed
	 * @param argumentsJson
	 *            the JSON representation of arguments passed to the tool
	 * @param cause
	 *            the underlying cause of the failure
	 */
	public ToolExecutionError(String toolName, String argumentsJson, Throwable cause) {
		this(toolName, argumentsJson, null, null, null, cause != null ? cause.getMessage() : null, cause);
	}

	/**
	 * Creates a new ToolExecutionError with tool name, arguments, and message.
	 *
	 * @param toolName
	 *            the name of the tool that failed
	 * @param argumentsJson
	 *            the JSON representation of arguments passed to the tool
	 * @param message
	 *            descriptive error message
	 */
	public ToolExecutionError(String toolName, String argumentsJson, String message) {
		this(toolName, argumentsJson, null, null, null, message, null);
	}

	/**
	 * Creates a new ToolExecutionError with tool name and arguments.
	 *
	 * @param toolName
	 *            the name of the tool that failed
	 * @param argumentsJson
	 *            the JSON representation of arguments passed to the tool
	 */
	public ToolExecutionError(String toolName, String argumentsJson) {
		this(toolName, argumentsJson, null, null, null, null, null);
	}

	/**
	 * Returns the name of the tool that failed execution.
	 *
	 * @return tool name
	 */
	public String getToolName() {
		return toolName;
	}

	/**
	 * Returns the arguments passed to the tool as a JSON string.
	 *
	 * @return arguments JSON
	 */
	public String getArgumentsJson() {
		return argumentsJson;
	}

	/**
	 * Returns the server name if this error originated from an MCP tool.
	 *
	 * @return MCP server name, or {@code null}
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * Returns the call ID correlating with this tool execution.
	 *
	 * @return tool call ID, or {@code null}
	 */
	public String getCallId() {
		return callId;
	}

	/**
	 * Returns the trajectory step ID correlating with this tool execution.
	 *
	 * @return step ID, or {@code null}
	 */
	public String getStepId() {
		return stepId;
	}
}
