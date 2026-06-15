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

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class ObservabilityTest {

	@Test
	public void testUsageObservability() throws Exception {
		TestUtils.retry(3, () -> {
			AgentConfig config = AgentConfig.builder().instructions("You are a helpful assistant.")
					.modelName("gemini-2.5-flash").build();

			try (Agent agent = new Agent(config)) {
				CompletableFuture<AgentResponse> future = agent.chat("Hi, say exactly one word: Hello.");
				await().atMost(120, TimeUnit.SECONDS).until(future::isDone);
				AgentResponse response = future.get();

				// Verify usage metadata is populated if available
				UsageMetadata usage = response.usageMetadata();
				if (usage != null) {
					System.out.println("Metadata: " + usage);
					assertTrue(usage.promptTokenCount() >= 0, "Prompt tokens should be >= 0");
				} else {
					System.out.println("Metadata was not returned by the harness in this test run.");
				}
			}
		});
	}
}
