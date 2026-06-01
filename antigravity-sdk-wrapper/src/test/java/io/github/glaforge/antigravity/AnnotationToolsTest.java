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

import io.github.glaforge.antigravity.tools.Tool;
import io.github.glaforge.antigravity.tools.Param;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnnotationToolsTest {

	public record Location(String city, String country, int zipCode) {
	}
	public record Weather(String conditions, double temperature) {
	}

	public static class MyToolbox {
		@Tool(name = "weather_forecast", description = "Gives the weather forecast for a specific location")
		public Weather forecast(
				@Param(name = "location", description = "the location for the weather forecast") Location location) {
			System.out.println("Executing forecast for: " + location);
			return new Weather("Sunny", 25.5);
		}
	}

	@Test
	public void testPojoToolInvocation() throws Exception {
		TestUtils.retry(3, () -> {
			AgentConfig config = AgentConfig.builder().instructions("""
					You are a weather bot.
					Always invoke the weather_forecast tool to get the weather, and tell the user the result.
					""").addTool(new MyToolbox()).modelName("gemini-2.5-flash").build();

			try (Agent agent = new Agent(config)) {
				System.out.println("Agent initialized successfully!");

				System.out.println("Sending prompt...");
				AgentResponse response = agent.getConversation()
						.chat("What is the weather in Paris, France, zip 75001?").join();
				System.out.println("\n--- Agent Response ---");
				System.out.println(response.getText());
				System.out.println("----------------------\n");

				assertTrue(response.getText().contains("Sunny"));
				assertTrue(response.getText().contains("25.5"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}
}
