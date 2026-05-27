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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class StructuredOutputsTest {

	public static class Person {
		public String name;
		public int age;

		@Override
		public String toString() {
			return "Person{name='" + name + "', age=" + age + "}";
		}
	}

	@Test
	public void testStructuredOutput() throws Exception {
		TestUtils.retry(3, () -> {
			String schema = """
					{
					  "type": "object",
					  "properties": {
					    "name": {
					      "type": "string"
					    },
					    "age": {
					      "type": "integer"
					    }
					  },
					  "required": ["name", "age"]
					}
					""";

			AgentConfig config = AgentConfig.builder().persona("Extract the person information from the text.")
					.modelName("gemini-2.5-flash").finishToolSchemaJson(schema).build();

			try (Agent agent = new Agent(config)) {
				System.out.println("Sending prompt...");
				CompletableFuture<AgentResponse> future = agent.getConversation()
						.chat("Bob is 42 years old and likes to fish.");
				await().atMost(120, TimeUnit.SECONDS).until(future::isDone);
				AgentResponse response = future.get();

				System.out.println("\n--- Agent Response ---");
				System.out.println(response.getText());
				System.out.println("----------------------\n");

				Person person = response.getStructuredOutput(Person.class);
				assertNotNull(person, "Should have parsed a Person object");
				assertEquals("Bob", person.name);
				assertEquals(42, person.age);
				System.out.println("Successfully parsed: " + person);
			}
		});
	}
}
