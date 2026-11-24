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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.glaforge.antigravity.hooks.ToolCall;
import io.github.glaforge.antigravity.localharness.HarnessConfig;
import io.github.glaforge.antigravity.localharness.InputConfig;
import io.github.glaforge.antigravity.localharness.SubagentsConfig;
import io.github.glaforge.antigravity.localharness.ThinkingEffort;
import io.github.glaforge.antigravity.tools.Tool;
import io.github.glaforge.antigravity.tools.ToolRegistry;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity0124Test {

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	@Test
	public void testDefaultModelAndThinkingLevelExtraHigh() {
		assertEquals("gemini-4-hydrogen", AgentConfig.DEFAULT_MODEL_NAME);
		assertEquals("gemini-4-hydrogen", AgentConfig.builder().build().getModelName());

		assertEquals("xhigh", ThinkingLevel.EXTRA_HIGH.getValue());
		assertEquals(ThinkingLevel.EXTRA_HIGH, ThinkingLevel.fromValue("xhigh"));
		assertEquals(ThinkingLevel.EXTRA_HIGH, ThinkingLevel.fromValue("extra_high"));
		assertEquals(ThinkingLevel.EXTRA_HIGH, ThinkingLevel.fromValue("EXTRA_HIGH"));

		GenerationConfig genConfig = GenerationConfig.builder().thinkingLevel(ThinkingLevel.EXTRA_HIGH).build();
		assertEquals(ThinkingLevel.EXTRA_HIGH, genConfig.thinkingLevel());
	}

	@Test
	public void testProtocolV0124Additions() {
		assertEquals(0, ThinkingEffort.THINKING_EFFORT_UNSPECIFIED.getNumber());
		assertEquals(1, ThinkingEffort.THINKING_EFFORT_MINIMAL.getNumber());
		assertEquals(2, ThinkingEffort.THINKING_EFFORT_LOW.getNumber());
		assertEquals(3, ThinkingEffort.THINKING_EFFORT_MEDIUM.getNumber());
		assertEquals(4, ThinkingEffort.THINKING_EFFORT_HIGH.getNumber());
		assertEquals(5, ThinkingEffort.THINKING_EFFORT_MAX.getNumber());

		SubagentsConfig subagents = SubagentsConfig.newBuilder().setDisableManageSubagentsTool(true).build();
		assertTrue(subagents.getDisableManageSubagentsTool());

		InputConfig inputConfig = InputConfig.newBuilder().setStorageDirectory("/tmp/ag").build();
		assertEquals("/tmp/ag", inputConfig.getStorageDirectory());
		assertNull(InputConfig.getDescriptor().findFieldByName("use_interactions_api"));
	}

	@Test
	public void testGeminiBaseUrlResolvedFromHarnessEnvAndSkippedForVertex() throws Exception {
		AgentConfig geminiWithEnvUrl = AgentConfig.builder()
				.environmentVariables(Map.of("GOOGLE_GEMINI_BASE_URL", "https://custom-gemini.example.com")).build();
		Agent geminiAgent = new Agent(geminiWithEnvUrl, false);
		HarnessConfig geminiHarness = geminiAgent.buildHarnessConfig("test-key");
		assertTrue(geminiHarness.getModels(0).hasGeminiApiEndpoint());
		assertEquals("https://custom-gemini.example.com",
				geminiHarness.getModels(0).getGeminiApiEndpoint().getBaseUrl());

		AgentConfig vertexWithEnvUrl = AgentConfig.builder().vertex(true).project("p").location("us-central1")
				.environmentVariables(Map.of("GOOGLE_GEMINI_BASE_URL", "https://custom-gemini.example.com")).build();
		Agent vertexAgent = new Agent(vertexWithEnvUrl, false);
		HarnessConfig vertexHarness = vertexAgent.buildHarnessConfig("test-key");
		assertTrue(vertexHarness.getModels(0).hasVertexEndpoint());
		assertEquals("", vertexHarness.getModels(0).getVertexEndpoint().getBaseUrl());
	}

	@Test
	public void testLiteRTAgentConfigVisionBackend() {
		LiteRTAgentConfig autoConfig = LiteRTAgentConfig.builder().modelPath("/tmp/gemma4.litertlm").build();
		assertNull(autoConfig.getVisionBackend());

		LiteRTAgentConfig overrideConfig = LiteRTAgentConfig.builder().modelPath("/tmp/gemma4.litertlm")
				.backend(LiteRTAgentConfig.Backend.GPU).visionBackend(LiteRTAgentConfig.Backend.CPU).build();
		assertEquals(LiteRTAgentConfig.Backend.GPU, overrideConfig.getBackend());
		assertEquals(LiteRTAgentConfig.Backend.CPU, overrideConfig.getVisionBackend());
	}

	@Test
	public void testNormalizeWirePathCollapsesDotSegments() {
		assertEquals("/etc/passwd", ToolCall.normalizeWirePath("file:///dev/shm/workspace/../../../etc/passwd"));
		assertEquals("/cns/el-d/home/other/kittens.md",
				ToolCall.normalizeWirePath("cns://el-d/home/user/workspace/../../other/kittens.md"));
		assertEquals("/etc/passwd", ToolCall.normalizeWirePath("/tmp/ws/../../etc/passwd"));
		assertEquals("https://example.com/a/../b", ToolCall.normalizeWirePath("https://example.com/a/../b"));

		ObjectNode args = MAPPER.createObjectNode();
		args.put("file_path", "file:///workspace/sub/../../secret.txt");
		ToolCall call = new ToolCall("view_file", args, "call-1", "traj:1", null);
		assertEquals("/secret.txt", call.canonicalPath());
		assertEquals("/secret.txt", call.args().path("file_path").asText());
	}

	public record ChartPayload(String title, AgentInput.Image chart) {
	}

	public static class MediaReturningTools {
		@Tool(name = "single_image", description = "Returns a single image")
		public AgentInput.Image singleImage() {
			return new AgentInput.Image("image/png", new byte[]{1, 2, 3, 4}, "chart preview");
		}

		@Tool(name = "media_record", description = "Returns a record with text and media")
		public ChartPayload mediaRecord() {
			return new ChartPayload("Q4 Revenue", new AgentInput.Image("image/jpeg", new byte[]{5, 6}, "q4 chart"));
		}

		@Tool(name = "media_list", description = "Returns mixed list with audio and text")
		public List<Object> mediaList() {
			return List.of("done", new AgentInput.Audio("audio/wav", new byte[]{7, 8}, "clip"));
		}

		@Tool(name = "media_map", description = "Returns map with document and note")
		public Map<String, Object> mediaMap() {
			Map<String, Object> mixedMap = new LinkedHashMap<>();
			mixedMap.put("note", "ok");
			mixedMap.put("doc",
					new AgentInput.Document("application/pdf", "pdf".getBytes(StandardCharsets.UTF_8), "spec"));
			return mixedMap;
		}
	}

	@Test
	public void testToolMediaReturnValuesExtractedToSupplementalMedia() throws Exception {
		ToolRegistry registry = new ToolRegistry();
		registry.registerToolsFromObject(new MediaReturningTools());

		ToolRegistry.ToolExecutionResult imgRes = registry.executeWithMedia("single_image", MAPPER.createObjectNode(),
				null);
		assertEquals(1, imgRes.supplementalMedia().size());
		assertEquals("image/png", imgRes.supplementalMedia().get(0).mimeType());
		assertArrayEquals(new byte[]{1, 2, 3, 4}, imgRes.supplementalMedia().get(0).data());
		assertTrue(imgRes.responseJson().contains("Returned 1 media attachment(s)."));

		ToolRegistry.ToolExecutionResult recRes = registry.executeWithMedia("media_record", MAPPER.createObjectNode(),
				null);
		assertEquals(1, recRes.supplementalMedia().size());
		assertEquals("image/jpeg", recRes.supplementalMedia().get(0).mimeType());
		JsonNode recJson = MAPPER.readTree(recRes.responseJson());
		assertEquals("Q4 Revenue", recJson.path("title").asText());
		assertFalse(recJson.has("chart"));

		ToolRegistry.ToolExecutionResult listRes = registry.executeWithMedia("media_list", MAPPER.createObjectNode(),
				null);
		assertEquals(1, listRes.supplementalMedia().size());
		assertEquals("audio/wav", listRes.supplementalMedia().get(0).mimeType());
		assertEquals("[\"done\"]", listRes.responseJson());

		ToolRegistry.ToolExecutionResult mapRes = registry.executeWithMedia("media_map", MAPPER.createObjectNode(),
				null);
		assertEquals(1, mapRes.supplementalMedia().size());
		assertEquals("application/pdf", mapRes.supplementalMedia().get(0).mimeType());
		JsonNode mapJson = MAPPER.readTree(mapRes.responseJson());
		assertEquals("ok", mapJson.path("note").asText());
		assertFalse(mapJson.has("doc"));
	}

	@Test
	public void testCompactionTrackingAndMultiTurnStateIsolation() throws Exception {
		Agent agent = new Agent(AgentConfig.builder().build(), false);

		// Turn 1: structured finish + active compaction + subagent compaction +
		// completed root compaction
		CompletableFuture<AgentResponse> turn1 = agent.chat("Turn 1");
		agent.handleIncomingMessage(null, """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-root",
				    "stepIndex": 0,
				    "depth": 0,
				    "state": "STATE_ACTIVE",
				    "compaction": {}
				  }
				}
				""");
		assertEquals(List.of(), agent.getCompactionIndices());

		agent.handleIncomingMessage(null, """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-sub",
				    "parentTrajectoryId": "traj-root",
				    "stepIndex": 0,
				    "depth": 1,
				    "state": "STATE_DONE",
				    "compaction": {}
				  }
				}
				""");
		assertEquals(List.of(), agent.getCompactionIndices());

		agent.handleIncomingMessage(null, """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-root",
				    "stepIndex": 0,
				    "depth": 0,
				    "state": "STATE_DONE",
				    "compaction": {}
				  }
				}
				""");
		assertEquals(List.of(0), agent.getCompactionIndices());

		agent.handleIncomingMessage(null, """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-root",
				    "stepIndex": 1,
				    "depth": 0,
				    "state": "STATE_DONE",
				    "finish": { "outputString": "{\\"answer\\": 42}" }
				  }
				}
				""");
		agent.handleIncomingMessage(null, """
				{
				  "trajectoryStateUpdate": {
				    "trajectoryId": "traj-root",
				    "state": "STATE_IDLE",
				    "stopReason": "STOP_REASON_MAX_MODEL_CALLS_EXCEEDED"
				  }
				}
				""");

		AgentResponse resp1 = turn1.get(5, TimeUnit.SECONDS);
		assertEquals("{\"answer\": 42}", resp1.text());
		assertEquals(StopReason.MAX_MODEL_CALLS_EXCEEDED, resp1.stopReason());

		// Turn 2: plain text turn must not retain Turn 1 structured output or stop
		// reason
		CompletableFuture<AgentResponse> turn2 = agent.chat("Turn 2");
		agent.handleIncomingMessage(null, """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-root",
				    "stepIndex": 2,
				    "depth": 0,
				    "source": "SOURCE_MODEL",
				    "target": "TARGET_USER",
				    "state": "STATE_DONE",
				    "textDelta": "Plain reply"
				  }
				}
				""");
		agent.handleIncomingMessage(null, """
				{
				  "trajectoryStateUpdate": {
				    "trajectoryId": "traj-root",
				    "state": "STATE_IDLE"
				  }
				}
				""");

		AgentResponse resp2 = turn2.get(5, TimeUnit.SECONDS);
		assertEquals("Plain reply", resp2.text());
		assertEquals(StopReason.UNSPECIFIED, resp2.stopReason());
		// Turn 1 response snapshot stays isolated
		assertEquals("{\"answer\": 42}", resp1.text());
		assertEquals(StopReason.MAX_MODEL_CALLS_EXCEEDED, resp1.stopReason());
	}
}
