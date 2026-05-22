package io.github.glaforge.antigravity;

import io.github.glaforge.antigravity.tools.AntigravityTool;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class HelloWorldTest {

	public static class WeatherTools {
		@AntigravityTool(name = "get_weather", description = "Get the current weather for a location")
		public Map<String, Object> getWeather(String location) {
			System.out.println("Executing getWeather for: " + location);
			return Map.of("location", location, "temperature", 22.5, "conditions", "Sunny", "unit", "Celsius");
		}
	}

	@Test
	public void testWeatherAgent() throws Exception {
		WeatherTools tools = new WeatherTools();
		AgentConfig config = AgentConfig.builder().persona(
				"You are a helpful weather assistant. You MUST use the get_weather tool to fetch weather and NEVER use bash commands.")
				.modelName("gemini-2.5-flash").addTool(tools).build();

		try (AntigravityAgent agent = new AntigravityAgent(config)) {
			System.out.println("Agent initialized successfully!");

			System.out.println("Sending prompt...");
			CompletableFuture<AgentResponse> future = agent.chat("What is the weather in Tokyo right now?");

			await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
			AgentResponse response = future.get();

			System.out.println("\n--- Agent Response ---");
			System.out.println(response.getText());
			System.out.println("----------------------\n");

			assertNotNull(response);
			assertNotNull(response.getText());
			assertTrue(response.getText().contains("22.5"), "Response should contain the temperature");
			assertTrue(response.getText().contains("Celsius") || response.getText().contains("Sunny"),
					"Response should contain weather conditions");
		}
	}
}
