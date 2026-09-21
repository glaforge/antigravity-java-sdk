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
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
public class StepUpdateFilterTest {

	@Test
	public void testStepUpdateIgnoresUserAndSystemDeltas() throws Exception {
		AgentConfig config = AgentConfig.builder().instructions("Test assistant").build();
		try (Agent agent = new Agent(config, false)) {
			List<AgentResponseChunk> chunks = new ArrayList<>();
			agent.initTurnForTest(chunks::add);

			// 1. Simulate harness broadcasting SOURCE_USER echo
			String userEchoPayload = """
					{
					  "stepUpdate": {
					    "cascadeId": "test-cascade",
					    "source": "SOURCE_USER",
					    "stepIndex": 0,
					    "textDelta": "What is the current weather in Paris?"
					  }
					}
					""";
			agent.handleIncomingMessage(null, userEchoPayload);

			// Must not append user query to response text or emit chunks
			assertEquals("", agent.getCurrentTextForTest(), "User prompt echo must not be appended to response text");
			assertTrue(chunks.isEmpty(), "User prompt echo must not be emitted to streaming chunk consumer");

			// 2. Simulate harness broadcasting SOURCE_SYSTEM event
			String systemPayload = """
					{
					  "stepUpdate": {
					    "source": "SOURCE_SYSTEM",
					    "stepIndex": 1,
					    "textDelta": "System environment initialized"
					  }
					}
					""";
			agent.handleIncomingMessage(null, systemPayload);
			assertEquals("", agent.getCurrentTextForTest(), "System step deltas must not be appended to response text");
			assertTrue(chunks.isEmpty(), "System step deltas must not be emitted to streaming chunk consumer");

			// 3. Simulate harness broadcasting SOURCE_MODEL streaming deltas
			String modelDelta1 = """
					{
					  "stepUpdate": {
					    "source": "SOURCE_MODEL",
					    "stepIndex": 2,
					    "thinkingDelta": "Checking weather for Paris",
					    "textDelta": "The current weather in Paris "
					  }
					}
					""";
			agent.handleIncomingMessage(null, modelDelta1);

			assertEquals("The current weather in Paris ", agent.getCurrentTextForTest());
			assertEquals("Checking weather for Paris", agent.getCurrentThoughtsForTest());
			assertEquals(1, chunks.size());
			assertEquals("The current weather in Paris ", chunks.get(0).textDelta());
			assertEquals("Checking weather for Paris", chunks.get(0).thoughtsDelta());

			// 4. Simulate second SOURCE_MODEL chunk
			String modelDelta2 = """
					{
					  "stepUpdate": {
					    "source": "SOURCE_MODEL",
					    "stepIndex": 2,
					    "textDelta": "is 22°C and Sunny."
					  }
					}
					""";
			agent.handleIncomingMessage(null, modelDelta2);

			assertEquals("The current weather in Paris is 22°C and Sunny.", agent.getCurrentTextForTest());
			assertEquals(2, chunks.size());
			assertEquals("is 22°C and Sunny.", chunks.get(1).textDelta());

			// 5. Simulate TARGET_ENVIRONMENT action step (e.g. tool execution
			// title/rationale)
			agent.initTurnForTest(chunks::add);
			chunks.clear();

			String envDelta = """
					{
					  "stepUpdate": {
					    "source": "SOURCE_MODEL",
					    "target": "TARGET_ENVIRONMENT",
					    "stepIndex": 1,
					    "textDelta": "Tokyo weather check"
					  }
					}
					""";
			agent.handleIncomingMessage(null, envDelta);

			assertEquals("", agent.getCurrentTextForTest(),
					"TARGET_ENVIRONMENT text must not be appended to response text");
			assertEquals("Tokyo weather check", agent.getCurrentThoughtsForTest(),
					"TARGET_ENVIRONMENT text must be routed to thoughts");
			assertEquals(1, chunks.size());
			assertEquals("", chunks.get(0).textDelta());
			assertEquals("Tokyo weather check", chunks.get(0).thoughtsDelta());
		}
	}
}
