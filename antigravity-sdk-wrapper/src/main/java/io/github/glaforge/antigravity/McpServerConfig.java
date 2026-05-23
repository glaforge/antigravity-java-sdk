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
public abstract class McpServerConfig {

	/**
	 * Defines the transport mechanism for the MCP server connection.
	 */
	public enum TransportType {
		/** Standard Input/Output transport. */
		STDIO, 
		/** Server-Sent Events transport. */
		SSE
	}

	private final TransportType type;

	/**
	 * Constructs a new configuration with the specified transport type.
	 * 
	 * @param type The transport type.
	 */
	protected McpServerConfig(TransportType type) {
		this.type = type;
	}

	/**
	 * Gets the transport type of this configuration.
	 * 
	 * @return The transport type.
	 */
	public TransportType getType() {
		return type;
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
	public static StdioMcpServerConfig stdio(String command, List<String> args) {
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
	public static SseMcpServerConfig sse(String url) {
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
	public static SseMcpServerConfig sse(String url, Map<String, String> headers) {
		return new SseMcpServerConfig(url, headers);
	}

	/**
	 * Configuration for an MCP server that communicates over Standard Input/Output.
	 */
	public static class StdioMcpServerConfig extends McpServerConfig {
		private final String command;
		private final List<String> args;

		private StdioMcpServerConfig(String command, List<String> args) {
			super(TransportType.STDIO);
			this.command = command;
			this.args = args;
		}

		/**
		 * Gets the command used to start the server.
		 * 
		 * @return The command.
		 */
		public String getCommand() {
			return command;
		}

		/**
		 * Gets the arguments passed to the command.
		 * 
		 * @return The list of arguments.
		 */
		public List<String> getArgs() {
			return args;
		}
	}

	/**
	 * Configuration for an MCP server that connects over Server-Sent Events (SSE).
	 */
	public static class SseMcpServerConfig extends McpServerConfig {
		private final String url;
		private final Map<String, String> headers;

		private SseMcpServerConfig(String url, Map<String, String> headers) {
			super(TransportType.SSE);
			this.url = url;
			this.headers = headers;
		}

		/**
		 * Gets the SSE endpoint URL.
		 * 
		 * @return The URL.
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * Gets the HTTP headers to send with the connection.
		 * 
		 * @return The map of headers.
		 */
		public Map<String, String> getHeaders() {
			return headers;
		}
	}
}
