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

import io.github.glaforge.antigravity.localharness.HarnessConfig;
import io.github.glaforge.antigravity.localharness.InputEvent;
import io.github.glaforge.antigravity.localharness.OutputEvent;
import io.github.glaforge.antigravity.localharness.PolicyConfig;
import io.github.glaforge.antigravity.localharness.PolicyDecision;
import io.github.glaforge.antigravity.localharness.PolicyDecisionRequest;
import io.github.glaforge.antigravity.localharness.PolicyDecisionResponse;
import io.github.glaforge.antigravity.localharness.PolicyEvaluationOutcome;
import io.github.glaforge.antigravity.localharness.PolicyRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class PoliciesTest {

	@Test
	public void testAllowAll() {
		Policy policy = Policies.allowAll();
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("any_tool", null));
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("another_tool", null));
	}

	@Test
	public void testDenyAll() {
		Policy policy = Policies.denyAll();
		assertEquals(Policy.Decision.DENY, policy.evaluate("any_tool", null));
		assertEquals(Policy.Decision.DENY, policy.evaluate("another_tool", null));
	}

	@Test
	public void testPassAll() {
		Policy policy = Policies.passAll();
		assertEquals(Policy.Decision.PASS, policy.evaluate("any_tool", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("another_tool", null));
	}

	@Test
	public void testAllowTool() {
		Policy policy = Policies.allowTool("run_command");
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("file_edit", null));
	}

	@Test
	public void testDenyTool() {
		Policy policy = Policies.denyTool("run_command");
		assertEquals(Policy.Decision.DENY, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("file_edit", null));
	}

	@Test
	public void testAllowTools() {
		Policy policy = Policies.allowTools("run_command", "file_edit");
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("file_edit", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("view_file", null));
	}

	@Test
	public void testDenyTools() {
		Policy policy = Policies.denyTools("run_command", "file_edit");
		assertEquals(Policy.Decision.DENY, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.DENY, policy.evaluate("file_edit", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("view_file", null));
	}

	@Test
	public void testAllowIf() {
		Policy policy = Policies.allowIf((tool, args) -> tool.startsWith("run_"));
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("run_script", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("file_edit", null));
	}

	@Test
	public void testDenyIf() {
		Policy policy = Policies.denyIf((tool, args) -> tool.startsWith("run_"));
		assertEquals(Policy.Decision.DENY, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.DENY, policy.evaluate("run_script", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("file_edit", null));
	}

	@Test
	public void testDenyIfWithArguments() throws Exception {
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.node.ObjectNode argsNode = mapper.createObjectNode();
		argsNode.put("command_line", "rm -rf /");

		Policy policy = Policies.denyIf((toolName, args) -> {
			if ("run_command".equals(toolName) && args != null && args.has("command_line")) {
				String cmd = args.get("command_line").asText();
				return cmd.contains("rm -rf");
			}
			return false;
		});

		assertEquals(Policy.Decision.DENY, policy.evaluate("run_command", argsNode));

		argsNode.put("command_line", "echo Hello");
		assertEquals(Policy.Decision.PASS, policy.evaluate("run_command", argsNode));
	}

	@Test
	public void testAskUserWithArguments() throws Exception {
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		com.fasterxml.jackson.databind.node.ObjectNode argsNode = mapper.createObjectNode();
		argsNode.put("path", "config/production.key");

		// Simulate the user saying 'no'
		Policy policy = Policies.askUser((toolName, args) -> {
			if ("view_file".equals(toolName) && args != null && args.has("path")) {
				String path = args.get("path").asText();
				if (path.contains("production.key")) {
					return false; // User denies
				}
			}
			return true; // Auto-allow
		});

		assertEquals(Policy.Decision.DENY, policy.evaluate("view_file", argsNode));

		argsNode.put("path", "config/readme.md");
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("view_file", argsNode));
	}

	@Test
	public void testPolicyChainOrder() throws Exception {
		AgentConfig config = AgentConfig.builder().addPolicy(Policies.allowTools("list_dir"))
				.addPolicy(Policies.denyAll()).build();

		java.lang.reflect.Method evalMethod = Agent.class.getDeclaredMethod("evaluatePolicies", String.class,
				com.fasterxml.jackson.databind.JsonNode.class);
		evalMethod.setAccessible(true);

		try (Agent agent = new Agent(config)) {
			// list_dir is allowed before the denyAll
			Policy.Decision allowed = (Policy.Decision) evalMethod.invoke(agent, "list_dir", null);
			assertEquals(Policy.Decision.ALLOW, allowed);

			// anything else hits the denyAll fallback
			Policy.Decision denied = (Policy.Decision) evalMethod.invoke(agent, "run_command", null);
			assertEquals(Policy.Decision.DENY, denied);
		}
	}

	@Test
	public void testProtobufPolicyConfig() {
		PolicyRule rule = PolicyRule.newBuilder().setRuleId("rule-1").setTool("run_command")
				.setDecision(PolicyDecision.POLICY_DECISION_DENY).setDenyReason("Security restriction").build();

		PolicyConfig protoConfig = PolicyConfig.newBuilder().addRules(rule).build();

		HarnessConfig harnessConfig = HarnessConfig.newBuilder().setCascadeId("test-cascade")
				.setPolicyConfig(protoConfig).build();

		assertTrue(harnessConfig.hasPolicyConfig());
		assertEquals(1, harnessConfig.getPolicyConfig().getRulesCount());

		PolicyRule retrievedRule = harnessConfig.getPolicyConfig().getRules(0);
		assertEquals("rule-1", retrievedRule.getRuleId());
		assertEquals("run_command", retrievedRule.getTool());
		assertEquals(PolicyDecision.POLICY_DECISION_DENY, retrievedRule.getDecision());
		assertEquals("Security restriction", retrievedRule.getDenyReason());
	}

	@Test
	public void testProtobufPolicyDecisionEvent() {
		PolicyDecisionRequest request = PolicyDecisionRequest.newBuilder().setRequestId("req-123").setRuleId("rule-1")
				.build();

		OutputEvent outputEvent = OutputEvent.newBuilder().setPolicyDecisionRequest(request).build();

		assertTrue(outputEvent.hasPolicyDecisionRequest());
		assertEquals("req-123", outputEvent.getPolicyDecisionRequest().getRequestId());

		PolicyDecisionResponse response = PolicyDecisionResponse.newBuilder().setRequestId("req-123")
				.setOutcome(PolicyEvaluationOutcome.POLICY_EVALUATION_OUTCOME_ALLOW).build();

		InputEvent inputEvent = InputEvent.newBuilder().setPolicyDecisionResponse(response).build();

		assertTrue(inputEvent.hasPolicyDecisionResponse());
		assertEquals(PolicyEvaluationOutcome.POLICY_EVALUATION_OUTCOME_ALLOW,
				inputEvent.getPolicyDecisionResponse().getOutcome());
	}
}
