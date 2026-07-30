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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@Tag("unit")
class AgentConfigMcpTest {

	@Test
	void testMcpServerConfig() {
		AgentConfig config = AgentConfig.builder().instructions("Test Persona")
				.addMcpServer(
						McpServerConfig.stdio("npx", List.of("-y", "@modelcontextprotocol/server-everything", "stdio")))
				.addMcpServer(McpServerConfig.sse("http://localhost:8080/sse"))
				.addMcpServer(McpServerConfig.http("http://localhost:8080/mcp"))
				.addMcpServer(McpServerConfig.streamableHttp("http://localhost:8080/stream")).build();

		assertEquals(4, config.getMcpServers().size());

		McpServerConfig.StdioMcpServerConfig stdioConfig = (McpServerConfig.StdioMcpServerConfig) config.getMcpServers()
				.get(0);
		assertEquals(McpServerConfig.TransportType.STDIO, stdioConfig.type());
		assertEquals("npx", stdioConfig.command());
		assertEquals(3, stdioConfig.args().size());

		McpServerConfig.SseMcpServerConfig sseConfig = (McpServerConfig.SseMcpServerConfig) config.getMcpServers()
				.get(1);
		assertEquals(McpServerConfig.TransportType.SSE, sseConfig.type());
		assertEquals("http://localhost:8080/sse", sseConfig.url());

		McpServerConfig.HttpMcpServerConfig httpConfig = (McpServerConfig.HttpMcpServerConfig) config.getMcpServers()
				.get(2);
		assertEquals(McpServerConfig.TransportType.HTTP, httpConfig.type());
		assertEquals("http://localhost:8080/mcp", httpConfig.url());

		McpServerConfig.HttpMcpServerConfig streamableConfig = (McpServerConfig.HttpMcpServerConfig) config
				.getMcpServers().get(3);
		assertEquals(McpServerConfig.TransportType.HTTP, streamableConfig.type());
		assertEquals("http://localhost:8080/stream", streamableConfig.url());
	}
}
