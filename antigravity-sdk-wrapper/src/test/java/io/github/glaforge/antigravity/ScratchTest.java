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

public class ScratchTest {

	@Test
	public void testSaveDirContents() throws Exception {
		File tempDir = Files.createTempDirectory("antigravity-scratch").toFile();
		System.out.println("Using temp dir: " + tempDir.getAbsolutePath());

		AgentConfig config1 = AgentConfig.builder().persona("You are a helpful assistant.")
				.saveDir(tempDir.getAbsolutePath()).build();

		String conversationId;
		try (AntigravityAgent agent1 = new AntigravityAgent(config1)) {
			CompletableFuture<AgentResponse> future1 = agent1.chat("My favorite color is blue.");
			await().atMost(120, TimeUnit.SECONDS).until(future1::isDone);
			AgentResponse response1 = future1.get();

			System.out.println("Chat 1:");
			System.out.println(response1.getText());
			assertNotNull(response1.getText());

			conversationId = agent1.getConversationId();
			System.out.println("Conversation ID: " + conversationId);
			assertNotNull(conversationId);
		}

		System.out.println("Wait 2s...");
		Thread.sleep(2000);

		File[] files = tempDir.listFiles();
		System.out.println("Contents of temp dir:");
		assertNotNull(files, "Temp dir should contain files");
		assertTrue(files.length > 0, "There should be at least one file created in the save directory");

		boolean foundStateFile = false;
		for (File f : files) {
			System.out.println(f.getName());
			if (f.getName().equals(conversationId + ".json") || f.getName().contains(conversationId)
					|| f.getName().contains("state")) {
				foundStateFile = true;
			}
		}

		assertTrue(foundStateFile, "Expected some state files or directories to be created for the conversation");
	}
}
