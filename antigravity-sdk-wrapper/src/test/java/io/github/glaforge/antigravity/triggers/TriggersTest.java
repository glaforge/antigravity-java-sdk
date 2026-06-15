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
package io.github.glaforge.antigravity.triggers;

import io.github.glaforge.antigravity.Agent;
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.AgentResponse;
import io.github.glaforge.antigravity.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TriggersTest {

	@Test
	public void testBuiltInTrigger() throws Exception {
		TestUtils.retry(3, () -> {
			AtomicInteger triggerCount = new AtomicInteger(0);

			AgentConfig config = AgentConfig.builder().instructions("You are a helpful assistant.")
					.addTrigger(Triggers.every(2000, TimeUnit.MILLISECONDS, ctx -> {
						System.out.println("Trigger fired!");
						triggerCount.incrementAndGet();
						ctx.fireTrigger("The user just sneezed. Say bless you.");
					})).build();

			try (Agent agent = new Agent(config)) {
				// Wait for the trigger to fire multiple times
				await().atMost(10, TimeUnit.SECONDS).until(() -> triggerCount.get() >= 1);

				CompletableFuture<AgentResponse> future = agent.chat("What is 2+2?");
				await().atMost(120, TimeUnit.SECONDS).until(future::isDone);

				AgentResponse response = future.get();
				System.out.println(response.text());
				assertTrue(response.text().toLowerCase().contains("bless you"), "Agent should have said bless you");
			}

			// Wait briefly to ensure trigger is stopped
			int countAfterClose = triggerCount.get();
			Thread.sleep(300);
			assertTrue(triggerCount.get() == countAfterClose, "Trigger should be stopped after agent is closed");
		});
	}
}
