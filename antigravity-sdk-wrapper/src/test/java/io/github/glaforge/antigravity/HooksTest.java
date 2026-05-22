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

import io.github.glaforge.antigravity.hooks.*;
import io.github.glaforge.antigravity.tools.AntigravityTool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HooksTest {

	public static class EchoTool {
		@AntigravityTool(description = "Echoes the input string")
		public String echo(String message) {
			return "{\"result\": \"ECHO: " + message + "\"}";
		}
	}

	@Test
	public void testLifecycleHooksOrder() throws Exception {
		int maxRetries = 3;
		for (int i = 0; i < maxRetries; i++) {
			try {
				List<String> events = new ArrayList<>();

				OnSessionStartHook startHook = () -> {
					events.add("start");
					return CompletableFuture.completedFuture(null);
				};
				OnSessionEndHook endHook = () -> {
					events.add("end");
					return CompletableFuture.completedFuture(null);
				};
				PreTurnHook preTurnHook = (prompt) -> {
					events.add("pre_turn:" + prompt);
					return CompletableFuture.completedFuture(HookResult.allowed());
				};
				PostTurnHook postTurnHook = (resp) -> {
					events.add("post_turn");
					return CompletableFuture.completedFuture(null);
				};
				PreToolCallDecideHook preToolHook = (call) -> {
					events.add("pre_tool:" + call.name());
					return CompletableFuture.completedFuture(HookResult.allowed());
				};
				PostToolCallHook postToolHook = (call, result) -> {
					events.add("post_tool:" + result);
					return CompletableFuture.completedFuture(null);
				};

				AgentConfig config = AgentConfig.builder().modelName("models/gemini-2.5-flash").persona(
						"You are a helpful assistant. If the user asks you to echo, you must call the echo tool EXACTLY ONCE. After the tool returns, immediately reply to the user with the result and finish the turn.")
						.addTool(new EchoTool()).addHook(startHook).addHook(endHook).addHook(preTurnHook).addHook(postTurnHook)
						.addHook(preToolHook).addHook(postToolHook).build();

				try (AntigravityAgent agent = new AntigravityAgent(config)) {
					AgentResponse response = agent.chat("Say hello").join();
					assertNotNull(response.getText());
				}

				System.out.println("Events: " + events);

				assertEquals("start", events.get(0));
				assertEquals("pre_turn:Say hello", events.get(1));
				assertEquals("pre_tool:echo", events.get(2));
				assertTrue(events.get(3).toLowerCase().startsWith("post_tool:{\"result\": \"echo: hello"));
				assertEquals("post_turn", events.get(4));
				assertEquals("end", events.get(5));
				break;
			} catch (Throwable e) {
				if (i == maxRetries - 1) {
					if (e instanceof Exception) throw (Exception) e;
					if (e instanceof Error) throw (Error) e;
					throw new RuntimeException(e);
				}
				System.err.println("Test failed on attempt " + (i + 1) + " due to: " + e.getMessage() + ". Retrying...");
			}
		}
	}
}
