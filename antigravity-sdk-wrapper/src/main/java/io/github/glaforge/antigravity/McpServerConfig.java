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

import java.util.List;
import java.util.Map;

/**
 * Configuration for connecting to an external Model Context Protocol (MCP)
 * server.
 */
public sealed interface McpServerConfig permits McpServerConfig.StdioMcpServerConfig,
		McpServerConfig.SseMcpServerConfig, McpServerConfig.HttpMcpServerConfig {

	/**
	 * Defines the transport mechanism for the MCP server connection.
	 */
	enum TransportType {
		/** Standard Input/Output transport. */
		STDIO,
		/** Server-Sent Events transport. */
		SSE,
		/** HTTP / Streamable HTTP transport. */
		HTTP
	}

	/**
	 * Gets the transport type of this configuration.
	 * 
	 * @return The transport type.
	 */
	TransportType type();

	/**
	 * Returns the optional server name.
	 *
	 * @return server name, or null if not set
	 */
	String name();

	/**
	 * Returns the optional connection/call timeout in seconds.
	 *
	 * @return timeout in seconds, or null if not set
	 */
	Double timeoutSeconds();

	/**
	 * Returns the optional allowlist of enabled MCP tool names.
	 *
	 * @return enabled tool names, or null if not set
	 */
	List<String> enabledTools();

	/**
	 * Returns the optional denylist of disabled MCP tool names.
	 *
	 * @return disabled tool names, or null if not set
	 */
	List<String> disabledTools();

	/**
	 * Returns whether all tools from this MCP server should be eagerly loaded into
	 * the model's context instead of lazily loaded.
	 *
	 * @return true if all tools should be forced eager
	 */
	boolean forceAllToolsEager();

	/**
	 * Validates common MCP server configuration options.
	 */
	private static void validateCommon(Double timeoutSeconds, List<String> enabledTools, List<String> disabledTools) {
		if (enabledTools != null && disabledTools != null) {
			throw new IllegalArgumentException("Cannot specify both enabledTools and disabledTools");
		}
		if (timeoutSeconds != null && timeoutSeconds <= 0) {
			throw new IllegalArgumentException("timeoutSeconds must be positive, got: " + timeoutSeconds);
		}
	}

	/**
	 * Creates an MCP Server configuration that communicates over Standard
	 * Input/Output.
	 * 
	 * @param command
	 *            The executable command (e.g. "python3", "npx", "node").
	 * @param args
	 *            The arguments to pass to the command.
	 * @return A new StdioMcpServerConfig instance.
	 */
	static StdioMcpServerConfig stdio(String command, List<String> args) {
		return new StdioMcpServerConfig(command, args);
	}

	/**
	 * Creates an MCP Server configuration that connects over Server-Sent Events
	 * (SSE).
	 * 
	 * @param url
	 *            The SSE endpoint URL.
	 * @return A new SseMcpServerConfig instance.
	 */
	static SseMcpServerConfig sse(String url) {
		return new SseMcpServerConfig(url, null);
	}

	/**
	 * Creates an MCP Server configuration that connects over Server-Sent Events
	 * (SSE) with headers.
	 * 
	 * @param url
	 *            The SSE endpoint URL.
	 * @param headers
	 *            HTTP headers to send (e.g. for authentication).
	 * @return A new SseMcpServerConfig instance.
	 */
	static SseMcpServerConfig sse(String url, Map<String, String> headers) {
		return new SseMcpServerConfig(url, headers);
	}

	/**
	 * Creates an MCP Server configuration that connects over HTTP / Streamable
	 * HTTP.
	 * 
	 * @param url
	 *            The HTTP endpoint URL.
	 * @return A new HttpMcpServerConfig instance.
	 */
	static HttpMcpServerConfig http(String url) {
		return new HttpMcpServerConfig(url, null);
	}

	/**
	 * Creates an MCP Server configuration that connects over HTTP / Streamable HTTP
	 * with headers.
	 * 
	 * @param url
	 *            The HTTP endpoint URL.
	 * @param headers
	 *            HTTP headers to send (e.g. for authentication).
	 * @return A new HttpMcpServerConfig instance.
	 */
	static HttpMcpServerConfig http(String url, Map<String, String> headers) {
		return new HttpMcpServerConfig(url, headers);
	}

	/**
	 * Creates an MCP Server configuration that connects over Streamable HTTP.
	 * 
	 * @param url
	 *            The HTTP endpoint URL.
	 * @return A new HttpMcpServerConfig instance.
	 */
	static HttpMcpServerConfig streamableHttp(String url) {
		return new HttpMcpServerConfig(url, null);
	}

	/**
	 * Creates an MCP Server configuration that connects over Streamable HTTP with
	 * headers.
	 * 
	 * @param url
	 *            The HTTP endpoint URL.
	 * @param headers
	 *            HTTP headers to send (e.g. for authentication).
	 * @return A new HttpMcpServerConfig instance.
	 */
	static HttpMcpServerConfig streamableHttp(String url, Map<String, String> headers) {
		return new HttpMcpServerConfig(url, headers);
	}

	/**
	 * Configuration for an MCP server that communicates over Standard Input/Output.
	 *
	 * @param name
	 *            Optional logical server name.
	 * @param command
	 *            The executable command.
	 * @param args
	 *            The arguments to pass to the command.
	 * @param env
	 *            Optional environment variables for the subprocess.
	 * @param timeoutSeconds
	 *            Optional timeout in seconds.
	 * @param enabledTools
	 *            Optional allowlist of tool names.
	 * @param disabledTools
	 *            Optional denylist of tool names.
	 * @param forceAllToolsEager
	 *            Whether to force all tools from this server to be loaded eagerly.
	 */
	record StdioMcpServerConfig(String name, String command, List<String> args, Map<String, String> env,
			Double timeoutSeconds, List<String> enabledTools, List<String> disabledTools,
			boolean forceAllToolsEager) implements McpServerConfig {

		public StdioMcpServerConfig {
			validateCommon(timeoutSeconds, enabledTools, disabledTools);
			args = args != null ? List.copyOf(args) : List.of();
			env = env != null ? Map.copyOf(env) : null;
			enabledTools = enabledTools != null ? List.copyOf(enabledTools) : null;
			disabledTools = disabledTools != null ? List.copyOf(disabledTools) : null;
		}

		/**
		 * Backward-compatible constructor with command and args.
		 */
		public StdioMcpServerConfig(String command, List<String> args) {
			this(null, command, args, null, null, null, null, false);
		}

		public StdioMcpServerConfig withName(String name) {
			return new StdioMcpServerConfig(name, command, args, env, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public StdioMcpServerConfig withEnv(Map<String, String> env) {
			return new StdioMcpServerConfig(name, command, args, env, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public StdioMcpServerConfig withTimeoutSeconds(Double timeoutSeconds) {
			return new StdioMcpServerConfig(name, command, args, env, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public StdioMcpServerConfig withEnabledTools(List<String> enabledTools) {
			return new StdioMcpServerConfig(name, command, args, env, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public StdioMcpServerConfig withDisabledTools(List<String> disabledTools) {
			return new StdioMcpServerConfig(name, command, args, env, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public StdioMcpServerConfig withForceAllToolsEager(boolean forceAllToolsEager) {
			return new StdioMcpServerConfig(name, command, args, env, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		@Override
		public TransportType type() {
			return TransportType.STDIO;
		}
	}

	/**
	 * Configuration for an MCP server that connects over Server-Sent Events (SSE).
	 *
	 * @param name
	 *            Optional logical server name.
	 * @param url
	 *            The SSE endpoint URL.
	 * @param headers
	 *            HTTP headers to send.
	 * @param timeoutSeconds
	 *            Optional timeout in seconds.
	 * @param enabledTools
	 *            Optional allowlist of tool names.
	 * @param disabledTools
	 *            Optional denylist of tool names.
	 * @param forceAllToolsEager
	 *            Whether to force all tools from this server to be loaded eagerly.
	 */
	record SseMcpServerConfig(String name, String url, Map<String, String> headers, Double timeoutSeconds,
			List<String> enabledTools, List<String> disabledTools,
			boolean forceAllToolsEager) implements McpServerConfig {

		public SseMcpServerConfig {
			validateCommon(timeoutSeconds, enabledTools, disabledTools);
			headers = headers != null ? Map.copyOf(headers) : null;
			enabledTools = enabledTools != null ? List.copyOf(enabledTools) : null;
			disabledTools = disabledTools != null ? List.copyOf(disabledTools) : null;
		}

		/**
		 * Backward-compatible constructor with url and headers.
		 */
		public SseMcpServerConfig(String url, Map<String, String> headers) {
			this(null, url, headers, null, null, null, false);
		}

		public SseMcpServerConfig withName(String name) {
			return new SseMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public SseMcpServerConfig withHeaders(Map<String, String> headers) {
			return new SseMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public SseMcpServerConfig withTimeoutSeconds(Double timeoutSeconds) {
			return new SseMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public SseMcpServerConfig withEnabledTools(List<String> enabledTools) {
			return new SseMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public SseMcpServerConfig withDisabledTools(List<String> disabledTools) {
			return new SseMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public SseMcpServerConfig withForceAllToolsEager(boolean forceAllToolsEager) {
			return new SseMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		@Override
		public TransportType type() {
			return TransportType.SSE;
		}
	}

	/**
	 * Configuration for an MCP server that connects over HTTP / Streamable HTTP.
	 *
	 * @param name
	 *            Optional logical server name.
	 * @param url
	 *            The HTTP endpoint URL.
	 * @param headers
	 *            HTTP headers to send.
	 * @param timeoutSeconds
	 *            Optional timeout in seconds.
	 * @param enabledTools
	 *            Optional allowlist of tool names.
	 * @param disabledTools
	 *            Optional denylist of tool names.
	 * @param forceAllToolsEager
	 *            Whether to force all tools from this server to be loaded eagerly.
	 */
	record HttpMcpServerConfig(String name, String url, Map<String, String> headers, Double timeoutSeconds,
			List<String> enabledTools, List<String> disabledTools,
			boolean forceAllToolsEager) implements McpServerConfig {

		public HttpMcpServerConfig {
			validateCommon(timeoutSeconds, enabledTools, disabledTools);
			headers = headers != null ? Map.copyOf(headers) : null;
			enabledTools = enabledTools != null ? List.copyOf(enabledTools) : null;
			disabledTools = disabledTools != null ? List.copyOf(disabledTools) : null;
		}

		/**
		 * Backward-compatible constructor with url and headers.
		 */
		public HttpMcpServerConfig(String url, Map<String, String> headers) {
			this(null, url, headers, null, null, null, false);
		}

		public HttpMcpServerConfig withName(String name) {
			return new HttpMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public HttpMcpServerConfig withHeaders(Map<String, String> headers) {
			return new HttpMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public HttpMcpServerConfig withTimeoutSeconds(Double timeoutSeconds) {
			return new HttpMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public HttpMcpServerConfig withEnabledTools(List<String> enabledTools) {
			return new HttpMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public HttpMcpServerConfig withDisabledTools(List<String> disabledTools) {
			return new HttpMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		public HttpMcpServerConfig withForceAllToolsEager(boolean forceAllToolsEager) {
			return new HttpMcpServerConfig(name, url, headers, timeoutSeconds, enabledTools, disabledTools,
					forceAllToolsEager);
		}

		@Override
		public TransportType type() {
			return TransportType.HTTP;
		}
	}
}
