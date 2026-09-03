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

import io.github.glaforge.antigravity.AgentBehavior;
import io.github.glaforge.antigravity.localharness.CallHookRequest;
import io.github.glaforge.antigravity.localharness.CallHookResponse;
import io.github.glaforge.antigravity.localharness.LifecycleHook;
import io.github.glaforge.antigravity.localharness.RunCommandToolConfig;
import io.github.glaforge.antigravity.localharness.StopArgs;
import io.github.glaforge.antigravity.localharness.StopResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity0116Test {

	@Test
	public void testDefaultModelUpdatedToGemini38Flash() {
		assertEquals("gemini-3.8-flash", AgentConfig.DEFAULT_MODEL_NAME);

		AgentConfig config = AgentConfig.builder().build();
		assertEquals("gemini-3.8-flash", config.getModelName());
	}

	@Test
	public void testLightweightAgentConfiguration() {
		AgentConfig config = AgentConfig.builder().instructions("Lightweight agent for small models").lightweight()
				.build();

		assertEquals(AgentBehavior.MINIMAL, config.getAgentBehavior());
		assertFalse(config.getCapabilities().enableSubagents());
		assertFalse(config.getCapabilities().allowUserQuestions());
		assertFalse(config.getCapabilities().enableShell());
		assertFalse(config.getCapabilities().enableWebSearch());
		assertFalse(config.getCapabilities().enableUrlReading());
		assertTrue(config.getCapabilities().enableViewFile());
		assertTrue(config.getCapabilities().enableListDir());
		assertTrue(config.getCapabilities().enableGrepSearch());
	}

	@Test
	public void testRunCommandSandboxing() {
		RunCommandConfig runCmd = RunCommandConfig.builder().enableDaemons(true).enableSandbox(true)
				.timeoutSeconds(60.0).build();

		assertTrue(runCmd.enableDaemons());
		assertTrue(runCmd.enableSandbox());
		assertEquals(60.0, runCmd.timeoutSeconds());

		RunCommandToolConfig protoConfig = RunCommandToolConfig.newBuilder().setEnabled(true)
				.setEnableDaemonCommands(runCmd.enableDaemons()).setEnableSandbox(runCmd.enableSandbox())
				.setMaxTimeoutMs(60000).build();

		assertTrue(protoConfig.getEnableSandbox());
	}

	@Test
	public void testStopHookAndArgsProto() {
		StopArgs stopArgs = StopArgs.newBuilder().setTrajectoryId("traj-123").setContinuationCount(2)
				.setStopReason("user_halt").setResponseText("Execution cancelled by user")
				.setErrorMessage("Interrupted").build();

		assertEquals("traj-123", stopArgs.getTrajectoryId());
		assertEquals(2, stopArgs.getContinuationCount());
		assertEquals("user_halt", stopArgs.getStopReason());
		assertEquals("Execution cancelled by user", stopArgs.getResponseText());
		assertEquals("Interrupted", stopArgs.getErrorMessage());

		CallHookRequest request = CallHookRequest.newBuilder().setRequestId("req-stop-1")
				.setType(LifecycleHook.LIFECYCLE_HOOK_STOP).setClientId("client-xyz").setStopArgs(stopArgs).build();

		assertEquals(LifecycleHook.LIFECYCLE_HOOK_STOP, request.getType());
		assertEquals("client-xyz", request.getClientId());
		assertTrue(request.hasStopArgs());

		StopResult stopResult = StopResult.newBuilder().setDecision(StopResult.Decision.ALLOW)
				.setReason("Clean stop confirmed").build();

		CallHookResponse response = CallHookResponse.newBuilder().setRequestId("req-stop-1").setStopResult(stopResult)
				.build();

		assertTrue(response.hasStopResult());
		assertEquals(StopResult.Decision.ALLOW, response.getStopResult().getDecision());
		assertEquals("Clean stop confirmed", response.getStopResult().getReason());
	}

	@Test
	public void testOnStopHookBuilder() {
		AgentConfig config = AgentConfig.builder().addOnStopHook((args, ctx) -> CompletableFuture.completedFuture(null))
				.build();

		assertEquals(1, config.getHooks().size());
	}
}
