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
		AgentConfig config = AgentConfig.builder().persona(
				"You are an assistant. Wait for me to give you a command. If I sneeze, immediately say 'Bless you'.")
				.build();

		try (AntigravityAgent agent = new AntigravityAgent(config)) {
			// Asynchronously fire a trigger after a brief delay
			Executors.newSingleThreadScheduledExecutor().schedule(() -> {
				System.out.println("Firing background trigger...");
				agent.fireTrigger("The user has just sneezed. Say bless you.");
			}, 500, TimeUnit.MILLISECONDS);

			CompletableFuture<AgentResponse> future = agent.chat("What is the weather in Tokyo right now?");
			await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
			AgentResponse response = future.get();
			System.out.println(response.getText());
			assertNotNull(response.getText());
		}
	}
}
