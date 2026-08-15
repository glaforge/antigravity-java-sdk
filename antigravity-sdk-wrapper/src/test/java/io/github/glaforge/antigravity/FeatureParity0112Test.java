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
import java.util.List;
import io.github.glaforge.antigravity.localharness.TrajectoryStateUpdate;
import static io.github.glaforge.antigravity.localharness.TrajectoryStateUpdate.StopReason.*;
import static io.github.glaforge.antigravity.localharness.AgentBehavior.AGENT_BEHAVIOR_AUTONOMOUS;
import static io.github.glaforge.antigravity.localharness.AgentBehavior.AGENT_BEHAVIOR_INTERACTIVE;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity0112Test {

	@Test
	public void testBudgetConfigBuilder() {
		BudgetConfig budget = BudgetConfig.builder().maxModelCalls(5).maxToolCalls(10).maxInputTokens(5000L)
				.maxOutputTokens(2000L).maxTotalTokens(10000L).build();

		assertEquals(5, budget.maxModelCalls());
		assertEquals(10, budget.maxToolCalls());
		assertEquals(5000L, budget.maxInputTokens());
		assertEquals(2000L, budget.maxOutputTokens());
		assertEquals(10000L, budget.maxTotalTokens());
	}

	@Test
	public void testAgentBehaviorEnum() {
		assertEquals(AgentBehavior.AUTONOMOUS, AgentBehavior.fromString("AUTONOMOUS"));
		assertEquals(AgentBehavior.INTERACTIVE, AgentBehavior.fromString("interactive"));
		assertNull(AgentBehavior.fromString(null));
		assertNull(AgentBehavior.fromString("unknown"));
	}

	@Test
	public void testStopReasonEnum() {
		assertEquals(StopReason.MAX_MODEL_CALLS_EXCEEDED,
				StopReason.fromProtobuf(STOP_REASON_MAX_MODEL_CALLS_EXCEEDED));
		assertEquals(StopReason.MAX_TOOL_CALLS_EXCEEDED, StopReason.fromProtobuf(STOP_REASON_MAX_TOOL_CALLS_EXCEEDED));
		assertEquals(StopReason.MAX_INPUT_TOKENS_EXCEEDED,
				StopReason.fromProtobuf(STOP_REASON_MAX_INPUT_TOKENS_EXCEEDED));
		assertEquals(StopReason.MAX_OUTPUT_TOKENS_EXCEEDED,
				StopReason.fromProtobuf(STOP_REASON_MAX_OUTPUT_TOKENS_EXCEEDED));
		assertEquals(StopReason.MAX_TOTAL_TOKENS_EXCEEDED,
				StopReason.fromProtobuf(STOP_REASON_MAX_TOTAL_TOKENS_EXCEEDED));
		assertEquals(StopReason.QUOTA_EXHAUSTED, StopReason.fromProtobuf(STOP_REASON_QUOTA_EXHAUSTED));
		assertEquals(StopReason.UNSPECIFIED, StopReason.fromProtobuf(STOP_REASON_UNSPECIFIED));
	}

	@Test
	public void testServiceTierEnum() {
		assertEquals("priority", ServiceTier.PRIORITY.getValue());
		assertEquals("standard", ServiceTier.STANDARD.getValue());
		assertEquals("flex", ServiceTier.FLEX.getValue());

		assertEquals(ServiceTier.PRIORITY, ServiceTier.fromString("priority"));
		assertEquals(ServiceTier.PRIORITY, ServiceTier.fromString("PRIORITY"));
		assertEquals(ServiceTier.STANDARD, ServiceTier.fromString("standard"));
		assertEquals(ServiceTier.FLEX, ServiceTier.fromString("flex"));
		assertNull(ServiceTier.fromString(null));
		assertNull(ServiceTier.fromString("invalid_tier"));
	}

	@Test
	public void testModalityEnumAndModalityTokenCount() {
		assertEquals(Modality.TEXT, Modality.fromString("text"));
		assertEquals(Modality.IMAGE, Modality.fromString("image"));
		assertEquals(Modality.VIDEO, Modality.fromString("video"));
		assertEquals(Modality.AUDIO, Modality.fromString("audio"));
		assertEquals(Modality.DOCUMENT, Modality.fromString("document"));
		assertNull(Modality.fromString(null));

		ModalityTokenCount count = new ModalityTokenCount(Modality.AUDIO, 420L);
		assertEquals(Modality.AUDIO, count.modality());
		assertEquals(420L, count.tokenCount());
	}

	@Test
	public void testExtendedUsageMetadata() {
		List<ModalityTokenCount> promptBreakdown = List.of(new ModalityTokenCount(Modality.TEXT, 100L),
				new ModalityTokenCount(Modality.IMAGE, 258L));
		List<ModalityTokenCount> candidateBreakdown = List.of(new ModalityTokenCount(Modality.TEXT, 50L));

		UsageMetadata usage = new UsageMetadata(358, 0, 50, 20, 428, "priority", promptBreakdown, List.of(),
				candidateBreakdown, List.of());
		assertEquals(358, usage.promptTokenCount());
		assertEquals(0, usage.cachedContentTokenCount());
		assertEquals(50, usage.candidatesTokenCount());
		assertEquals(20, usage.thoughtsTokenCount());
		assertEquals(428, usage.totalTokenCount());
		assertEquals("priority", usage.serviceTier());
		assertEquals(2, usage.promptTokensDetails().size());
		assertEquals(1, usage.candidatesTokensDetails().size());

		// Test backward compatible constructor
		UsageMetadata legacyUsage = new UsageMetadata(100, 0, 20, 10, 130);
		assertEquals(100, legacyUsage.promptTokenCount());
		assertNull(legacyUsage.serviceTier());
		assertTrue(legacyUsage.promptTokensDetails().isEmpty());
	}

	@Test
	public void testGenerationConfigWithServiceTier() {
		GenerationConfig genConfig = GenerationConfig.builder().thinkingLevel(ThinkingLevel.HIGH)
				.serviceTier(ServiceTier.PRIORITY).temperature(0.7).build();

		assertEquals(ThinkingLevel.HIGH, genConfig.thinkingLevel());
		assertEquals(ServiceTier.PRIORITY, genConfig.serviceTier());
		assertEquals(0.7, genConfig.temperature());
	}

	@Test
	public void testAgentConfigWithBudgetAndBehavior() {
		BudgetConfig budget = BudgetConfig.builder().maxModelCalls(3).maxToolCalls(5).build();
		AgentConfig config = AgentConfig.builder().instructions("Helpful assistant").budgetConfig(budget)
				.agentBehavior(AgentBehavior.AUTONOMOUS).build();

		assertNotNull(config.getBudgetConfig());
		assertEquals(3, config.getBudgetConfig().maxModelCalls());
		assertEquals(5, config.getBudgetConfig().maxToolCalls());
		assertEquals(AgentBehavior.AUTONOMOUS, config.getAgentBehavior());
	}
}
