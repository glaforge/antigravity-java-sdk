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
import io.github.glaforge.antigravity.tools.ToolRegistry;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity019Test {

	@Test
	public void testBuiltinToolsExports() {
		assertEquals("list_directory", BuiltinTools.LIST_DIR.getValue());
		assertEquals("run_command", BuiltinTools.RUN_COMMAND.getValue());
		assertEquals("search_web", BuiltinTools.SEARCH_WEB.getValue());
		assertEquals("read_url_content", BuiltinTools.READ_URL_CONTENT.getValue());
		assertEquals("finish", BuiltinTools.FINISH.getValue());

		assertTrue(BuiltinTools.readOnly().contains(BuiltinTools.VIEW_FILE));
		assertFalse(BuiltinTools.readOnly().contains(BuiltinTools.RUN_COMMAND));

		assertTrue(BuiltinTools.nondestructive().contains(BuiltinTools.CREATE_FILE));
		assertFalse(BuiltinTools.nondestructive().contains(BuiltinTools.RUN_COMMAND));

		assertTrue(BuiltinTools.fileTools().contains(BuiltinTools.EDIT_FILE));
		assertEquals(13, BuiltinTools.allTools().size());
		assertTrue(BuiltinTools.none().isEmpty());
	}

	@Test
	public void testRetryConfigAndPresets() {
		RetryConfig benchmarkCfg = RetryConfig.benchmark();
		assertNotNull(benchmarkCfg.apiRetry());
		assertEquals(5, benchmarkCfg.apiRetry().maxRetries());
		assertEquals(1000, benchmarkCfg.apiRetry().initialSleepDurationMs());
		assertEquals(2.0, benchmarkCfg.apiRetry().exponentialMultiplier());
		assertEquals(0.2, benchmarkCfg.apiRetry().jitterRange());

		assertNotNull(benchmarkCfg.modelOutputRetry());
		assertEquals(3, benchmarkCfg.modelOutputRetry().maxRetries());

		var apiProto = benchmarkCfg.apiRetry().toProtobuf();
		var outputProto = benchmarkCfg.modelOutputRetry().toProtobuf();
		assertEquals(5, apiProto.getMaxRetries());
		assertEquals(3, outputProto.getMaxRetries());
	}

	@Test
	public void testDebugConfigDefaults() {
		DebugConfig defaults = DebugConfig.defaults();
		assertTrue(defaults.enableServerSideTracing());
		assertEquals("DEBUG", defaults.loggingLevel());

		DebugConfig custom = new DebugConfig(false, "INFO");
		assertFalse(custom.enableServerSideTracing());
		assertEquals("INFO", custom.loggingLevel());
	}

	@Test
	public void testToolExecutionErrorStructure() {
		RuntimeException cause = new IllegalArgumentException("Invalid parameter x");
		ToolExecutionError error = new ToolExecutionError("calculate_sum", "{\"x\": -1}", cause);

		assertEquals("calculate_sum", error.getToolName());
		assertEquals("{\"x\": -1}", error.getArgumentsJson());
		assertEquals("Invalid parameter x", error.getMessage());
		assertEquals(cause, error.getCause());
	}

	@Test
	public void testAudioAgentInput() {
		byte[] audioBytes = new byte[]{1, 2, 3, 4, 5};
		AgentInput.Audio audioInput = new AgentInput.Audio("audio/mp3", audioBytes, "Meeting recording snippet");

		assertEquals("audio/mp3", audioInput.mimeType());
		assertArrayEquals(audioBytes, audioInput.data());
		assertEquals("Meeting recording snippet", audioInput.description());
	}

	@Test
	public void testAutomaticToolNameResolution() {
		class MeetingSummarizer {
			public String summarize(String meetingNotes) {
				return "Summary: " + meetingNotes;
			}
		}

		MeetingSummarizer summarizer = new MeetingSummarizer();
		ToolRegistry registry = new ToolRegistry();
		registry.registerToolsFromObject(summarizer);

		assertDoesNotThrow(() -> {
			String result = registry.execute("summarize", com.fasterxml.jackson.databind.json.JsonMapper.builder()
					.build().createObjectNode().put("meetingNotes", "Discussed v0.1.9 release"), null);
			assertTrue(result.contains("Discussed v0.1.9 release"));
		});
	}

	@Test
	public void testAgentConfigWithV019Features() {
		AgentConfig config = AgentConfig.builder()
				.instructions("Analyze meeting audio and automatically retry transient errors")
				.retryConfig(RetryConfig.benchmark()).debugConfig(DebugConfig.defaults()).build();

		assertNotNull(config.getRetryConfig());
		assertEquals(5, config.getRetryConfig().apiRetry().maxRetries());
		assertNotNull(config.getDebugConfig());
		assertTrue(config.getDebugConfig().enableServerSideTracing());
	}
}
