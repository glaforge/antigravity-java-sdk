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
import java.util.concurrent.TimeUnit;

import java.util.concurrent.CompletableFuture;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class WebToolsTest {

	@Test
	@Timeout(value = 180, unit = TimeUnit.SECONDS)
	public void testWebTools() throws Exception {
		TestUtils.retry(2, () -> {
			CapabilitiesConfig capabilities = CapabilitiesConfig.builder().enableWebSearch(true).enableUrlReading(true)
					.build();
			AgentConfig config = AgentConfig.builder().modelName("gemini-3.6-flash").capabilities(capabilities).build();

			try (Agent agent = new Agent(config)) {
				CompletableFuture<AgentResponse> future = agent
						.chat("Search the web for the current weather in New York City. Give a short summary.");
				await().atMost(90, TimeUnit.SECONDS).until(future::isDone);
				AgentResponse response = future.get();

				assertNotNull(response);
				assertNotNull(response.text());
				assertTrue(response.text().length() > 0);
			}
		});
	}
}
