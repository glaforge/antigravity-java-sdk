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

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.glaforge.antigravity.beta.WorkflowDefinition;
import io.github.glaforge.antigravity.beta.WorkflowException;
import io.github.glaforge.antigravity.beta.WorkflowProgress;
import io.github.glaforge.antigravity.beta.WorkflowResult;
import io.github.glaforge.antigravity.beta.Workflows;
import io.github.glaforge.antigravity.hooks.ToolCall;
import io.github.glaforge.antigravity.localharness.HarnessConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity0122Test {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Test
	@SuppressWarnings("deprecation")
	public void testBuiltinToolsInvokeSubagentAndRunWorkflow() {
		assertEquals("invoke_subagent", BuiltinTools.INVOKE_SUBAGENT.getValue());
		assertEquals("run_workflow", BuiltinTools.RUN_WORKFLOW.getValue());
		assertSame(BuiltinTools.INVOKE_SUBAGENT, BuiltinTools.START_SUBAGENT);

		assertEquals(BuiltinTools.INVOKE_SUBAGENT, BuiltinTools.fromValue("invoke_subagent"));
		assertEquals(BuiltinTools.INVOKE_SUBAGENT, BuiltinTools.fromValue("start_subagent"));
		assertEquals(BuiltinTools.RUN_WORKFLOW, BuiltinTools.fromValue("run_workflow"));

		assertEquals("invoke_subagent", BuiltinTools.normalizeToolName("start_subagent"));
		assertEquals("invoke_subagent", BuiltinTools.normalizeToolName("invoke_subagent"));
		assertEquals("start_subagent", BuiltinTools.normalizeToolName("start_subagent", "mcp-server"));
		assertEquals("run_command", BuiltinTools.normalizeToolName("run_command"));

		assertTrue(BuiltinTools.defaultTools().contains(BuiltinTools.INVOKE_SUBAGENT));
		assertTrue(BuiltinTools.defaultTools().contains(BuiltinTools.RUN_WORKFLOW));
		assertTrue(BuiltinTools.nondestructive().contains(BuiltinTools.RUN_WORKFLOW));
	}

	@Test
	public void testToolCallAndPolicyToolNameNormalization() throws Exception {
		ToolCall call = new ToolCall("start_subagent", JSON.readTree("{\"agent\":\"helper\"}"), "call-1", "t:1", null);
		assertEquals("invoke_subagent", call.name());
		assertEquals("call-1", call.id());
		assertEquals("t:1", call.stepId());

		// MCP tools with a serverName should not be normalized
		ToolCall mcpCall = new ToolCall("start_subagent", JSON.readTree("{}"), "call-2", "t:2", "custom-mcp");
		assertEquals("start_subagent", mcpCall.name());

		Policy allowPolicy = Policies.allowTools(BuiltinTools.INVOKE_SUBAGENT);
		assertEquals(Policy.Decision.ALLOW, allowPolicy.evaluate("start_subagent", JSON.readTree("{}")));
		assertEquals(Policy.Decision.ALLOW, allowPolicy.evaluate("invoke_subagent", JSON.readTree("{}")));

		Policy denyLegacy = Policies.denyTools("start_subagent");
		assertEquals(Policy.Decision.DENY, denyLegacy.evaluate("invoke_subagent", JSON.readTree("{}")));
		assertEquals(Policy.Decision.DENY, denyLegacy.evaluate("start_subagent", JSON.readTree("{}")));
	}

	@Test
	public void testInlineSkillsAndMutualExclusivity() throws Exception {
		InlineSkill skill = InlineSkill.builder().name("code-review").description("Reviews Java code")
				.content("# Code Review\nCheck null safety.").addAllowedTool("view_file")
				.addMetadata("visibility", "public").build();

		assertEquals("code-review", skill.name());
		assertEquals("Reviews Java code", skill.description());
		assertEquals(List.of("view_file"), skill.allowedTools());
		assertEquals(Map.of("visibility", "public"), skill.metadata());

		// Mutual exclusivity in AgentConfig
		assertThrows(IllegalArgumentException.class,
				() -> AgentConfig.builder().addSkillPath("/tmp/skills").addInlineSkill(skill).build());

		// Mutual exclusivity in LiteRTAgentConfig
		assertThrows(IllegalArgumentException.class, () -> LiteRTAgentConfig.builder().baseUrl("http://localhost:8080")
				.addSkillPath("/tmp/skills").addInlineSkill(skill).build());

		// Mutual exclusivity in LocalOpenAIAgentConfig
		assertThrows(IllegalArgumentException.class, () -> LocalOpenAIAgentConfig.builder()
				.baseUrl("http://localhost:8080").addSkillPath("/tmp/skills").addInlineSkill(skill).build());

		// Mutual exclusivity in SubagentOverrideSkillsConfig
		assertThrows(IllegalArgumentException.class,
				() -> new SubagentOverrideSkillsConfig(List.of("/tmp/skills"), List.of(skill)));

		// Valid inline skills config and proto serialization
		SubagentConfig sub = SubagentConfig.builder().name("reviewer").description("Reviewer subagent")
				.skillsConfig(SubagentSkillsConfig.override(skill)).build();

		AgentConfig cfg = AgentConfig.builder().addInlineSkill(skill).addSubagent(sub).build();
		Agent agent = new Agent(cfg, false);
		HarnessConfig proto = agent.buildHarnessConfig("test-key");

		assertTrue(proto.getSkillsConfig().getEnabled());
		assertEquals(1, proto.getSkillsConfig().getSkillsCount());
		assertEquals("code-review", proto.getSkillsConfig().getSkills(0).getSkill().getName());
		assertEquals("Reviews Java code", proto.getSkillsConfig().getSkills(0).getSkill().getDescription());
		assertEquals(List.of("view_file"), proto.getSkillsConfig().getSkills(0).getSkill().getAllowedToolsList());
		assertEquals("public", proto.getSkillsConfig().getSkills(0).getSkill().getMetadataOrThrow("visibility"));

		assertEquals(1, proto.getCustomSubagentsCount());
		assertEquals(1, proto.getCustomSubagents(0).getSkillsConfig().getOverrideConfig().getSkillSourcesCount());
		assertEquals("code-review", proto.getCustomSubagents(0).getSkillsConfig().getOverrideConfig().getSkillSources(0)
				.getSkill().getName());
	}

	@Test
	public void testGenerationConfigMaxOutputTokensValidationAndProto() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> GenerationConfig.builder().maxOutputTokens(0).build());
		assertThrows(IllegalArgumentException.class, () -> GenerationConfig.builder().maxOutputTokens(-10).build());

		GenerationConfig gen = GenerationConfig.builder().thinkingLevel(ThinkingLevel.HIGH).maxOutputTokens(4096)
				.build();
		assertEquals(4096, gen.maxOutputTokens());

		AgentConfig cfg = AgentConfig.builder().generation(gen).build();
		Agent agent = new Agent(cfg, false);
		HarnessConfig proto = agent.buildHarnessConfig("test-key");
		assertEquals(4096, proto.getModels(0).getGeminiApiEndpoint().getOptions().getMaxOutputTokens());
	}

	@Test
	public void testCapabilitiesRunWorkflowAndMcpMaxOutputBytes() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> CapabilitiesConfig.builder().mcpMaxOutputBytes(0).build());
		assertThrows(IllegalArgumentException.class,
				() -> CapabilitiesConfig.builder().mcpMaxOutputBytes(-1024).build());

		CapabilitiesConfig caps = CapabilitiesConfig.builder().enableSubagents(true).mcpMaxOutputBytes(65536).build();

		assertTrue(caps.enableRunWorkflow());
		assertEquals(65536, caps.mcpMaxOutputBytes());

		CapabilitiesConfig disabledWfCaps = CapabilitiesConfig.builder().enableSubagents(true).enableRunWorkflow(false)
				.build();
		assertFalse(disabledWfCaps.enableRunWorkflow());

		AgentConfig cfg = AgentConfig.builder().capabilities(caps).build();
		Agent agent = new Agent(cfg, false);
		HarnessConfig proto = agent.buildHarnessConfig("test-key");

		assertEquals(65536, proto.getMcpMaxOutputBytes());
		assertTrue(proto.getHarnessSideTools().getRunWorkflow().getEnabled());
	}

	@Test
	public void testMcpServerConfigEnhancementsAndProto() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> McpServerConfig.stdio("npx", List.of())
				.withEnabledTools(List.of("a")).withDisabledTools(List.of("b")));
		assertThrows(IllegalArgumentException.class,
				() -> McpServerConfig.http("http://localhost:3000").withTimeoutSeconds(0.0));

		McpServerConfig stdio = McpServerConfig.stdio("python3", List.of("-m", "mcp_server")).withName("local-mcp")
				.withEnv(Map.of("API_TOKEN", "secret")).withTimeoutSeconds(45.0).withEnabledTools(List.of("read_doc"))
				.withForceAllToolsEager(true);

		AgentConfig cfg = AgentConfig.builder().addMcpServer(stdio).build();
		Agent agent = new Agent(cfg, false);
		HarnessConfig proto = agent.buildHarnessConfig("test-key");

		assertEquals(1, proto.getMcpServersCount());
		var mcpProto = proto.getMcpServers(0);
		assertEquals("local-mcp", mcpProto.getName());
		assertEquals(45, mcpProto.getTimeoutSeconds());
		assertEquals(List.of("read_doc"), mcpProto.getEnabledToolsList());
		assertTrue(mcpProto.getForceAllToolsEager());
		assertEquals("secret", mcpProto.getStdio().getEnvOrThrow("API_TOKEN"));
	}

	@Test
	public void testCompactionConfigUseModelReportedTokenCount() throws Exception {
		CompactionConfig compaction = CompactionConfig.builder().tokenThreshold(50000).useModelReportedTokenCount(true)
				.build();
		assertTrue(compaction.useModelReportedTokenCount());

		AgentConfig cfg = AgentConfig.builder().compactionConfig(compaction).build();
		Agent agent = new Agent(cfg, false);
		HarnessConfig proto = agent.buildHarnessConfig("test-key");
		assertTrue(proto.getCompactionConfig().getUseModelReportedTokenCount());
	}

	@Test
	public void testUsageMetadataNegateAndSubtractWhenTotalZero() {
		UsageMetadata a = new UsageMetadata(120, 20, 50, 10, 0);
		UsageMetadata b = new UsageMetadata(40, 5, 15, 2, 0);

		UsageMetadata diff = a.subtract(b);
		assertEquals(80, diff.promptTokenCount());
		assertEquals(15, diff.cachedContentTokenCount());
		assertEquals(35, diff.candidatesTokenCount());
		assertEquals(8, diff.thoughtsTokenCount());
		assertEquals(0, diff.totalTokenCount());

		UsageMetadata neg = b.negate();
		assertEquals(-40, neg.promptTokenCount());
		assertEquals(-5, neg.cachedContentTokenCount());
		assertEquals(-15, neg.candidatesTokenCount());
		assertEquals(-8, diff.negate().thoughtsTokenCount());

		assertEquals(diff, b.subtractFrom(a));
	}

	@Test
	public void testWorkflowsValidationAndRunWorkflowStepParsing(@TempDir Path tempDir) throws Exception {
		String validScript = """
				workflows.phase("research")
				res = await workflows.agent("Find info", schema={"type": "string"})
				workflows.log(res)
				""";
		WorkflowDefinition def = Workflows.define("my_workflow", "Research workflow", validScript);
		assertEquals("my_workflow", def.name());
		assertEquals("Research workflow", def.description());
		assertTrue(def.source().contains("phase(\"research\")"));
		assertFalse(def.source().contains("workflows.phase"));

		// Fluent builder with Map and Java Type/Class schemas
		record AuditVerdict(boolean passed, String summary) {
		}
		WorkflowDefinition built = Workflows.builder("built_wf").description("Built in Java").phase("step1")
				.agent("out", "Summarize docs", String.class, "Summarizer")
				.agent("verdict", "Verify summary", AuditVerdict.class, "Verifier").logExpr("out", "verdict").build();
		assertEquals("built_wf", built.name());
		assertTrue(built.source().contains("await agent(\"Summarize docs\", schema={\"type\":\"string\"}"));
		assertTrue(built.source().contains("\"passed\":{\"type\":\"boolean\"}"));
		assertTrue(built.source().contains("\"required\":[\"passed\",\"summary\"]"));

		// Invalid workflow constructs
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("import os\nlog('hi')"));
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("while True:\n    log('hi')"));
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("return 42"));
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("x = obj._secret"));
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("x = __import__('os')"));

		// Verify file:// URI normalization
		assertEquals("/tmp/test script.py", Agent.normalizeFileUri("file:///tmp/test%20script.py"));
		assertEquals("/tmp/plain.py", Agent.normalizeFileUri("/tmp/plain.py"));

		// Verify runWorkflow capability guard
		Agent disabledAgent = new Agent(AgentConfig.builder().build(), false);
		assertThrows(IllegalArgumentException.class, () -> disabledAgent.beta().runWorkflow(def));

		// Verify stepUpdate.runWorkflow parsing
		Agent enabledAgent = new Agent(AgentConfig.builder()
				.capabilities(CapabilitiesConfig.builder().enableSubagents(true).enableRunWorkflow(true).build())
				.workspaceDir(tempDir).build(), false);

		List<WorkflowProgress> progressUpdates = new ArrayList<>();
		Path scriptFile = tempDir.resolve("wf.py");
		Files.writeString(scriptFile, def.source(), StandardCharsets.UTF_8);

		var future = enabledAgent.beta().runWorkflow(scriptFile, "Custom run", progressUpdates::add);

		String stepJson = """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-1",
				    "stepIndex": 3,
				    "state": "STATE_DONE",
				    "runWorkflow": {
				      "scriptPath": "file:///tmp/wf.py",
				      "script": "",
				      "description": "Custom run",
				      "output": "Workflow finished ok"
				    },
				    "textDelta": "Done executing workflow."
				  }
				}
				""";
		enabledAgent.handleIncomingMessage(null, stepJson);
		enabledAgent.handleIncomingMessage(null, "{\"trajectoryStateUpdate\": {\"state\": \"STATE_IDLE\"}}");

		WorkflowResult result = future.get();
		assertEquals(1, progressUpdates.size());
		assertEquals("/tmp/wf.py", progressUpdates.get(0).scriptPath());
		assertEquals("Workflow finished ok", result.output());
		assertEquals("Done executing workflow.", result.text());
	}
}
