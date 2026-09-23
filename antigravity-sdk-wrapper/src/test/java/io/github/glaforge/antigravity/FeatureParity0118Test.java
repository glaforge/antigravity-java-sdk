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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glaforge.antigravity.hooks.HookResult;
import io.github.glaforge.antigravity.localharness.CustomAgent;
import io.github.glaforge.antigravity.localharness.Field;
import io.github.glaforge.antigravity.localharness.ModelConfig;
import io.github.glaforge.antigravity.localharness.PreToolResult;
import io.github.glaforge.antigravity.localharness.Struct;
import io.github.glaforge.antigravity.localharness.Value;
import io.github.glaforge.antigravity.Policies;
import io.github.glaforge.antigravity.tools.SchemaGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
public class FeatureParity0118Test {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void testEvalPreset() {
		AgentConfig config = AgentConfig.builder().instructions("Run benchmark evaluation").eval().build();

		assertNotNull(config.getCapabilities());
		assertFalse(config.getCapabilities().enableGenerateImage());
		assertFalse(config.getCapabilities().enableSubagents());
		assertNotNull(config.getCapabilities().runCommandConfig());
		assertTrue(config.getCapabilities().runCommandConfig().enableDaemons());

		assertNotNull(config.getRetryConfig());
		assertNotNull(config.getRetryConfig().apiRetry());
		assertEquals(Integer.MAX_VALUE, config.getRetryConfig().apiRetry().maxRetries());
		assertEquals(1000, config.getRetryConfig().apiRetry().initialSleepDurationMs());
		assertNull(config.getRetryConfig().modelOutputRetry());

		assertNotNull(config.getPolicies());
		assertEquals(1, config.getPolicies().size());
	}

	@Test
	public void testSubagentConfigRecordAndBuilder() {
		SubagentConfig subagent = SubagentConfig.builder().name("code_reviewer")
				.description("Reviews pull requests and provides suggestions")
				.instructions("You are an expert code reviewer.").model("gemini-2.5-pro").addTool("view_file")
				.addTool(BuiltinTools.RUN_COMMAND).agentBehavior(AgentBehavior.AUTONOMOUS).build();

		assertEquals("code_reviewer", subagent.name());
		assertEquals("Reviews pull requests and provides suggestions", subagent.description());
		assertEquals("You are an expert code reviewer.", subagent.instructions());
		assertEquals("gemini-2.5-pro", subagent.model());
		assertEquals(2, subagent.tools().size());
		assertTrue(subagent.tools().contains("view_file"));
		assertTrue(subagent.tools().contains("run_command"));
		assertEquals(AgentBehavior.AUTONOMOUS, subagent.agentBehavior());

		// Verify Protobuf mapping with ModelConfig (field 10)
		CustomAgent.Builder customAgentBuilder = CustomAgent.newBuilder().setName(subagent.name())
				.setDescription(subagent.description());

		if (subagent.model() != null) {
			customAgentBuilder.setModel(ModelConfig.newBuilder().setName(subagent.model()).build());
		}

		CustomAgent customAgent = customAgentBuilder.build();
		assertEquals("code_reviewer", customAgent.getName());
		assertTrue(customAgent.hasModel());
		assertEquals("gemini-2.5-pro", customAgent.getModel().getName());
	}

	@Test
	public void testLiteRTAgentConfigPresetDefaults() {
		LiteRTAgentConfig config = LiteRTAgentConfig.builder().instructions("Execute on device")
				.modelPath("/tmp/model.litertlm").addWorkspace("/tmp/agy-test").addPolicy(Policies.allowAll())
				.lightweight().build();

		assertEquals(AgentBehavior.MINIMAL, config.getAgentConfig().getAgentBehavior());
		assertNotNull(config.getAgentConfig().getCapabilities());
		assertFalse(config.getAgentConfig().getCapabilities().enableSubagents());
		assertFalse(config.getAgentConfig().getCapabilities().enableShell());
		assertTrue(config.getAgentConfig().getCapabilities().enableViewFile());
		assertTrue(config.getAgentConfig().getCapabilities().enableListDir());
		assertEquals(1, config.getAgentConfig().getWorkspaces().size());
		assertEquals("/tmp/agy-test", config.getAgentConfig().getWorkspaces().get(0));
		assertEquals(1, config.getAgentConfig().getPolicies().size());
	}

