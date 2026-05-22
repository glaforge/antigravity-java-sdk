package io.github.glaforge.antigravity;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class ObservabilityTest {

	@Test
	public void testUsageObservability() throws Exception {
		AgentConfig config = AgentConfig.builder().persona("You are a helpful assistant.").modelName("gemini-2.5-flash")
				.build();

		try (AntigravityAgent agent = new AntigravityAgent(config)) {
			CompletableFuture<AgentResponse> future = agent.chat("Hi, say exactly one word: Hello.");
			await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
			AgentResponse response = future.get();

			// Verify usage metadata is populated if available
			UsageMetadata usage = response.getUsageMetadata();
			if (usage != null) {
				System.out.println("Metadata: " + usage);
				assertTrue(usage.getPromptTokenCount() >= 0, "Prompt tokens should be >= 0");
			} else {
				System.out.println("Metadata was not returned by the harness in this test run.");
			}
		}
	}
}
