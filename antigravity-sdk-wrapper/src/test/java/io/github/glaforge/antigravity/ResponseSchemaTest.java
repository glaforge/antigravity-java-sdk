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
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
public class ResponseSchemaTest {

	public static class Review {
		public String product;
		public int rating;
		public String comment;
	}

	@Test
	public void testAgentConfigResponseSchemaClass() {
		AgentConfig config = AgentConfig.builder().instructions("Analyze reviews").responseSchema(Review.class).build();

		assertNotNull(config.getResponseSchema());
		assertNotNull(config.getFinishToolSchemaJson());
		assertEquals(config.getResponseSchema(), config.getFinishToolSchemaJson());
		assertTrue(config.getResponseSchema().contains("product"));
		assertTrue(config.getResponseSchema().contains("rating"));
		assertTrue(config.getResponseSchema().contains("comment"));
	}

	@Test
	public void testAgentConfigResponseSchemaJsonString() {
		String jsonSchema = "{\"type\": \"object\", \"properties\": {\"rating\": {\"type\": \"integer\"}}}";
		AgentConfig config = AgentConfig.builder().instructions("Analyze reviews").responseSchema(jsonSchema).build();

		assertEquals(jsonSchema, config.getResponseSchema());
		assertEquals(jsonSchema, config.getFinishToolSchemaJson());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testDeprecatedFinishToolSchemaBackwardCompatibility() {
		AgentConfig config = AgentConfig.builder().instructions("Analyze reviews").finishToolSchema(Review.class)
				.build();

		assertNotNull(config.getResponseSchema());
		assertEquals(config.getResponseSchema(), config.getFinishToolSchemaJson());
		assertTrue(config.getResponseSchema().contains("product"));

		AgentConfig configJson = AgentConfig.builder().instructions("Analyze reviews")
				.finishToolSchemaJson("{\"type\": \"object\"}").build();

		assertEquals("{\"type\": \"object\"}", configJson.getResponseSchema());
		assertEquals("{\"type\": \"object\"}", configJson.getFinishToolSchemaJson());
	}

	@Test
	public void testAgentBuilderResponseSchema() throws Exception {
		// Verify Agent.Builder config delegation for responseSchema
		Agent.Builder builder = Agent.builder().instructions("Analyze reviews").responseSchema(Review.class);

		// Reflection or package-private check on internal configBuilder
		Field field = Agent.Builder.class.getDeclaredField("configBuilder");
		field.setAccessible(true);
		AgentConfig.Builder configBuilder = (AgentConfig.Builder) field.get(builder);
		AgentConfig config = configBuilder.build();

		assertNotNull(config.getResponseSchema());
		assertTrue(config.getResponseSchema().contains("product"));

		Agent.Builder stringBuilder = Agent.builder().instructions("Analyze reviews")
				.responseSchema("{\"type\": \"object\"}");
		AgentConfig.Builder stringConfigBuilder = (AgentConfig.Builder) field.get(stringBuilder);
		AgentConfig stringConfig = stringConfigBuilder.build();

		assertEquals("{\"type\": \"object\"}", stringConfig.getResponseSchema());
	}
}