	@Test
	public void testLocalOpenAIAgentConfigMatchingBlogPost() {
		LocalOpenAIAgentConfig config = LocalOpenAIAgentConfig.builder().baseUrl("http://localhost:11434/v1")
				.modelName("gemma4:26b").addWorkspace("/tmp/agy-test").addPolicy(Policies.allowAll()).lightweight()
				.build();

		assertEquals("http://localhost:11434/v1", config.getBaseUrl());
		assertEquals("gemma4:26b", config.getModelName());
		assertEquals("http://localhost:11434/v1", config.getAgentConfig().getBaseUrl());
		assertEquals("gemma4:26b", config.getAgentConfig().getModelName());
		assertEquals(AgentBehavior.MINIMAL, config.getAgentConfig().getAgentBehavior());
		assertEquals(1, config.getAgentConfig().getWorkspaces().size());
		assertEquals("/tmp/agy-test", config.getAgentConfig().getWorkspaces().get(0));
		assertEquals(1, config.getAgentConfig().getPolicies().size());
	}

	@Test
	public void testBuiltinToolsScheduleAndManageTask() {
		assertEquals("schedule", BuiltinTools.SCHEDULE.getValue());
		assertEquals("manage_task", BuiltinTools.MANAGE_TASK.getValue());

		List<BuiltinTools> defaults = BuiltinTools.defaultTools();
		assertTrue(defaults.contains(BuiltinTools.SCHEDULE));
		assertTrue(defaults.contains(BuiltinTools.MANAGE_TASK));
		assertFalse(defaults.contains(BuiltinTools.ASK_QUESTION));

		List<BuiltinTools> nondestructive = BuiltinTools.nondestructive();
		assertTrue(nondestructive.contains(BuiltinTools.SCHEDULE));
		assertTrue(nondestructive.contains(BuiltinTools.MANAGE_TASK));

		List<BuiltinTools> all = BuiltinTools.allTools();
		assertEquals(15, all.size());
		assertTrue(all.contains(BuiltinTools.SCHEDULE));
		assertTrue(all.contains(BuiltinTools.MANAGE_TASK));
	}

	@Test
	public void testCapabilitiesConfigSchedule() {
		CapabilitiesConfig config = CapabilitiesConfig.builder().enableSchedule(true).build();

		assertTrue(config.enableSchedule());

		CapabilitiesConfig disabledConfig = CapabilitiesConfig.builder().enableSchedule(false).build();

		assertFalse(disabledConfig.enableSchedule());
	}

	@Test
	public void testDeprecatedCompactionConfigFields() {
		CompactionConfig config = new CompactionConfig(50000, 10000, 120000);

		assertEquals(50000, config.tokenThreshold());
		assertEquals(10000, config.checkpointIntervalTokens());
		assertEquals(120000, config.maxContextTokens());
	}

