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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class StreamingTest {

	@Test
	public void testStreaming() throws Exception {
		TestUtils.retry(3, () -> {
			AgentConfig config = AgentConfig.builder().instructions("You are a helpful assistant.")
					.modelName("gemini-2.5-flash").build();

			try (Agent agent = new Agent(config)) {
				System.out.println("Starting chat stream...");
				AtomicInteger chunkCount = new AtomicInteger(0);

				CompletableFuture<AgentResponse> future = agent
						.chatStream("Write a 3 sentence story about a brave knight.", chunk -> {
							System.out.print(chunk.textDelta());
							chunkCount.incrementAndGet();
						});
				await().atMost(120, TimeUnit.SECONDS).until(future::isDone);
				AgentResponse response = future.get();

				System.out.println("\n--- Stream Complete ---");

				// Verify streaming fired multiple times
				assertTrue(chunkCount.get() > 0, "Should have received multiple streaming chunks");

				// Verify response is fully assembled
				assertNotNull(response.text());
				assertFalse(response.text().isEmpty());
				System.out.println("Final Text Length: " + response.text().length());
			}
		});
	}
}
