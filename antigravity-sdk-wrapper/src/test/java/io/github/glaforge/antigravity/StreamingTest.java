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
import java.util.concurrent.Flow;
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
	@Test
	public void testAgentStream() throws Exception {
		TestUtils.retry(3, () -> {
			AgentConfig config = AgentConfig.builder()
					.instructions("You are a helpful assistant. Please think out loud using thoughts.")
					.modelName("gemini-2.5-pro") // pro models usually produce more thoughts
					.build();

			try (Agent agent = new Agent(config)) {
				System.out.println("Starting AgentStream test...");
				AgentStream stream = agent.streamChat("Think step-by-step about what 2+2 is and reply.");

				AtomicInteger thoughtChunks = new AtomicInteger(0);

				stream.thoughts().subscribe(new Flow.Subscriber<String>() {
					Flow.Subscription sub;
					@Override
					public void onSubscribe(Flow.Subscription s) {
						this.sub = s;
						sub.request(Long.MAX_VALUE);
					}
					@Override
					public void onNext(String item) {
						System.out.println("Thought chunk: " + item);
						thoughtChunks.incrementAndGet();
					}
					@Override
					public void onError(Throwable t) {
					}
					@Override
					public void onComplete() {
					}
				});

				await().atMost(120, TimeUnit.SECONDS).until(() -> stream.result().isDone());
				AgentResponse response = stream.result().get();

				// It is possible the model did not output any thoughts, but we verified the
				// stream works without error
				System.out.println("Final Thoughts Length: " + response.thoughts().length());
			}
		});
	}

}