	@Test
	public void testHookResultModifiedArgs() {
		Map<String, Object> modified = Map.of("path", "/tmp/updated.txt", "line_count", 42);

		HookResult result = HookResult.allowedWithModifiedArgs(modified);
		assertTrue(result.allow());
		assertEquals(modified, result.modifiedArgs());
		assertNull(result.modifiedArgumentsJson());

		// Test HookResult with JsonNode
		JsonNode jsonArgs = objectMapper.createObjectNode().put("path", "/tmp/json_node.txt").put("count", 10);
		HookResult jsonResult = HookResult.allowedWithModifiedArgs(jsonArgs);
		assertTrue(jsonResult.allow());
		assertNotNull(jsonResult.modifiedArgs());
		assertEquals("/tmp/json_node.txt", jsonResult.modifiedArgs().get("path"));
		assertEquals(10, jsonResult.modifiedArgs().get("count"));

		// Test backward compatible constructor with deprecated modifiedArgumentsJson
		@SuppressWarnings("deprecation")
		HookResult legacyResult = HookResult.allowedWithModifiedArguments("{\"path\":\"/tmp/legacy.txt\"}");
		assertTrue(legacyResult.allow());
		assertEquals("{\"path\":\"/tmp/legacy.txt\"}", legacyResult.modifiedArgumentsJson());
		assertNull(legacyResult.modifiedArgs());

		// Verify Protobuf wire structure compatibility with PreToolResult
		PreToolResult.Builder ptr = PreToolResult.newBuilder();
		Struct.Builder structBuilder = Struct.newBuilder();
		for (Map.Entry<String, Object> entry : modified.entrySet()) {
			Value val;
			if (entry.getValue() instanceof Number n) {
				val = Value.newBuilder().setNumberValue(n.doubleValue()).build();
			} else {
				val = Value.newBuilder().setStringValue(String.valueOf(entry.getValue())).build();
			}
			structBuilder.addFields(Field.newBuilder().setName(entry.getKey()).setValue(val).build());
		}
		ptr.setModifiedArgs(structBuilder.build());

		PreToolResult proto = ptr.build();
		assertTrue(proto.hasModifiedArgs());
		assertEquals(2, proto.getModifiedArgs().getFieldsCount());
	}

	@Test
	public void testSchemaGeneratorKeywordNormalization() throws Exception {
		String inputJson = """
				{
				  "type": "OBJECT",
				  "properties": {
				    "items": {
				      "type": "ARRAY",
				      "min_items": 1,
				      "max_items": 10,
				      "unique_items": true
				    },
				    "extra": {
				      "type": "OBJECT",
				      "additional_properties": false,
				      "pattern_properties": {
				        "^x-": { "type": "STRING" }
				      }
				    }
				  },
				  "description": "An object with min_items and max_items in description",
				  "enum": ["min_items", "max_items"]
				}
				""";

		JsonNode originalNode = objectMapper.readTree(inputJson);
		JsonNode normalizedNode = SchemaGenerator.normalizeSchema(originalNode);

		// Uppercase types converted to lowercase
		assertEquals("object", normalizedNode.get("type").asText());

		// Keywords converted to camelCase
		JsonNode itemsNode = normalizedNode.at("/properties/items");
		assertEquals("array", itemsNode.get("type").asText());
		assertTrue(itemsNode.has("minItems"), "Should have minItems");
		assertFalse(itemsNode.has("min_items"), "Should not have min_items");
		assertEquals(1, itemsNode.get("minItems").asInt());

		assertTrue(itemsNode.has("maxItems"), "Should have maxItems");
		assertFalse(itemsNode.has("max_items"), "Should not have max_items");
		assertEquals(10, itemsNode.get("maxItems").asInt());

		assertTrue(itemsNode.has("uniqueItems"), "Should have uniqueItems");
		assertFalse(itemsNode.has("unique_items"), "Should not have unique_items");
		assertTrue(itemsNode.get("uniqueItems").asBoolean());

		JsonNode extraNode = normalizedNode.at("/properties/extra");
		assertTrue(extraNode.has("additionalProperties"), "Should have additionalProperties");
		assertFalse(extraNode.has("additional_properties"), "Should not have additional_properties");

		assertTrue(extraNode.has("patternProperties"), "Should have patternProperties");
		assertFalse(extraNode.has("pattern_properties"), "Should not have pattern_properties");
		assertEquals("string", extraNode.at("/patternProperties/^x-/type").asText());

		// Literal string values preserved without mutation
		assertEquals("An object with min_items and max_items in description",
				normalizedNode.get("description").asText());
		assertEquals("min_items", normalizedNode.get("enum").get(0).asText());
		assertEquals("max_items", normalizedNode.get("enum").get(1).asText());
	}
}
