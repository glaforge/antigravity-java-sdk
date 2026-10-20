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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import io.github.glaforge.antigravity.localharness.HarnessConfig;
import io.github.glaforge.antigravity.localharness.ModelConfig;

@Tag("unit")
class EnolaFeedbackTest {

	@Test
	void testApiKeyResolutionFromConfigAndEnvironmentVariables() throws Exception {
		// 1. Passing GEMINI_API_KEY via AgentConfig.environmentVariables
		AgentConfig envConfig = AgentConfig.builder()
				.environmentVariables(Map.of("GEMINI_API_KEY", "env-map-secret-key")).build();
		assertEquals("env-map-secret-key", Agent.resolveGeminiApiKey(envConfig));

		Agent envAgent = new Agent(envConfig, false);
		HarnessConfig envProto = envAgent.buildHarnessConfig();
		assertEquals("env-map-secret-key", envProto.getModels(0).getGeminiApiEndpoint().getApiKey());

		// 2. Explicit .apiKey(...) takes highest priority over environmentVariables
		AgentConfig explicitConfig = AgentConfig.builder().apiKey("explicit-direct-key")
				.addEnvironmentVariable("GEMINI_API_KEY", "env-map-secret-key").build();
		assertEquals("explicit-direct-key", explicitConfig.getApiKey());
		assertEquals("explicit-direct-key", Agent.resolveGeminiApiKey(explicitConfig));

		Agent explicitAgent = new Agent(explicitConfig, false);
		HarnessConfig explicitProto = explicitAgent.buildHarnessConfig();
		assertEquals("explicit-direct-key", explicitProto.getModels(0).getGeminiApiEndpoint().getApiKey());

		// 3. Agent.Builder shorthand delegates
		AgentConfig builderConfig = Agent.builder().apiKey("builder-api-key").buildConfig();
		assertEquals("builder-api-key", builderConfig.getApiKey());
	}

	@Test
	void testVertexEndpointConfigurationAndHydration() throws Exception {
		// Explicit Vertex configuration
		AgentConfig vertexConfig = Agent.builder().vertex(true).project("my-gcp-project").location("europe-west1")
				.generation(GenerationConfig.builder().thinkingLevel(ThinkingLevel.LOW).maxOutputTokens(1024).build())
				.buildConfig();
		assertTrue(vertexConfig.isVertex());
		assertEquals("my-gcp-project", vertexConfig.getProject());
		assertEquals("europe-west1", vertexConfig.getLocation());

		Agent vertexAgent = new Agent(vertexConfig, false);
		HarnessConfig proto = vertexAgent.buildHarnessConfig();
		ModelConfig modelProto = proto.getModels(0);
		assertTrue(modelProto.hasVertexEndpoint());
		assertEquals("my-gcp-project", modelProto.getVertexEndpoint().getProject());
		assertEquals("europe-west1", modelProto.getVertexEndpoint().getLocation());
		assertEquals("low", modelProto.getVertexEndpoint().getOptions().getThinkingLevel());
		assertEquals(1024, modelProto.getVertexEndpoint().getOptions().getMaxOutputTokens());

		// Auto-detection via GOOGLE_GENAI_USE_VERTEXAI in environmentVariables
		AgentConfig autoVertexConfig = AgentConfig.builder().addEnvironmentVariable("GOOGLE_GENAI_USE_VERTEXAI", "true")
				.addEnvironmentVariable("GOOGLE_CLOUD_PROJECT", "env-project")
				.addEnvironmentVariable("GOOGLE_CLOUD_LOCATION", "us-central1").build();
		assertTrue(autoVertexConfig.isVertex());
		assertEquals("env-project", autoVertexConfig.getProject());
		assertEquals("us-central1", autoVertexConfig.getLocation());
	}

	@Test
	void testWorkspaceSynchronizationAndAppDataDirValidation() {
		// Default config populates getWorkspaces() from default workspaceDir (user.dir)
		AgentConfig defaultConfig = AgentConfig.builder().build();
		assertNotNull(defaultConfig.getWorkspaceDir());
		assertEquals(1, defaultConfig.getWorkspaces().size());
		assertEquals(defaultConfig.getWorkspaceDir().toAbsolutePath().normalize().toString(),
				defaultConfig.getWorkspaces().get(0));

		// Setting workspaceDir updates default workspaces list
		Path customDir = Path.of("/tmp/my-custom-workspace");
		AgentConfig dirConfig = Agent.builder().workspaceDir(customDir).buildConfig();
		assertEquals(customDir, dirConfig.getWorkspaceDir());
		assertEquals(List.of("/tmp/my-custom-workspace"), dirConfig.getWorkspaces());

		// Setting workspaces updates workspaceDir if workspaceDir wasn't explicitly set
		AgentConfig wsListConfig = AgentConfig.builder().workspaces(List.of("/tmp/ws-1", "/tmp/ws-2")).build();
		assertEquals(Path.of("/tmp/ws-1"), wsListConfig.getWorkspaceDir());
		assertEquals(List.of("/tmp/ws-1", "/tmp/ws-2"), wsListConfig.getWorkspaces());

		// Relative appDataDir is rejected
		assertThrows(IllegalArgumentException.class,
				() -> AgentConfig.builder().appDataDir("relative/app/data").build());

		// Absolute appDataDir is accepted
		AgentConfig absAppData = AgentConfig.builder().appDataDir("/tmp/abs-app-data").build();
		assertEquals("/tmp/abs-app-data", absAppData.getAppDataDir());
	}

