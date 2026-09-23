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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class UsageMetadataParsingTest {

	private final JsonMapper mapper = JsonMapper.builder().build();

	@Test
	public void testParseCamelCaseUsageMetadata() throws Exception {
		String json = """
				{
				    "promptTokenCount": "1480",
				    "cachedContentTokenCount": "100",
				    "candidatesTokenCount": "25",
				    "thoughtsTokenCount": "30",
				    "totalTokenCount": "1635",
				    "serviceTier": "priority",
				    "promptTokensDetails": [
				        {"modality": "TEXT", "tokenCount": "1480"}
				    ]
				}
				""";
		JsonNode node = mapper.readTree(json);
		UsageMetadata usage = Agent.parseUsageMetadata(node);

		assertNotNull(usage);
		assertEquals(1480, usage.promptTokenCount());
		assertEquals(100, usage.cachedContentTokenCount());
		assertEquals(25, usage.candidatesTokenCount());
		assertEquals(30, usage.thoughtsTokenCount());
		assertEquals(1635, usage.totalTokenCount());
		assertEquals("priority", usage.serviceTier());
		assertEquals(1, usage.promptTokensDetails().size());
		assertEquals(Modality.TEXT, usage.promptTokensDetails().get(0).modality());
		assertEquals(1480, usage.promptTokensDetails().get(0).tokenCount());
	}

	@Test
	public void testParseSnakeCaseUsageMetadata() throws Exception {
		String json = """
				{
				    "prompt_token_count": 500,
				    "cached_content_token_count": 50,
				    "candidates_token_count": 100,
				    "thoughts_token_count": 40,
				    "total_token_count": 690,
				    "service_tier": "standard",
				    "prompt_tokens_details": [
				        {"modality": "TEXT", "token_count": 500}
				    ]
				}
				""";
		JsonNode node = mapper.readTree(json);
		UsageMetadata usage = Agent.parseUsageMetadata(node);

		assertNotNull(usage);
		assertEquals(500, usage.promptTokenCount());
		assertEquals(50, usage.cachedContentTokenCount());
		assertEquals(100, usage.candidatesTokenCount());
		assertEquals(40, usage.thoughtsTokenCount());
		assertEquals(690, usage.totalTokenCount());
		assertEquals("standard", usage.serviceTier());
		assertEquals(1, usage.promptTokensDetails().size());
		assertEquals(Modality.TEXT, usage.promptTokensDetails().get(0).modality());
		assertEquals(500, usage.promptTokensDetails().get(0).tokenCount());
	}

	@Test
	public void testAgentResponseUsageAlias() {
		UsageMetadata usage = new UsageMetadata(100, 0, 50, 10, 160);
		AgentResponse response = new AgentResponse("Hello", "Thinking...", usage);

		assertEquals(usage, response.usageMetadata());
		assertEquals(usage, response.usage());
	}

	@Test
	public void testUsageMetadataArithmeticWithZeroIdentity() {
		UsageMetadata initial = new UsageMetadata(0, 0, 0, 0, 0);
		UsageMetadata turn1 = new UsageMetadata(100, 10, 50, 20, 180, "priority",
				List.of(new ModalityTokenCount(Modality.TEXT, 100)), List.of(), List.of(), List.of());

		// Subtraction against initial zero usage must preserve details
		UsageMetadata diff = turn1.subtract(initial);
		assertSame(turn1, diff);

		// Addition with zero usage must preserve details
		UsageMetadata added = initial.add(turn1);
		assertSame(turn1, added);
	}
}
