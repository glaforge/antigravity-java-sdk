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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
public class LiteRTEndToEndLiveTest {

	@Test
	@Timeout(value = 60, unit = TimeUnit.SECONDS)
	public void testLiteRTEndToEndInference() throws Exception {
		Assumptions.assumeTrue(isServerRunning(), "LiteRT local server is not running on http://127.0.0.1:9379");
		String modelPath = System.getProperty("user.home") + "/.litert-lm/models/gemma4-e2b/model.litertlm";

		LiteRTAgentConfig config = LiteRTAgentConfig.builder().modelPath(modelPath).baseUrl("http://127.0.0.1:9379/v1")
				.modelName("gemma4-e2b").lightweight().build();

		try (Agent agent = new Agent(config)) {
			StringBuilder streamedChunks = new StringBuilder();
			AgentResponse response = agent.chatStream("What is the capital of France? Answer in one word.", chunk -> {
				if (chunk.textDelta() != null) {
					streamedChunks.append(chunk.textDelta());
				}
			}).get(45, TimeUnit.SECONDS);

			System.out.println("End-to-End LiteRT Agent Response: " + response.text());
			System.out.println("Streamed Chunks: " + streamedChunks);

			assertNotNull(response);
			assertNotNull(response.text());
			assertTrue(response.text().toLowerCase().contains("paris"),
					"Expected response to contain 'Paris', got: " + response.text());
		}
	}

	private static boolean isServerRunning() {
		try {
			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:9379/v1/models"))
					.timeout(Duration.ofSeconds(2)).GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response.statusCode() == 200;
		} catch (Exception e) {
			return false;
		}
	}
}