	@Test
	void testConversationHistoryFromInitializeResponseAndLiveTurns() throws Exception {
		Agent agent = new Agent(AgentConfig.builder().build(), false);
		assertTrue(agent.getHistory().isEmpty());

		// 1. Simulate InitializeConversationResponse with resumed history from .db
		String initJson = """
				{
				  "initializeConversationResponse": {
				    "cascadeId": "resumed-conv-123",
				    "history": [
				      {
				        "trajectoryId": "traj-main",
				        "stepIndex": 0,
				        "source": "SOURCE_USER",
				        "state": "STATE_DONE",
				        "target": "TARGET_MODEL",
				        "requestText": "List the files in the workspace"
				      },
				      {
				        "trajectoryId": "traj-main",
				        "stepIndex": 1,
				        "source": "SOURCE_MODEL",
				        "state": "STATE_DONE",
				        "target": "TARGET_ENVIRONMENT",
				        "thinking": "I should list the directory first.",
				        "listDirectory": {
				          "path": "/workspace"
				        }
				      },
				      {
				        "trajectoryId": "traj-main",
				        "stepIndex": 2,
				        "source": "SOURCE_MODEL",
				        "state": "STATE_DONE",
				        "target": "TARGET_USER",
				        "text": "Here are the files in /workspace: README.md and pom.xml."
				      }
				    ]
				  }
				}
				""";
		agent.handleIncomingMessage(null, initJson);

		assertEquals("resumed-conv-123", agent.getConversationId());
		List<ConversationStep> history = agent.getHistory();
		assertEquals(3, history.size());

		ConversationStep userStep = history.get(0);
		assertEquals("traj-main:0", userStep.id());
		assertEquals("SOURCE_USER", userStep.source());
		assertEquals("List the files in the workspace", userStep.content());
		assertFalse(userStep.completeResponse());

		ConversationStep toolStep = history.get(1);
		assertEquals("traj-main:1", toolStep.id());
		assertEquals("I should list the directory first.", toolStep.thinking());
		assertEquals(1, toolStep.toolCalls().size());
		assertEquals("list_dir", toolStep.toolCalls().get(0).name());

		ConversationStep modelStep = history.get(2);
		assertEquals("traj-main:2", modelStep.id());
		assertEquals("Here are the files in /workspace: README.md and pom.xml.", modelStep.content());
		assertTrue(modelStep.completeResponse());

		// 2. Simulate streaming StepUpdates during a subsequent live turn (same
		// stepIndex updated in-place)
		agent.handleIncomingMessage(null, """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-main",
				    "stepIndex": 3,
				    "source": "SOURCE_MODEL",
				    "state": "STATE_ACTIVE",
				    "target": "TARGET_USER",
				    "thinkingDelta": "Thinking part 1. ",
				    "textDelta": "Hello "
				  }
				}
				""");
		agent.handleIncomingMessage(null, """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-main",
				    "stepIndex": 3,
				    "source": "SOURCE_MODEL",
				    "state": "STATE_DONE",
				    "target": "TARGET_USER",
				    "thinkingDelta": "Thinking part 2.",
				    "textDelta": "world!"
				  }
				}
				""");

		List<ConversationStep> updatedHistory = agent.getHistory();
		assertEquals(4, updatedHistory.size());
		ConversationStep liveStep = updatedHistory.get(3);
		assertEquals("traj-main:3", liveStep.id());
		assertEquals("STATE_DONE", liveStep.state());
		assertEquals("Hello world!", liveStep.content());
		assertEquals("Thinking part 1. Thinking part 2.", liveStep.thinking());
		assertTrue(liveStep.completeResponse());

		// 3. Verify clearHistory()
		agent.clearHistory();
		assertTrue(agent.getHistory().isEmpty());
	}
}
