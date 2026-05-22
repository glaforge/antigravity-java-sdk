package io.github.glaforge.antigravity;

import java.util.List;
import java.util.Map;

/**
 * Configuration for connecting to an external Model Context Protocol (MCP)
 * server.
 */
public abstract class McpServerConfig {

	public enum TransportType {
		STDIO, SSE
	}

	private final TransportType type;

	protected McpServerConfig(TransportType type) {
		this.type = type;
	}

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
	 */
	public static SseMcpServerConfig sse(String url, Map<String, String> headers) {
		return new SseMcpServerConfig(url, headers);
	}

	public static class StdioMcpServerConfig extends McpServerConfig {
		private final String command;
		private final List<String> args;

		private StdioMcpServerConfig(String command, List<String> args) {
			super(TransportType.STDIO);
			this.command = command;
			this.args = args;
		}

		public String getCommand() {
			return command;
		}

		public List<String> getArgs() {
			return args;
		}
	}

	public static class SseMcpServerConfig extends McpServerConfig {
		private final String url;
		private final Map<String, String> headers;

		private SseMcpServerConfig(String url, Map<String, String> headers) {
			super(TransportType.SSE);
			this.url = url;
			this.headers = headers;
		}

		public String getUrl() {
			return url;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}
	}
}
