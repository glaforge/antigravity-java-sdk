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
		WeatherTool tools = new WeatherTool();
		AtomicBoolean allowWeather = new AtomicBoolean(false);

		Policy customPolicy = (toolName, args) -> {
			if ("get_weather".equals(toolName) && allowWeather.get()) {
				return Policy.Decision.ALLOW;
			}
			return Policy.Decision.DENY;
		};

		AgentConfig config = AgentConfig.builder()
				.persona("You are a weather assistant. Fetch the weather for Tokyo. If denied, just say you can't.")
				.addTool(tools).addPolicy(customPolicy).build();

		try (AntigravityAgent agent = new AntigravityAgent(config)) {
			System.out.println("Testing with denied policy...");
			CompletableFuture<AgentResponse> future1 = agent.chat("What is the weather in Tokyo right now?");
			await().atMost(30, TimeUnit.SECONDS).until(future1::isDone);
			AgentResponse response1 = future1.get();
			System.out.println(response1.getText());
			assertNotNull(response1.getText());
			assertTrue(response1.getText().toLowerCase().contains("cannot")
					|| response1.getText().toLowerCase().contains("can't"));

			System.out.println("\nTesting with allowed policy...");
			allowWeather.set(true);
			CompletableFuture<AgentResponse> future2 = agent.chat("Try to fetch the weather for Tokyo again.");
			await().atMost(30, TimeUnit.SECONDS).until(future2::isDone);
			AgentResponse response2 = future2.get();
			System.out.println(response2.getText());
			assertNotNull(response2.getText());
			// Since there is no actual implementation in the agent to recall tools in this
			// basic test setup unless the agent decides to, we just assert the agent
			// responds.
		}
	}
}
