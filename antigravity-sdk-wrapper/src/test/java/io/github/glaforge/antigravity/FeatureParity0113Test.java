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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.github.glaforge.antigravity.hooks.HookResult;
import io.github.glaforge.antigravity.hooks.ToolCall;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity0113Test {

	@Test
	public void testRunCommandConfigDefaultsAndBuilder() {
		RunCommandConfig defaults = RunCommandConfig.defaults();
		assertFalse(defaults.enableDaemons());
		assertNull(defaults.timeoutSeconds());

		RunCommandConfig custom = RunCommandConfig.builder().enableDaemons(true).timeoutSeconds(120.5).build();

		assertTrue(custom.enableDaemons());
		assertEquals(120.5, custom.timeoutSeconds());
	}

	@Test
	public void testRunCommandConfigValidation() {
		assertThrows(IllegalArgumentException.class, () -> new RunCommandConfig(false, 0.0));
		assertThrows(IllegalArgumentException.class, () -> new RunCommandConfig(false, -5.0));
		assertThrows(IllegalArgumentException.class, () -> RunCommandConfig.builder().timeoutSeconds(-1.0).build());
	}

	@Test
	public void testCapabilitiesConfigWithRunCommandConfig() {
		RunCommandConfig runCmd = RunCommandConfig.builder().enableDaemons(true).timeoutSeconds(60.0).build();

		CapabilitiesConfig caps = CapabilitiesConfig.builder().enableShell(true).runCommandConfig(runCmd).build();

		assertTrue(caps.enableShell());
		assertNotNull(caps.runCommandConfig());
		assertTrue(caps.runCommandConfig().enableDaemons());
		assertEquals(60.0, caps.runCommandConfig().timeoutSeconds());

		AgentConfig agentConfig = AgentConfig.builder().capabilities(caps).build();

		assertNotNull(agentConfig.getCapabilities().runCommandConfig());
		assertTrue(agentConfig.getCapabilities().runCommandConfig().enableDaemons());
	}

	@Test
	public void testWorkspaceContainmentEnum() {
		assertEquals(WorkspaceContainment.ENABLED, WorkspaceContainment.fromString("enabled"));
		assertEquals(WorkspaceContainment.ENABLED, WorkspaceContainment.fromString("ENABLED"));
		assertEquals(WorkspaceContainment.DISABLED, WorkspaceContainment.fromString("disabled"));
		assertEquals(WorkspaceContainment.DISABLED, WorkspaceContainment.fromString("DISABLED"));
		assertEquals(WorkspaceContainment.UNSPECIFIED, WorkspaceContainment.fromString("unspecified"));
		assertNull(WorkspaceContainment.fromString(null));
		assertNull(WorkspaceContainment.fromString("invalid_value"));
	}

	@Test
	public void testAgentConfigWorkspaceContainment() {
		AgentConfig config = AgentConfig.builder().workspaceContainment(WorkspaceContainment.ENABLED).build();

		assertEquals(WorkspaceContainment.ENABLED, config.getWorkspaceContainment());
	}

	@Test
	public void testToolCallStepIdAndCorrelationFields() {
		var args = JsonNodeFactory.instance.objectNode().put("path", "file.txt");
		ToolCall call = new ToolCall("view_file", args, "call_123", "traj_main:4", "filesystem_server");

		assertEquals("view_file", call.name());
		assertEquals(args, call.args());
		assertEquals("call_123", call.id());
		assertEquals("traj_main:4", call.stepId());
		assertEquals("filesystem_server", call.serverName());

		// Test backward compatibility constructor
		ToolCall legacy = new ToolCall("run_command", args);
		assertEquals("run_command", legacy.name());
		assertEquals(args, legacy.args());
		assertNull(legacy.id());
		assertNull(legacy.stepId());
		assertNull(legacy.serverName());
	}

	@Test
	public void testToolExecutionErrorWithStepId() {
		Exception cause = new IllegalStateException("Process exited with code 1");
		ToolExecutionError error = new ToolExecutionError("run_command", "{\"cmd\":\"bad\"}", "server_1", "call_abc",
				"traj-1:3", "Command execution failed", cause);

		assertEquals("run_command", error.getToolName());
		assertEquals("{\"cmd\":\"bad\"}", error.getArgumentsJson());
		assertEquals("server_1", error.getServerName());
		assertEquals("call_abc", error.getCallId());
		assertEquals("traj-1:3", error.getStepId());
		assertEquals("Command execution failed", error.getMessage());
		assertSame(cause, error.getCause());

		// Test legacy constructor
		ToolExecutionError legacy = new ToolExecutionError("custom_tool", "{}");
		assertEquals("custom_tool", legacy.getToolName());
		assertEquals("{}", legacy.getArgumentsJson());
		assertNull(legacy.getServerName());
		assertNull(legacy.getCallId());
		assertNull(legacy.getStepId());
	}

	@Test
	public void testHookResultWithModifiedArguments() {
		HookResult result = HookResult.allowedWithModifiedArguments("{\"cmd\":\"echo safe\"}");
		assertTrue(result.allow());
		assertNull(result.reason());
		assertEquals("{\"cmd\":\"echo safe\"}", result.modifiedArgumentsJson());

		HookResult custom = HookResult.builder().allow(true).reason("rewritten command")
				.modifiedArgumentsJson("{\"cmd\":\"safe_run\"}").build();

		assertTrue(custom.allow());
		assertEquals("rewritten command", custom.reason());
		assertEquals("{\"cmd\":\"safe_run\"}", custom.modifiedArgumentsJson());
	}
}
