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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Tag("integration")
public class SlashCommandsTest {

	@Test
	@Timeout(value = 60, unit = TimeUnit.SECONDS)
	public void testSlashCommand() throws Exception {
		Path tempDir = Files.createTempDirectory("antigravity-slash-test");
		AgentConfig config = AgentConfig.builder().appDataDir(tempDir.toString())
				.capabilities(CapabilitiesConfig.builder().enableWriteFile(true).enableViewFile(true).build())
				.addPolicy(new Policy() {
					@Override
					public Decision evaluate(String toolName, com.fasterxml.jackson.databind.JsonNode arguments) {
						return Decision.ALLOW;
					}
				}).build();

		try (Agent agent = new Agent(config)) {
			// Using the /plan slash command programmatically
			AgentInput slashCommand = AgentInput.SlashCommand.of("plan");
			AgentInput textPrompt = AgentInput.Text.of("Write a python script that prints numbers 1 to 10.");

			AgentResponse response = agent.chat(List.of(slashCommand, textPrompt)).get(45, TimeUnit.SECONDS);
			assertNotNull(response);
			assertNotNull(response.text());

			// The planning agent should ask us a question about the plan or present the
			// plan.
			// Since we just execute and wait, it'll run to completion.
		}
	}
}
