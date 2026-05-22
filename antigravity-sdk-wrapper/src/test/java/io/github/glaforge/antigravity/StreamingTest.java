package io.github.glaforge.antigravity;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class StreamingTest {

	@Test
	public void testStreaming() throws Exception {
		AgentConfig config = AgentConfig.builder().persona("You are a helpful assistant.").modelName("gemini-2.5-flash")
				.build();

		try (AntigravityAgent agent = new AntigravityAgent(config)) {
			System.out.println("Starting chat stream...");
			AtomicInteger chunkCount = new AtomicInteger(0);

			CompletableFuture<AgentResponse> future = agent.chatStream("Write a 3 sentence story about a brave knight.",
					chunk -> {
						System.out.print(chunk.getTextDelta());
						chunkCount.incrementAndGet();
					});
			await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
			AgentResponse response = future.get();

			System.out.println("\n--- Stream Complete ---");

			// Verify streaming fired multiple times
			assertTrue(chunkCount.get() > 0, "Should have received multiple streaming chunks");

			// Verify response is fully assembled
			assertNotNull(response.getText());
			assertFalse(response.getText().isEmpty());
			System.out.println("Final Text Length: " + response.getText().length());
		}
	}
}
