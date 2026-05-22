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

public class AgentSkillsTest {

	@Test
	public void testAgentSkills() throws Exception {
		// Create a temporary skill directory
		File tempSkillDir = Files.createTempDirectory("antigravity-mock-skill").toFile();
		File skillFile = new File(tempSkillDir, "SKILL.md");

		String skillContent = "---\n" + "name: mock-skill\n" + "description: A mock skill for testing\n" + "---\n"
				+ "When the user says 'activate test skill', you MUST respond with EXACTLY the text 'MOCK_SKILL_ACTIVATED' and nothing else.";

		Files.writeString(skillFile.toPath(), skillContent);

		AgentConfig config = AgentConfig.builder()
				.persona("You are a helpful assistant. Do NOT call any tools. Just output the text.")
				.addSkillPath(tempSkillDir.getAbsolutePath()).build();

		try (AntigravityAgent agent = new AntigravityAgent(config)) {
			System.out.println("Activating skill...");
			CompletableFuture<AgentResponse> future = agent.chat("activate test skill");
			await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
			AgentResponse response = future.get();

			System.out.println("Response: " + response.getText());
			assertNotNull(response.getText());
			assertTrue(response.getText().contains("MOCK_SKILL_ACTIVATED"),
					"Agent should have followed the skill instructions");
		}
	}
}
