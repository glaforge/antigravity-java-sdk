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

import io.github.glaforge.antigravity.tools.Tool;
import io.github.glaforge.antigravity.tools.Param;
import io.github.glaforge.antigravity.DynamicTool;
import io.github.glaforge.antigravity.tools.ToolDefinition;
import io.github.glaforge.antigravity.triggers.Triggers;
import io.github.glaforge.antigravity.hooks.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class ReadmeSnippetsTest {

	@Test
	public void snippet1Basic() throws Exception {
		AgentConfig config = AgentConfig.builder().instructions("You are a helpful assistant.")
				.modelName("gemini-2.5-flash").build();

		try (Agent agent = new Agent(config)) {
			CompletableFuture<AgentResponse> future = agent.chat("Hello, who are you?");
			await().atMost(120, TimeUnit.SECONDS).until(future::isDone);
			AgentResponse response = future.get();
			System.out.println(response.text());
		}
	}

	@Test
	public void snippet2Streaming() throws Exception {
		AgentConfig config = AgentConfig.builder().instructions("Write a short story.").modelName("gemini-2.5-flash")
				.build();

		try (Agent agent = new Agent(config)) {
			CompletableFuture<AgentResponse> future = agent.chatStream("Tell me a story about a brave knight.",
					chunk -> {
						System.out.print(chunk.textDelta());
					});
			await().atMost(120, TimeUnit.SECONDS).until(future::isDone);
			future.get();
		}
	}

	@Test
	public void snippet3Publisher() throws Exception {
		AgentConfig config = AgentConfig.builder().modelName("gemini-2.5-flash").build();
		try (Agent agent = new Agent(config)) {
			Flow.Publisher<AgentResponseChunk> publisher = agent.chatPublisher("Tell me a story.");

			CompletableFuture<AgentResponse> done = new CompletableFuture<>();
			publisher.subscribe(new Flow.Subscriber<>() {
				private Flow.Subscription subscription;
				@Override
				public void onSubscribe(Flow.Subscription subscription) {
					this.subscription = subscription;
					subscription.request(Long.MAX_VALUE);
				}
				@Override
				public void onNext(AgentResponseChunk item) {
					System.out.print(item.textDelta());
				}
				@Override
				public void onError(Throwable throwable) {
					throwable.printStackTrace();
					done.completeExceptionally(throwable);
				}
				@Override
				public void onComplete() {
					System.out.println("\nDone!");
					done.complete(null);
				}
			});
			await().atMost(120, TimeUnit.SECONDS).until(done::isDone);
		}
	}

	@Test
	public void snippet4SugaredStream() throws Exception {
		AgentConfig config = AgentConfig.builder().modelName("gemini-2.5-flash").build();
		try (Agent agent = new Agent(config)) {
			AgentStream stream = agent.streamChat("Think step-by-step and say hi.");

			// Just sub natively instead of using Flux for testing
			stream.thoughts().subscribe(new Flow.Subscriber<>() {
				@Override
				public void onSubscribe(Flow.Subscription s) {
					s.request(Long.MAX_VALUE);
				}
				@Override
				public void onNext(String thought) {
					System.out.println("Thinking: " + thought);
				}
				@Override
				public void onError(Throwable t) {
				}
				@Override
				public void onComplete() {
				}
			});

			CompletableFuture<AgentResponse> future = stream.result();
			await().atMost(120, TimeUnit.SECONDS).until(future::isDone);
			AgentResponse response = future.get();
			System.out.println(response.text());
		}
	}

	public static class MyToolbox {
		@Tool(name = "get_weather", description = "Get the weather for a location.")
		public String getWeather(@Param(name = "location", description = "The city and state") String location) {
			return "The weather in " + location + " is sunny.";
		}
	}

	@Test
	public void snippet5AnnotatedTools() throws Exception {
		TestUtils.retry(3, () -> {
			AgentConfig config = AgentConfig.builder().instructions("You can fetch the weather.")
					.modelName("gemini-2.5-flash").addTool(new MyToolbox()).build();
			try (Agent agent = new Agent(config)) {
				CompletableFuture<AgentResponse> f = agent.chat("What's the weather in Seattle?");
				await().atMost(120, TimeUnit.SECONDS).until(f::isDone);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void snippet6DynamicTools() throws Exception {
		AgentConfig config = AgentConfig.builder().instructions("You can fetch the weather.")
				.modelName("gemini-2.5-flash").addTool(new DynamicTool() {
					@Override
					public String getName() {
						return "get_weather";
					}

					record WeatherParams(String location) {
					}

					@Override
					public ToolDefinition getDefinition() {
						return ToolDefinition.builder().name("get_weather")
								.description("Get the weather for a location.").parametersSchema(WeatherParams.class)
								.build();
					}

					@Override
					public Object execute(JsonNode arguments) {
						String location = arguments.get("location").asText();
						return "The weather in " + location + " is sunny.";
					}
				}).build();
		try (Agent agent = new Agent(config)) {
			CompletableFuture<AgentResponse> f = agent.chat("What's the weather in Seattle?");
			await().atMost(120, TimeUnit.SECONDS).until(f::isDone);
		}
	}

	@Test
	public void snippet7PoliciesBasic() throws Exception {
		AgentConfig config = AgentConfig.builder().instructions("You are restricted from running dangerous commands.")
				.addPolicy(Policies.denyAll()).build();
	}

	@Test
	public void snippet8PoliciesAdvanced() throws Exception {
		AgentConfig config = AgentConfig.builder().instructions("You are a secure agent.")
				.addPolicy(Policies.denyIf((toolName, argsNode) -> {
					if ("run_command".equals(toolName) && argsNode.has("command_line")) {
						return argsNode.get("command_line").asText().contains("rm -rf");
					}
					return false;
				})).addPolicy(Policies.allowTools("list_dir", "get_weather")).addPolicy(Policies.denyAll()).build();
	}

	@Test
	public void snippet9Hooks() throws Exception {
		AgentConfig config = AgentConfig.builder().instructions("You are an observed agent.")
				.addPreTurnHook((prompt, context) -> {
					System.out.println("Starting turn with prompt: " + prompt);
					return CompletableFuture.completedFuture(HookResult.allowed());
				}).addOnInteractionHook(request -> {
					InteractionAnswer answer = InteractionAnswer.builder()
							.freeformResponse("My answer to your question is...").build();
					return CompletableFuture.completedFuture(List.of(answer));
				}).build();
	}

	@Test
	public void snippet10BackgroundTriggers() throws Exception {
		AgentConfig config = AgentConfig.builder()
				.instructions("If you are given a deployment status, notify the user.").modelName("gemini-2.5-flash")
				.addTrigger(Triggers.every(1, TimeUnit.SECONDS, ctx -> {
					ctx.fireTrigger("Check the deployment status.");
				})).build();

		try (Agent agent = new Agent(config)) {
			CompletableFuture<AgentResponse> f = agent.chat("Start watching the deployment.");
			await().atMost(120, TimeUnit.SECONDS).until(f::isDone);
		}
	}

	@Test
	public void snippet11MCP() throws Exception {
		McpServerConfig mcpConfig = McpServerConfig.stdio("npx",
				List.of("-y", "@modelcontextprotocol/server-sqlite", "test.db"));

		AgentConfig config = AgentConfig.builder().addMcpServer(mcpConfig).build();
	}

	public record Person(String name) {
	}

	@Test
	public void snippet13StructuredOutputs() throws Exception {
		AgentConfig config = AgentConfig.builder()
				.instructions(
						"Extract the person's name and return it in the provided schema. Do not output anything else.")
				.modelName("gemini-2.5-flash").finishToolSchema(Person.class).build();

		try (Agent agent = new Agent(config)) {
			CompletableFuture<AgentResponse> f = agent.chat("Extract: Alice");
			await().atMost(120, TimeUnit.SECONDS).until(f::isDone);
			AgentResponse response = f.get();

			Person parsedPerson = response.getStructuredOutput(Person.class);
			System.out.println(parsedPerson.name());
		}
	}

	@Test
	public void snippet14Cancellation() throws Exception {
		AgentConfig config = AgentConfig.builder().modelName("gemini-2.5-flash").build();

		try (Agent agent = new Agent(config)) {
			// Start a long-running request
			CompletableFuture<AgentResponse> future = agent.chat("Write a very long story.");

			// Cancel the agent immediately from another thread
			agent.cancel();

			try {
				future.get(120, TimeUnit.SECONDS);
			} catch (java.util.concurrent.ExecutionException e) {
				if (e.getCause() instanceof AgentCancelledException) {
					System.out.println("Agent was cancelled successfully!");
				}
			}
		}
	}

	@Test
	public void snippet15SlashCommands() throws Exception {
		AgentConfig config = AgentConfig.builder().modelName("gemini-2.5-flash").build();

		try (Agent agent = new Agent(config)) {
			// You can send slash commands directly!
			CompletableFuture<AgentResponse> f = agent.chat("/help");
			await().atMost(120, TimeUnit.SECONDS).until(f::isDone);
			AgentResponse response = f.get();
			System.out.println(response.text());
		}
	}

	@Test
	public void snippet16BuiltinTools() throws Exception {
		CapabilitiesConfig capabilities = CapabilitiesConfig.builder().enableWebSearch(true).enableShell(true)
				.enableWriteFile(true).enableFileEdit(true).enableListDir(true).enableGrepSearch(true).build();

		AgentConfig config = AgentConfig.builder()
				.instructions("Search the web for the latest news and save it to a file.").modelName("gemini-2.5-flash")
				.capabilities(capabilities).build();

		try (Agent agent = new Agent(config)) {
			// The agent now has access to web search and file operations natively!
		}
	}
}
