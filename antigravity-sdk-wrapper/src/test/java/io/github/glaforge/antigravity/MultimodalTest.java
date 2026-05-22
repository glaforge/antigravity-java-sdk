package io.github.glaforge.antigravity;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class MultimodalTest {

	@Test
	public void testDocumentInput() throws Exception {
		File tempDoc = File.createTempFile("sample_doc", ".txt");
		tempDoc.deleteOnExit();
		Files.writeString(tempDoc.toPath(), "The secret passcode is XYZ987.");

		AgentConfig config = AgentConfig.builder().persona(
				"You are a helpful assistant that answers questions based on provided documents. I am attaching a text document. You MUST read it and extract the passcode.")
				.build();

		try (AntigravityAgent agent = new AntigravityAgent(config)) {
			System.out.println("Sending multimodal input...");
			CompletableFuture<AgentResponse> future = agent.chat(AgentInput.Text.of(
					"I attached a document. Please read the text inside it. What is the secret passcode? Just reply with the passcode."),
					new AgentInput.Document("text/plain", Files.readAllBytes(tempDoc.toPath()),
							"Attached Document: secret_file.txt"));

			await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
			AgentResponse response = future.get();

			System.out.println("Response: " + response.getText());
			assertNotNull(response.getText());
			assertTrue(response.getText().contains("XYZ987"),
					"Agent should have read the document and found the passcode.");
		}
	}
}
