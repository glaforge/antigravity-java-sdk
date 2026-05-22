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
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class PersistenceTest {

	@Test
	public void testPersistence() throws Exception {
		int maxRetries = 3;
		for (int i = 0; i < maxRetries; i++) {
			try {
				File tempDir = Files.createTempDirectory("antigravity-save-dir").toFile();

				String conversationId;

				// Session 1: Establish context
				AgentConfig config1 = AgentConfig.builder().persona("You are a helpful assistant.")
						.saveDir(tempDir.getAbsolutePath()).build();

				try (AntigravityAgent agent1 = new AntigravityAgent(config1)) {
					CompletableFuture<AgentResponse> future1 = agent1.chat("Please remember the secret code word is BANANA.");
					await().atMost(120, TimeUnit.SECONDS).until(future1::isDone);
					AgentResponse response1 = future1.get();

					System.out.println("Session 1 response: " + response1.getText());
					assertNotNull(response1.getText());

					conversationId = agent1.getConversationId();
					System.out.println("Session 1 ended with Conversation ID: " + conversationId);
				}

				assertNotNull(conversationId);
				assertFalse(conversationId.isEmpty());

				// Give localharness a tiny bit of time to flush to disk?
				Thread.sleep(1000);

				// Session 2: Retrieve context
				AgentConfig config2 = AgentConfig.builder().persona("You are a helpful assistant.")
						.saveDir(tempDir.getAbsolutePath()).conversationId(conversationId).build();

				try (AntigravityAgent agent2 = new AntigravityAgent(config2)) {
					CompletableFuture<AgentResponse> future2 = agent2.chat("What is the secret code word?");
					await().atMost(120, TimeUnit.SECONDS).until(future2::isDone);
					AgentResponse response2 = future2.get();

					System.out.println("Session 2 response: " + response2.getText());
					assertNotNull(response2.getText());
					assertTrue(response2.getText().toLowerCase().contains("banana"));
				}
				break;
			} catch (Throwable e) {
				if (i == maxRetries - 1) {
					throw e;
				}
				System.err.println("Test failed on attempt " + (i + 1) + " due to: " + e.getMessage() + ". Retrying...");
			}
		}
	}
}
