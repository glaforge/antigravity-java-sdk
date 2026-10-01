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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
public class FeatureParity0120Test {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void testSessionContinuationModeValidation() {
		// Valid RESUME mode with conversationId
		AgentConfig validConfig = AgentConfig.builder().conversationId("session-xyz")
				.sessionContinuationMode(SessionContinuationMode.RESUME).build();
		assertEquals(SessionContinuationMode.RESUME, validConfig.getSessionContinuationMode());
		assertEquals("session-xyz", validConfig.getConversationId());

		// Invalid RESUME mode without conversationId
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			AgentConfig.builder().sessionContinuationMode(SessionContinuationMode.RESUME).build();
		});
		assertTrue(thrown.getMessage()
				.contains("conversationId must be specified when sessionContinuationMode is RESUME"));

		// CREATE_OR_RESUME and CREATE_ONLY without conversationId are valid
		AgentConfig createOrResume = AgentConfig.builder()
				.sessionContinuationMode(SessionContinuationMode.CREATE_OR_RESUME).build();
		assertEquals(SessionContinuationMode.CREATE_OR_RESUME, createOrResume.getSessionContinuationMode());
	}

	@Test
	public void testLocalOpenAIAgentConfigParity() {
		BudgetConfig budget = BudgetConfig.builder().maxModelCalls(10).maxTotalTokens(50000).build();

		LocalOpenAIAgentConfig config = LocalOpenAIAgentConfig.builder().baseUrl("http://localhost:11434/v1")
				.modelName("llama3.2").conversationId("sess-openai-1")
				.sessionContinuationMode(SessionContinuationMode.CREATE_OR_RESUME).budgetConfig(budget)
				.addPolicy(Policies.allowTool("run_command")).build();

		assertEquals("http://localhost:11434/v1", config.getBaseUrl());
		assertEquals("llama3.2", config.getModelName());
		assertEquals(SessionContinuationMode.CREATE_OR_RESUME, config.getSessionContinuationMode());
		assertNotNull(config.getBudgetConfig());
		assertEquals(10, config.getBudgetConfig().maxModelCalls());
		assertEquals(50000, config.getBudgetConfig().maxTotalTokens());
		assertEquals(1, config.getPolicies().size());

		// Verify underlying AgentConfig
		AgentConfig underlying = config.getAgentConfig();
		assertEquals("sess-openai-1", underlying.getConversationId());
		assertEquals(SessionContinuationMode.CREATE_OR_RESUME, underlying.getSessionContinuationMode());
		assertEquals(budget, underlying.getBudgetConfig());
	}

	@Test
	public void testLiteRTAgentConfigParity() {
		BudgetConfig budget = BudgetConfig.builder().maxModelCalls(5).build();

		LiteRTAgentConfig config = LiteRTAgentConfig.builder().modelPath("/models/gemma-2b-it.bin")
				.conversationId("sess-litert-1").sessionContinuationMode(SessionContinuationMode.RESUME)
				.budgetConfig(budget).addPolicy(Policies.allowAll()).build();

		assertEquals("/models/gemma-2b-it.bin", config.getModelPath());
		assertEquals(SessionContinuationMode.RESUME, config.getSessionContinuationMode());
		assertNotNull(config.getBudgetConfig());
		assertEquals(5, config.getBudgetConfig().maxModelCalls());
		assertEquals(1, config.getPolicies().size());

		// Verify underlying AgentConfig
		AgentConfig underlying = config.getAgentConfig();
		assertEquals("sess-litert-1", underlying.getConversationId());
		assertEquals(SessionContinuationMode.RESUME, underlying.getSessionContinuationMode());
		assertEquals(budget, underlying.getBudgetConfig());
	}

	@Test
	public void testAuthorizationCallbackJustificationForwarding() {
		AtomicReference<String> capturedJustification = new AtomicReference<>();
		AtomicReference<String> capturedTool = new AtomicReference<>();

		Policies.UserConfirmationCallback callback = (tool, args, justification) -> {
			capturedTool.set(tool);
			capturedJustification.set(justification);
			return "proceed".equals(args.path("action").asText());
		};

		String reason = "Modifying production database requires approval";
		Policy policy = Policies.askUser(callback, reason);
		assertEquals(reason, policy.reason());

		ObjectNode args = objectMapper.createObjectNode();
		args.put("action", "proceed");

		Policy.Decision decision = policy.evaluate("sql_execute", args);
		assertEquals(Policy.Decision.ALLOW, decision);
		assertEquals("sql_execute", capturedTool.get());
		assertEquals(reason, capturedJustification.get());

		args.put("action", "abort");
		decision = policy.evaluate("sql_execute", args);
		assertEquals(Policy.Decision.DENY, decision);
	}

	@Test
	public void testConfirmRunCommandWithJustification() {
		AtomicReference<String> capturedJustification = new AtomicReference<>();

		Policy policy = Policies.confirmRunCommand((tool, args, justification) -> {
			capturedJustification.set(justification);
			return true;
		}, "Security policy requires approval for shell commands");

		ObjectNode args = objectMapper.createObjectNode();
		args.put("command", "ls -la");

		assertEquals(Policy.Decision.ALLOW, policy.evaluate("run_command", args));
		assertEquals("Security policy requires approval for shell commands", capturedJustification.get());

		// Pass for other tools
		assertEquals(Policy.Decision.PASS, policy.evaluate("view_file", args));
	}

	@Test
	public void testToolsetPruningDeprecatedTools() {
		List<BuiltinTools> deprecated = BuiltinTools.deprecated();
		assertEquals(3, deprecated.size());
		assertTrue(deprecated.contains(BuiltinTools.LIST_DIR));
		assertTrue(deprecated.contains(BuiltinTools.SEARCH_DIR));
		assertTrue(deprecated.contains(BuiltinTools.FIND_FILE));

		// Minimal tools must only contain 4 tools
		List<BuiltinTools> minimal = BuiltinTools.minimal();
		assertEquals(4, minimal.size());
		assertTrue(minimal.contains(BuiltinTools.RUN_COMMAND));
		assertTrue(minimal.contains(BuiltinTools.VIEW_FILE));
		assertTrue(minimal.contains(BuiltinTools.CREATE_FILE));
		assertTrue(minimal.contains(BuiltinTools.EDIT_FILE));
		assertFalse(minimal.contains(BuiltinTools.LIST_DIR));
		assertFalse(minimal.contains(BuiltinTools.SEARCH_DIR));
		assertFalse(minimal.contains(BuiltinTools.FIND_FILE));

		// Read-only tools must exclude deprecated tools
		List<BuiltinTools> readOnly = BuiltinTools.readOnly();
		for (BuiltinTools dep : deprecated) {
			assertFalse(readOnly.contains(dep), "readOnly must not contain deprecated tool: " + dep);
		}

		// Nondestructive tools must exclude deprecated tools
		List<BuiltinTools> nondestructive = BuiltinTools.nondestructive();
		for (BuiltinTools dep : deprecated) {
			assertFalse(nondestructive.contains(dep), "nondestructive must not contain deprecated tool: " + dep);
		}

		// Default tools must exclude ASK_QUESTION and all deprecated tools
		List<BuiltinTools> defaults = BuiltinTools.defaultTools();
		assertFalse(defaults.contains(BuiltinTools.ASK_QUESTION));
		for (BuiltinTools dep : deprecated) {
			assertFalse(defaults.contains(dep), "defaultTools must not contain deprecated tool: " + dep);
		}

		// All tools must contain all values including deprecated
		List<BuiltinTools> all = BuiltinTools.allTools();
		for (BuiltinTools dep : deprecated) {
			assertTrue(all.contains(dep), "allTools must contain deprecated tool: " + dep);
		}
	}
}
