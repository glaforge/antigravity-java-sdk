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
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class ObservabilityTest {

	@Test
	@Timeout(value = 240, unit = TimeUnit.SECONDS)
	public void testUsageObservability() throws Exception {
		TestUtils.retry(2, () -> {
			AgentConfig config = AgentConfig.builder().instructions("You are a helpful assistant.")
					.modelName("gemini-3.6-flash").build();

			try (Agent agent = new Agent(config)) {
				CompletableFuture<AgentResponse> future = agent.chat("Hi, say exactly one word: Hello.");
				await().atMost(90, TimeUnit.SECONDS).until(future::isDone);
				AgentResponse response = future.get();

				// Verify usage metadata is populated
				UsageMetadata usage = response.usageMetadata();
				assertNotNull(usage, "UsageMetadata should not be null in response");
				assertEquals(usage, response.usage(), "response.usage() should match response.usageMetadata()");
				assertTrue(usage.promptTokenCount() > 0, "Prompt tokens should be > 0");
				assertTrue(usage.totalTokenCount() > 0, "Total tokens should be > 0");

				// Verify Agent getters
				assertNotNull(agent.getUsageMetadata(), "agent.getUsageMetadata() should not be null");
				assertNotNull(agent.getTotalUsage(), "agent.getTotalUsage() should not be null");
				assertTrue(agent.getTotalUsage().totalTokenCount() > 0, "Cumulative total tokens should be > 0");
			}
		});
	}
}
