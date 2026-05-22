package io.github.glaforge.antigravity;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class SubagentsTest {

	@Test
	public void testSubagents() throws Exception {
		AgentConfig config = AgentConfig.builder().persona("You are a coordinator agent.").enableSubagents(true)
				.build();

		try (AntigravityAgent agent = new AntigravityAgent(config)) {
			CompletableFuture<AgentResponse> future = agent
					.chat("Please spawn a subagent to write a 2 sentence poem about space.");
			await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
			AgentResponse response = future.get();

			System.out.println(response.getText());
			assertNotNull(response.getText());
			assertTrue(response.getText().toLowerCase().contains("subagent")
					|| response.getText().toLowerCase().contains("spawned"));
		}
	}
}
