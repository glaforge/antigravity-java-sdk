package io.github.glaforge.antigravity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class AgentConfigMcpTest {

	@Test
	void testMcpServerConfig() {
		AgentConfig config = AgentConfig.builder().persona("Test Persona")
				.addMcpServer(
						McpServerConfig.stdio("npx", List.of("-y", "@modelcontextprotocol/server-everything", "stdio")))
				.addMcpServer(McpServerConfig.sse("http://localhost:8080/sse")).build();

		assertEquals(2, config.getMcpServers().size());

		McpServerConfig.StdioMcpServerConfig stdioConfig = (McpServerConfig.StdioMcpServerConfig) config.getMcpServers()
				.get(0);
		assertEquals(McpServerConfig.TransportType.STDIO, stdioConfig.getType());
		assertEquals("npx", stdioConfig.getCommand());
		assertEquals(3, stdioConfig.getArgs().size());

		McpServerConfig.SseMcpServerConfig sseConfig = (McpServerConfig.SseMcpServerConfig) config.getMcpServers()
				.get(1);
		assertEquals(McpServerConfig.TransportType.SSE, sseConfig.getType());
		assertEquals("http://localhost:8080/sse", sseConfig.getUrl());
	}
}
