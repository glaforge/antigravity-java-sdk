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
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class BackgroundTriggersTest {

	@Test
	public void testBackgroundTriggers() throws Exception {
		TestUtils.retry(3, () -> {
			AgentConfig config = AgentConfig.builder().persona("""
					You are an assistant. Wait for me to give you a command.
					If I sneeze, immediately say 'Bless you'.
					""").build();

			try (Agent agent = new Agent(config)) {
				// Asynchronously fire a trigger after a brief delay
				Executors.newSingleThreadScheduledExecutor().schedule(() -> {
					System.out.println("Firing background trigger...");
					agent.fireTrigger("The user has just sneezed. Say bless you.");
				}, 500, TimeUnit.MILLISECONDS);

				CompletableFuture<AgentResponse> future = agent.getConversation()
						.chat("What is the weather in Tokyo right now?");
				await().atMost(120, TimeUnit.SECONDS).until(future::isDone);
				AgentResponse response = future.get();
				System.out.println(response.getText());
				assertNotNull(response.getText());
			}
		});
	}
}
