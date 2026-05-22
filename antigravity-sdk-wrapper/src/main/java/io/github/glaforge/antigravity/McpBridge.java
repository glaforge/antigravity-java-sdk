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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.glaforge.antigravity.localharness.Tool;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Bridges the Antigravity SDK with external Model Context Protocol (MCP)
 * servers.
 */
public class McpBridge {

	private final List<McpSyncClient> clients = new ArrayList<>();
	private final List<DynamicTool> dynamicTools = new ArrayList<>();
	private final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Connects to all configured MCP servers and initializes the tools.
	 * 
	 * @param configs
	 *            List of server configurations to connect to.
	 */
	public void connect(List<McpServerConfig> configs) {
		for (McpServerConfig config : configs) {
			try {
				McpSyncClient client = connectToServer(config);
				client.initialize();

				// Discover tools from the server
				McpSchema.ListToolsResult toolsResult = client.listTools();
				if (toolsResult != null && toolsResult.tools() != null) {
					for (McpSchema.Tool mcpTool : toolsResult.tools()) {
						dynamicTools.add(new McpDynamicTool(client, mcpTool));
					}
				}
				clients.add(client);
				System.out.println("Successfully connected to MCP Server: " + config.getType());
			} catch (Exception e) {
				System.err.println("WARNING: Failed to connect or initialize MCP server: " + e.getMessage());
				// Depending on requirements, we log warning and continue
			}
		}
	}

	/**
	 * @return All discovered tools from the connected MCP servers.
	 */
	public List<DynamicTool> getDiscoveredTools() {
		return dynamicTools;
	}

	/**
	 * Closes all connected MCP clients gracefully.
	 */
	public void close() {
		for (McpSyncClient client : clients) {
			try {
				client.closeGracefully();
			} catch (Exception e) {
				// Ignore errors during close
			}
		}
		clients.clear();
		dynamicTools.clear();
	}

	private McpSyncClient connectToServer(McpServerConfig config) {
		if (config instanceof McpServerConfig.StdioMcpServerConfig stdioConfig) {
			ServerParameters params = ServerParameters.builder(stdioConfig.getCommand())
					.args(stdioConfig.getArgs().toArray(new String[0])).build();
			StdioClientTransport transport = new StdioClientTransport(params, McpJsonDefaults.getMapper());

			return McpClient.sync(transport)
					.clientInfo(McpSchema.Implementation.builder("antigravity-java-sdk", "1.0.0").build())
					.requestTimeout(Duration.ofSeconds(30)).build();
		} else if (config instanceof McpServerConfig.SseMcpServerConfig sseConfig) {
			HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(sseConfig.getUrl())
					.build();
			return McpClient.sync(transport)
					.clientInfo(McpSchema.Implementation.builder("antigravity-java-sdk", "1.0.0").build())
					.requestTimeout(Duration.ofSeconds(30)).build();
		} else {
			throw new IllegalArgumentException("Unsupported MCP Server Config: " + config.getClass());
		}
	}

	/**
	 * Wrapper for an MCP Tool to expose it as a DynamicTool to the Antigravity SDK.
	 */
	private class McpDynamicTool implements DynamicTool {

		private final McpSyncClient client;
		private final McpSchema.Tool mcpTool;

		public McpDynamicTool(McpSyncClient client, McpSchema.Tool mcpTool) {
			this.client = client;
			this.mcpTool = mcpTool;
		}

		@Override
		public String getName() {
			return mcpTool.name();
		}

		@Override
		public Tool getDefinition() {
			try {
				String schemaJson = mapper.writeValueAsString(mcpTool.inputSchema());
				return Tool.newBuilder().setName(mcpTool.name())
						.setDescription(mcpTool.description() != null ? mcpTool.description() : "")
						.setParametersJsonSchema(schemaJson).build();
			} catch (Exception e) {
				throw new RuntimeException("Failed to serialize MCP tool schema", e);
			}
		}

		@Override
		public Object execute(JsonNode arguments) throws Exception {
			Map<String, Object> argsMap = mapper.convertValue(arguments, new TypeReference<Map<String, Object>>() {
			});
			CallToolRequest request = CallToolRequest.builder(mcpTool.name()).arguments(argsMap).build();
			CallToolResult result = client.callTool(request);

			if (result == null || result.content() == null) {
				return "Tool executed successfully with no output.";
			}

			// Simple concatenation of text content or return JSON representation
			StringBuilder sb = new StringBuilder();
			for (McpSchema.Content content : result.content()) {
				if (content instanceof McpSchema.TextContent textContent) {
					sb.append(textContent.text()).append("\n");
				} else {
					sb.append(content.toString()).append("\n");
				}
			}
			return sb.toString().trim();
		}
	}
}
