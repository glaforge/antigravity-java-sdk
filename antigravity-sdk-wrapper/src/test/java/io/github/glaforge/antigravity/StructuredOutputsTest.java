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

		try (AntigravityAgent agent = new AntigravityAgent(config)) {
			System.out.println("Sending prompt...");
			CompletableFuture<AgentResponse> future = agent.chat("Bob is 42 years old and likes to fish.");
			await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
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
	}
}
