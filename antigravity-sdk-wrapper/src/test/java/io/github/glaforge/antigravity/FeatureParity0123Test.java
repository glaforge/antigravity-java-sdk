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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.glaforge.antigravity.beta.WorkflowAgent;
import io.github.glaforge.antigravity.beta.WorkflowDefinition;
import io.github.glaforge.antigravity.beta.WorkflowProgress;
import io.github.glaforge.antigravity.beta.WorkflowResult;
import io.github.glaforge.antigravity.beta.Workflows;
import io.github.glaforge.antigravity.hooks.ToolCall;
import io.github.glaforge.antigravity.localharness.CompactionConfig;
import io.github.glaforge.antigravity.localharness.HarnessConfig;
import io.github.glaforge.antigravity.localharness.SkillsConfig;
import io.github.glaforge.antigravity.localharness.SubagentsConfig;
import io.github.glaforge.antigravity.localharness.SystemInstructions;
import io.github.glaforge.antigravity.localharness.ToolOutputTruncation;
import io.github.glaforge.antigravity.tools.Param;
import io.github.glaforge.antigravity.tools.SchemaGenerator;
import io.github.glaforge.antigravity.tools.Tool;
import io.github.glaforge.antigravity.tools.ToolDefinition;
import io.github.glaforge.antigravity.tools.ToolRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity0123Test {

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	@Test
	public void testWorkflowCallToolMultiTurnAgentAndArgsPrimitives() {
		String raw = """
				import json
				target = workflows.args.get("target", "README.md")
				workflows.phase("Read")
				try:
				    raw = await workflows.call_tool("read_file", Path=target)
				except workflows.ToolError as err:
				    workflows.log(err.tool, err.message)
				    raw = "{}"

				reviewer = workflows.agent("You are a code reviewer.", role="Code Reviewer")
				attempts = 0
				while attempts < 2:
				    verdict = await reviewer(f"Review {json.dumps(raw)}", schema={"type": "string"})
				    workflows.log(verdict)
				    attempts += 1
				""";

		WorkflowDefinition def = Workflows.define("advanced_workflow", "Workflow using v0.1.23 primitives", raw);
		String src = def.source();
		assertTrue(src.contains("import json"));
		assertTrue(src.contains("target = args.get(\"target\", \"README.md\")"));
		assertTrue(src.contains("raw = await call_tool(\"read_file\", Path=target)"));
		assertTrue(src.contains("except ToolError as err:"));
		assertTrue(src.contains("reviewer = agent(\"You are a code reviewer.\", role=\"Code Reviewer\")"));
		assertTrue(src.contains("while attempts < 2:"));
		assertFalse(src.contains("workflows."));

		// Builder methods for callTool, defineAgent, callAgent
		Map<String, Object> toolArgs = new LinkedHashMap<>();
		toolArgs.put("Path", "pom.xml");
		toolArgs.put("StartLine", 1);
		WorkflowDefinition built = Workflows.builder("built_v0123").description("Builder test").phase("Inspect")
				.callTool("pom_content", "read_file", toolArgs)
				.callToolExpr("dynamic_content", "read_file", Map.of("Path", "args.get('file')"))
				.defineAgent("reviewer", "Review files carefully", "Reviewer", "self", "inherit")
				.callAgent("turn1", "reviewer", "First review turn", String.class)
				.callAgent("turn2", "reviewer", "Second review turn", Map.of("type", "boolean"))
				.logExpr("turn1", "turn2").build();

		String builtSrc = built.source();
		assertTrue(builtSrc.contains("pom_content = await call_tool(\"read_file\", Path=\"pom.xml\", StartLine=1)"));
		assertTrue(builtSrc.contains("dynamic_content = await call_tool(\"read_file\", Path=args.get('file'))"));
		assertTrue(builtSrc.contains(
				"reviewer = agent(\"Review files carefully\", role=\"Reviewer\", type_name=\"self\", workspace=\"inherit\")"));
		assertTrue(builtSrc.contains("turn1 = await reviewer(\"First review turn\", schema={\"type\":\"string\"})"));
		assertTrue(builtSrc.contains("turn2 = await reviewer(\"Second review turn\", schema={\"type\":\"boolean\"})"));

		// Compile-time stubs outside runtime
		assertEquals(Map.of(), Workflows.args());
		assertThrows(IllegalArgumentException.class, () -> Workflows.callTool(""));
		assertThrows(IllegalArgumentException.class, () -> Workflows.callTool("   ", Map.of()));
		assertThrows(IllegalStateException.class, () -> Workflows.callTool("read_file", Map.of("Path", "a.txt")));
		assertThrows(IllegalStateException.class, () -> Workflows.agent("Standing instructions"));
		WorkflowAgent handle = new WorkflowAgent();
		assertThrows(IllegalStateException.class, () -> handle.call("Turn 1", String.class));
		assertThrows(IllegalStateException.class, () -> handle.call("Turn 2", Map.of("type", "string")));

		Workflows.ToolError toolError = new Workflows.ToolError("run_command", "permission denied");
		assertEquals("run_command", toolError.tool());
		assertEquals("permission denied", toolError.toolMessage());
		assertEquals("run_command: permission denied", toolError.getMessage());
	}

	@Test
	public void testRunWorkflowWithRuntimeArgsAndMultiWorkspaceResolution(@TempDir Path tempDir) throws Exception {
		Path ws1 = tempDir.resolve("ws1");
		Path ws2 = tempDir.resolve("ws2");
		Files.createDirectories(ws1);
		Files.createDirectories(ws2);

		// Place script in second workspace only
		Path scriptInWs2 = ws2.resolve("my_script.py");
		Files.writeString(scriptInWs2, "phase('FromWS2')\nlog(args.get('env'))\n", StandardCharsets.UTF_8);

		final String[] capturedPrompt = new String[1];
		Agent fakeAgent = new Agent(AgentConfig.builder().workspaces(List.of(ws1.toString(), ws2.toString()))
				.capabilities(CapabilitiesConfig.builder().enableSubagents(true).enableRunWorkflow(true).build())
				.build(), false) {
			@Override
			public CompletableFuture<AgentResponse> chat(String prompt) {
				capturedPrompt[0] = prompt;
				return super.chat(prompt);
			}
		};

		Map<String, Object> runtimeArgs = new LinkedHashMap<>();
		runtimeArgs.put("env", "prod");
		runtimeArgs.put("retries", 3);

		var wfFuture = fakeAgent.beta().runWorkflow("my_script.py", "Custom run", runtimeArgs, null);
		fakeAgent.handleIncomingMessage(null, """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-wf-args",
				    "stepIndex": 1,
				    "state": "STATE_DONE",
				    "runWorkflow": {
				      "scriptPath": "file:///tmp/my_script.py",
				      "script": "phase(\\"FromWS2\\")",
				      "description": "Custom run",
				      "output": "prod\\n"
				    },
				    "textDelta": "Completed"
				  }
				}
				""");
		fakeAgent.handleIncomingMessage(null, "{\"trajectoryStateUpdate\": {\"state\": \"STATE_IDLE\"}}");

		WorkflowResult res = wfFuture.get(5, TimeUnit.SECONDS);
		assertNotNull(capturedPrompt[0]);
		assertTrue(capturedPrompt[0].contains(scriptInWs2.toAbsolutePath().normalize().toString()));
		assertTrue(capturedPrompt[0].contains("with Args `{\"env\":\"prod\",\"retries\":3}`"));
		assertEquals("prod\n", res.output());

		// Non-finite numbers in args must be rejected
		assertThrows(IllegalArgumentException.class,
				() -> fakeAgent.beta().runWorkflow("my_script.py", Map.of("bad", Double.NaN)));
		assertThrows(IllegalArgumentException.class,
				() -> fakeAgent.beta().runWorkflow("my_script.py", Map.of("bad", Double.POSITIVE_INFINITY)));
	}

	@Test
	public void testCustomSystemInstructionsWithTitle() {
		CustomSystemInstructions custom = CustomSystemInstructions.of("Always answer in haiku.", "HaikuPolicy");
		assertEquals("Always answer in haiku.", custom.text());
		assertEquals("HaikuPolicy", custom.title());

		AgentConfig config = AgentConfig.builder().customInstructions("Strict security auditor.", "SecurityRole")
				.build();
		assertNotNull(config.getCustomSystemInstructions());
		assertEquals("Strict security auditor.", config.getCustomSystemInstructions().text());
		assertEquals("SecurityRole", config.getCustomSystemInstructions().title());
		assertEquals("Strict security auditor.", config.getInstructions());

		LocalOpenAIAgentConfig openaiConfig = LocalOpenAIAgentConfig.builder().baseUrl("http://localhost:8000/v1")
				.modelName("llama3").customInstructions("Local prompt", "LocalSection").build();
		assertEquals("LocalSection", openaiConfig.getAgentConfig().getCustomSystemInstructions().title());

		LiteRTAgentConfig litertConfig = LiteRTAgentConfig.builder().modelPath("/tmp/model.litertlm")
				.customInstructions("On-device prompt", "EdgeSection").build();
		assertEquals("EdgeSection", litertConfig.getAgentConfig().getCustomSystemInstructions().title());
	}

	public record ReadWriteRecord(@Param(description = "Entity ID", readOnly = true) String id,
			@Param(description = "Secret token", writeOnly = true) String secret,
			@JsonProperty(access = JsonProperty.Access.READ_ONLY) String createdAt,
			@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {
	}

	public static class SampleToolWithReadOnlyWriteOnly {
		@Tool(name = "update_secret", description = "Updates secret")
		public String update(@Param(name = "id", readOnly = true) String id,
				@Param(name = "token", writeOnly = true) String token) {
			return "ok";
		}
	}

	@Test
	public void testSchemaGeneratorReadOnlyWriteOnlyAndLiteralMetadataPreservation() throws Exception {
		ObjectNode recordSchema = SchemaGenerator.generateSchema(ReadWriteRecord.class);
		JsonNode props = recordSchema.path("properties");
		assertTrue(props.path("id").path("readOnly").asBoolean());
		assertFalse(props.path("id").has("writeOnly"));
		assertTrue(props.path("secret").path("writeOnly").asBoolean());
		assertFalse(props.path("secret").has("readOnly"));
		assertTrue(props.path("createdAt").path("readOnly").asBoolean());
		assertTrue(props.path("password").path("writeOnly").asBoolean());

		// ToolRegistry parameter schema generation
		ToolRegistry registry = new ToolRegistry();
		registry.registerToolsFromObject(new SampleToolWithReadOnlyWriteOnly());
		List<ToolDefinition> defs = registry.getToolDefinitions();
		assertEquals(1, defs.size());
		JsonNode toolParams = MAPPER.readTree(defs.get(0).getParametersJsonSchema()).path("properties");
		assertTrue(toolParams.path("id").path("readOnly").asBoolean());
		assertTrue(toolParams.path("token").path("writeOnly").asBoolean());

		// normalizeSchema: snake_case read_only/write_only and literal preservation of
		// title, description, summary, format, pattern
		ObjectNode rawSchema = MAPPER.createObjectNode();
		rawSchema.put("type", "OBJECT");
		rawSchema.put("title", "STRING");
		rawSchema.put("description", "INTEGER");
		rawSchema.put("summary", "BOOLEAN");
		rawSchema.put("format", "DATE-TIME");
		rawSchema.put("pattern", "^[A-Z]+$");
		rawSchema.put("read_only", true);
		rawSchema.put("write_only", false);

		JsonNode normalized = SchemaGenerator.normalizeSchema(rawSchema);
		assertEquals("object", normalized.path("type").asText());
		assertEquals("STRING", normalized.path("title").asText());
		assertEquals("INTEGER", normalized.path("description").asText());
		assertEquals("BOOLEAN", normalized.path("summary").asText());
		assertEquals("DATE-TIME", normalized.path("format").asText());
		assertEquals("^[A-Z]+$", normalized.path("pattern").asText());
		assertTrue(normalized.path("readOnly").asBoolean());
		assertFalse(normalized.path("writeOnly").asBoolean());
		assertFalse(normalized.has("read_only"));
		assertFalse(normalized.has("write_only"));
	}

	@Test
	public void testDeterministicPathNormalizationAndCanonicalPath() throws Exception {
		ObjectNode args = MAPPER.createObjectNode();
		args.put("output_path", "file:///tmp/out.txt");
		args.put("file_path", "cns://el-d/home/user/data.csv");

		ToolCall call = new ToolCall("write_file", args, "call_1", "traj:1", null);
		// Both paths normalized in-place
		assertEquals("/tmp/out.txt", call.args().path("output_path").asText());
		assertEquals("/cns/el-d/home/user/data.csv", call.args().path("file_path").asText());
		// file_path has higher priority than output_path in WIRE_PATH_ARGUMENT_KEYS
		assertEquals("/cns/el-d/home/user/data.csv", call.canonicalPath());

		Map<String, Object> mapArgs = new LinkedHashMap<>();
		mapArgs.put("ScriptPath", "file:///workspace/run.py");
		String canonical = ToolCall.normalizePathArguments(mapArgs);
		assertEquals("/workspace/run.py", canonical);
		assertEquals("/workspace/run.py", mapArgs.get("ScriptPath"));
	}

	@Test
	public void testMediaHomeExpansionAndMimeNormalization(@TempDir Path tempDir) throws Exception {
		// MIME normalization via constructors and Media.fromData
		byte[] bytes = new byte[]{1, 2, 3};
		AgentInput.Image img = new AgentInput.Image("  IMAGE/PNG  ", bytes, "desc");
		assertEquals("image/png", img.mimeType());

		AgentInput.Media fromDataImg = AgentInput.Media.fromData(bytes, "  Image/WebP \n", "webp image");
		assertInstanceOf(AgentInput.Image.class, fromDataImg);
		assertEquals("image/webp", fromDataImg.mimeType());

		AgentInput.Media fromDataDoc = AgentInput.Media.fromData(bytes, " APPLICATION/PDF ");
		assertInstanceOf(AgentInput.Document.class, fromDataDoc);
		assertEquals("application/pdf", fromDataDoc.mimeType());

		assertThrows(IllegalArgumentException.class,
				() -> AgentInput.Media.fromData(bytes, "application/octet-stream"));

		// Home expansion (~)
		String originalHome = System.getProperty("user.home");
		try {
			System.setProperty("user.home", tempDir.toString());
			Path testImgPath = tempDir.resolve("sample.png");
			Files.write(testImgPath, bytes);

			AgentInput.Image loaded = AgentInput.Image.fromFile("~/sample.png", "home image");
			assertArrayEquals(bytes, loaded.data());
			assertEquals("home image", loaded.description());
			assertNotNull(loaded.mimeType());
		} finally {
			if (originalHome != null) {
				System.setProperty("user.home", originalHome);
			}
		}
	}

	@Test
	public void testFormatDiffBlocks() throws Exception {
		List<Map<String, Object>> blocks = List.of(Map.of("start_line", 10, "end_line", 12, "lines",
				List.of(Map.of("action", "LINE_ACTION_CONTEXT", "text", "unchanged"),
						Map.of("action", "LINE_ACTION_DELETE", "text", "old line"),
						Map.of("action", "LINE_ACTION_INSERT", "text", "new line"))));

		String diff = DiffFormatter.formatDiffBlocks(blocks);
		String expected = """
				@@ -10 +12 @@
				 unchanged
				-old line
				+new line""";
		assertEquals(expected, diff);

		JsonNode jsonBlocks = MAPPER.valueToTree(blocks);
		assertEquals(expected, DiffFormatter.formatDiffBlocks(jsonBlocks));
	}

	@Test
	public void testProtocolV0123Additions() {
		var sysBuilder = SystemInstructions.newBuilder()
				.setSectionHeaderFormat(SystemInstructions.SectionHeaderFormat.SECTION_HEADER_FORMAT_XML);
		sysBuilder.getCustomBuilder().addPartBuilder().setText("Custom body").setTitle("CustomTag");
		SystemInstructions sys = sysBuilder.build();
		assertEquals(SystemInstructions.SectionHeaderFormat.SECTION_HEADER_FORMAT_XML, sys.getSectionHeaderFormat());
		assertEquals("CustomTag", sys.getCustom().getPart(0).getTitle());

		CompactionConfig compaction = CompactionConfig.newBuilder().setIncludeLastUserMessage(true).build();
		assertTrue(compaction.getIncludeLastUserMessage());

		SubagentsConfig subagents = SubagentsConfig.newBuilder().setDisableDefineSubagentTool(true).build();
		assertTrue(subagents.getDisableDefineSubagentTool());

		SkillsConfig skills = SkillsConfig.newBuilder().putSkillVersions("my-skill", "1.2.0")
				.setPresentationMode(SkillsConfig.PresentationMode.FILE_SYSTEM).setMaterializedSkillsDir("/tmp/skills")
				.addPreactivateSkills("my-skill").build();
		assertEquals("1.2.0", skills.getSkillVersionsMap().get("my-skill"));
		assertEquals(SkillsConfig.PresentationMode.FILE_SYSTEM, skills.getPresentationMode());
		assertEquals("/tmp/skills", skills.getMaterializedSkillsDir());
		assertEquals(List.of("my-skill"), skills.getPreactivateSkillsList());

		var harnessBuilder = HarnessConfig.newBuilder();
		harnessBuilder.addToolsBuilder().setName("my_tool").setToolOutputTruncation(
				ToolOutputTruncation.newBuilder().setTruncate(ToolOutputTruncation.TruncateStrategy.newBuilder()
						.setMaxTokens(4096).setSaveFullOutput(true).build()).build());
		HarnessConfig harness = harnessBuilder.build();
		assertEquals(4096, harness.getTools(0).getToolOutputTruncation().getTruncate().getMaxTokens());
		assertTrue(harness.getTools(0).getToolOutputTruncation().getTruncate().getSaveFullOutput());
	}
}
