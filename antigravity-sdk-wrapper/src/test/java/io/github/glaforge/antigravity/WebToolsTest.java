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

import static org.junit.jupiter.api.Assertions.*;

public class WebToolsTest {

	@Test
	public void testWebTools() throws Exception {
		// Enable web search and url reading
		CapabilitiesConfig capabilities = CapabilitiesConfig.builder().enableWebSearch(true).enableUrlReading(true)
				.build();
		AgentConfig config = AgentConfig.builder().capabilities(capabilities).build();

		try (Agent agent = new Agent(config)) {
			// Ask a question that requires searching the web
			AgentResponse response = agent
					.chat("Search the web for the current weather in New York City. Give a short summary.").join();

			assertNotNull(response);
			assertNotNull(response.text());
			assertTrue(response.text().length() > 0);
		}
	}
}
