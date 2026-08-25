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

import io.github.glaforge.antigravity.localharness.BudgetConfig;
import io.github.glaforge.antigravity.localharness.CallHookRequest;
import io.github.glaforge.antigravity.localharness.HarnessConfig;
import io.github.glaforge.antigravity.localharness.Modality;
import io.github.glaforge.antigravity.localharness.ModalityTokenCount;
import io.github.glaforge.antigravity.localharness.OnCompactionArgs;
import io.github.glaforge.antigravity.localharness.PolicyConfig;
import io.github.glaforge.antigravity.localharness.PreToolArgs;
import io.github.glaforge.antigravity.localharness.SubagentSkillsConfig;
import io.github.glaforge.antigravity.localharness.TrajectoryStateUpdate;
import io.github.glaforge.antigravity.localharness.TrajectoryStateUpdate.StopReason;
import io.github.glaforge.antigravity.localharness.UsageMetadata;
import io.github.glaforge.antigravity.localharness.WorkspaceContainment;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity0114Test {

	@Test
	public void testBudgetConfigProtobuf() {
		BudgetConfig budget = BudgetConfig.newBuilder().setMaxModelCalls(10).setMaxToolCalls(25)
				.setMaxInputTokens(50000L).setMaxOutputTokens(10000L).setMaxTotalTokens(60000L).build();

		HarnessConfig config = HarnessConfig.newBuilder().setBudgetConfig(budget).build();

		assertTrue(config.hasBudgetConfig());
		assertEquals(10, config.getBudgetConfig().getMaxModelCalls());
		assertEquals(25, config.getBudgetConfig().getMaxToolCalls());
		assertEquals(50000L, config.getBudgetConfig().getMaxInputTokens());
		assertEquals(10000L, config.getBudgetConfig().getMaxOutputTokens());
		assertEquals(60000L, config.getBudgetConfig().getMaxTotalTokens());
	}

	@Test
	public void testStopReasonAndTrajectoryHierarchy() {
		TrajectoryStateUpdate update = TrajectoryStateUpdate.newBuilder().setTrajectoryId("traj-child")
				.setParentTrajectoryId("traj-root").setDepth(2)
				.setStopReason(StopReason.STOP_REASON_MAX_TOOL_CALLS_EXCEEDED).build();

		assertEquals("traj-child", update.getTrajectoryId());
		assertEquals("traj-root", update.getParentTrajectoryId());
		assertEquals(2, update.getDepth());
		assertEquals(StopReason.STOP_REASON_MAX_TOOL_CALLS_EXCEEDED, update.getStopReason());
	}

	@Test
	public void testModalityTokenDetails() {
		ModalityTokenCount promptCount = ModalityTokenCount.newBuilder().setModality(Modality.TEXT).setTokenCount(1200L)
				.build();

		ModalityTokenCount imageCount = ModalityTokenCount.newBuilder().setModality(Modality.IMAGE).setTokenCount(500L)
				.build();

		UsageMetadata usage = UsageMetadata.newBuilder().setPromptTokenCount(1700L).addPromptTokensDetails(promptCount)
				.addPromptTokensDetails(imageCount).build();

		assertEquals(1700L, usage.getPromptTokenCount());
		assertEquals(2, usage.getPromptTokensDetailsCount());
		assertEquals(Modality.TEXT, usage.getPromptTokensDetails(0).getModality());
		assertEquals(1200L, usage.getPromptTokensDetails(0).getTokenCount());
		assertEquals(Modality.IMAGE, usage.getPromptTokensDetails(1).getModality());
		assertEquals(500L, usage.getPromptTokensDetails(1).getTokenCount());
	}

	@Test
	public void testCompactionHookArgs() {
		OnCompactionArgs compactionArgs = OnCompactionArgs.newBuilder().setTrajectoryId("traj-1").setStepIndex(5)
				.setSummary("Condensed conversation history").build();

		CallHookRequest request = CallHookRequest.newBuilder().setOnCompactionArgs(compactionArgs).build();

		assertTrue(request.hasOnCompactionArgs());
		assertEquals("traj-1", request.getOnCompactionArgs().getTrajectoryId());
		assertEquals(5, request.getOnCompactionArgs().getStepIndex());
		assertEquals("Condensed conversation history", request.getOnCompactionArgs().getSummary());
	}

	@Test
	public void testPreToolArgsWithTraceContext() {
		PreToolArgs args = PreToolArgs.newBuilder().setToolName("run_command").setArgumentsJson("{\"cmd\":\"ls\"}")
				.setCallId("call-99").setTrajectoryId("traj-42").setStepIndex(3).build();

		assertEquals("run_command", args.getToolName());
		assertEquals("call-99", args.getCallId());
		assertEquals("traj-42", args.getTrajectoryId());
		assertEquals(3, args.getStepIndex());
	}

	@Test
	public void testWorkspaceContainmentInPolicyConfig() {
		PolicyConfig policyConfig = PolicyConfig.newBuilder()
				.setWorkspaceContainment(WorkspaceContainment.WORKSPACE_CONTAINMENT_ENABLED).build();

		assertEquals(WorkspaceContainment.WORKSPACE_CONTAINMENT_ENABLED, policyConfig.getWorkspaceContainment());
	}

	@Test
	public void testSubagentSkillsConfig() {
		SubagentSkillsConfig skillsConfig = SubagentSkillsConfig.newBuilder().setInheritConfig(
				SubagentSkillsConfig.InheritConfig.newBuilder().addExtraSkillsPaths("/extra/skill/path").build())
				.build();

		assertTrue(skillsConfig.hasInheritConfig());
		assertEquals("/extra/skill/path", skillsConfig.getInheritConfig().getExtraSkillsPaths(0));
	}
}
