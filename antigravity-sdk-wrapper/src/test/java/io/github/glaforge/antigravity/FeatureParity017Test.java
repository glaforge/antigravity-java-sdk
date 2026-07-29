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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import io.github.glaforge.antigravity.tools.Tool;

import io.github.glaforge.antigravity.ToolContext;
import io.github.glaforge.antigravity.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

public class FeatureParity017Test {

	@Test
	void testSessionContextAtomicUpdates() {
		SessionContext context = new SessionContext();
		context.set("counter", 1);

		// Test atomic update
		Object updated = context.update("counter", (k, v) -> ((Integer) v) + 10);
		assertEquals(11, updated);
		assertEquals(11, context.get("counter", 0));

		// Test computeIfAbsent
		Object computed = context.computeIfAbsent("missing", k -> "computed_val");
		assertEquals("computed_val", computed);
		assertEquals("computed_val", context.get("missing", null));

		// Test merge
		Object merged = context.merge("counter", 5, (oldV, newV) -> ((Integer) oldV) + ((Integer) newV));
		assertEquals(16, merged);
	}

	@Test
	void testCustomEnvironmentVariablesAndVertexHydration() {
		AgentConfig config = AgentConfig.builder().environmentVariables(Map.of("CUSTOM_VAR", "value1"))
				.addEnvironmentVariable("EXTRA_VAR", "value2").build();

		assertEquals("value1", config.getEnvironmentVariables().get("CUSTOM_VAR"));
		assertEquals("value2", config.getEnvironmentVariables().get("EXTRA_VAR"));

		// Vertex hydration test
		assertEquals("my-gcp-project", AgentConfig.hydrateVertexProject("my-gcp-project"));
		assertEquals("us-central1", AgentConfig.hydrateVertexLocation("us-central1"));
	}

	@Test
	void testThinkingLevelAndGenerationConfig() {
		assertEquals(ThinkingLevel.EXTRA_HIGH, ThinkingLevel.fromValue("extra_high"));
		assertEquals(ThinkingLevel.HIGH, ThinkingLevel.fromValue("HIGH"));

		GenerationConfig genConfig = GenerationConfig.builder().temperature(0.7).thinkingLevel(ThinkingLevel.EXTRA_HIGH)
				.build();

		assertEquals(ThinkingLevel.EXTRA_HIGH, genConfig.thinkingLevel());

		GenerationConfig genConfigString = GenerationConfig.builder().thinkingLevel("extra_high").build();

		assertEquals(ThinkingLevel.EXTRA_HIGH, genConfigString.thinkingLevel());
	}

	@Test
	void testCapabilitiesConfigImageGen() {
		CapabilitiesConfig cap = CapabilitiesConfig.builder().enableGenerateImage(true)
				.imageModelName("custom-image-model").build();

		assertTrue(cap.enableGenerateImage());
		assertEquals("custom-image-model", cap.imageModelName());
		assertEquals("gemini-3.1-flash-lite-image", AgentConfig.DEFAULT_IMAGE_GENERATION_MODEL);
	}

	@Test
	void testLiteRTAgentConfig() {
		LiteRTAgentConfig litert = LiteRTAgentConfig.builder().modelPath("/tmp/model.litertlm")
				.backend(LiteRTAgentConfig.Backend.GPU).port(8080).instructions("Local Gemma assistant").build();

		assertEquals("/tmp/model.litertlm", litert.getModelPath());
		assertEquals(LiteRTAgentConfig.Backend.GPU, litert.getBackend());
		assertEquals(8080, litert.getPort());
		assertEquals("gemma-local", litert.getAgentConfig().getModelName());
		assertEquals("Local Gemma assistant", litert.getAgentConfig().getInstructions());
	}

	@Test
	void testLocalOpenAIAgentConfig() {
		LocalOpenAIAgentConfig openai = LocalOpenAIAgentConfig.builder().baseUrl("http://localhost:11434/v1")
				.modelName("llama3").instructions("Local Ollama assistant").build();

		assertEquals("http://localhost:11434/v1", openai.getBaseUrl());
		assertEquals("http://localhost:11434/v1", openai.getAgentConfig().getBaseUrl());
		assertEquals("llama3", openai.getModelName());
		assertEquals("llama3", openai.getAgentConfig().getModelName());
		assertEquals("Local Ollama assistant", openai.getAgentConfig().getInstructions());
	}

	@Test
	void testAsyncToolExecutionInRegistry() throws Exception {
		ToolRegistry registry = new ToolRegistry();
		registry.registerToolsFromObject(new AsyncToolService());

		String result = registry.execute("asyncHello", null, null);
		assertNotNull(result);
		assertTrue(result.contains("Hello Async World"));
	}

	public static class AsyncToolService {
		@Tool(description = "Returns an asynchronous completion stage")
		public CompletableFuture<String> asyncHello() {
			return CompletableFuture.completedFuture("Hello Async World");
		}
	}
}
