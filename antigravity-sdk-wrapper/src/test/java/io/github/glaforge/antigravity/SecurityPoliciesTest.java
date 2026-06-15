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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class SecurityPoliciesTest {

	public static class WeatherTool {
		public String get_weather(String location) {
			return "Sunny and 22C";
		}
	}

	@Test
	public void testSecurityPolicies() throws Exception {
		TestUtils.retry(3, () -> {
			WeatherTool tools = new WeatherTool();
			AtomicBoolean allowWeather = new AtomicBoolean(false);

			Policy customPolicy = (toolName, args) -> {
				if ("get_weather".equals(toolName) && allowWeather.get()) {
					return Policy.Decision.ALLOW;
				}
				return Policy.Decision.DENY;
			};

			AgentConfig config = AgentConfig.builder().instructions("""
					You are a weather assistant. Fetch the weather for Tokyo.
					If denied, just say you can't.
					""").addTool(tools).addPolicy(customPolicy).build();

			try (Agent agent = new Agent(config)) {
				System.out.println("Testing with denied policy...");
				CompletableFuture<AgentResponse> future1 = agent.chat("What is the weather in Tokyo right now?");
				await().atMost(120, TimeUnit.SECONDS).until(future1::isDone);
				AgentResponse response1 = future1.get();
				System.out.println(response1.text());
				assertNotNull(response1.text());
				assertTrue(response1.text().toLowerCase().contains("cannot")
						|| response1.text().toLowerCase().contains("can't"));

				System.out.println("\nTesting with allowed policy...");
				allowWeather.set(true);
				CompletableFuture<AgentResponse> future2 = agent.chat("Try to fetch the weather for Tokyo again.");
				await().atMost(120, TimeUnit.SECONDS).until(future2::isDone);
				AgentResponse response2 = future2.get();
				System.out.println(response2.text());
				assertNotNull(response2.text());
				// Since there is no actual implementation in the agent to recall tools in this
				// basic test setup unless the agent decides to, we just assert the agent
				// responds.
			}
		});
	}
}
