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
import io.github.glaforge.antigravity.localharness.ActionSkillLookup;
import io.github.glaforge.antigravity.localharness.TrajectoryStateUpdate;
import io.github.glaforge.antigravity.localharness.WorkspaceContainment;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
public class FeatureParity0120Test {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void testSkillsConfigRecordAndBuilder() {
		SkillSource source1 = SkillSource.fromDirectory("/path/to/skill-1");
		SkillSource source2 = SkillSource.fromPath(Path.of("/path/to/skill-2"));

		assertEquals("/path/to/skill-1", source1.directoryPath());
		assertNotNull(source2.directoryPath());

		SkillsConfig config = SkillsConfig.builder().enabled(true).addSkill(source1).addSkill(source2).build();

		assertTrue(config.enabled());
		assertEquals(2, config.skills().size());
		assertEquals("/path/to/skill-1", config.skills().get(0).directoryPath());
	}

	@Test
	public void testAgentConfigSkillsIntegration() {
		SkillSource skill = SkillSource.fromDirectory("/opt/custom-skills");
		AgentConfig config = AgentConfig.builder().instructions("Helpful skills agent").addSkill(skill).build();

		assertNotNull(config.getSkillsConfig());
		assertTrue(config.getSkillsConfig().enabled());
		assertEquals(1, config.getSkillsConfig().skills().size());
		assertEquals("/opt/custom-skills", config.getSkillsConfig().skills().get(0).directoryPath());
	}

	@Test
	public void testAgentConfigSkillsConfigOverride() {
		SkillsConfig skills = SkillsConfig.builder().enabled(false).addSkillDirectory("/var/skills").build();

		AgentConfig config = AgentConfig.builder().skillsConfig(skills).build();

		assertNotNull(config.getSkillsConfig());
		assertFalse(config.getSkillsConfig().enabled());
		assertEquals(1, config.getSkillsConfig().skills().size());
	}

	@Test
	public void testPolicyDenialReason() {
		Policy denyPolicy = Policies.denyAll("Strict security perimeter active");
		Policy.Decision decision = denyPolicy.evaluate("run_command", objectMapper.createObjectNode());

		assertEquals(Policy.Decision.DENY, decision);
		assertEquals("Strict security perimeter active", denyPolicy.reason());

		Policy denyTool = Policies.denyTool("run_command", "Shell execution disabled in sandbox");
		assertEquals(Policy.Decision.DENY, denyTool.evaluate("run_command", objectMapper.createObjectNode()));
		assertEquals("Shell execution disabled in sandbox", denyTool.reason());

		assertEquals(Policy.Decision.PASS, denyTool.evaluate("view_file", objectMapper.createObjectNode()));
	}

	@Test
	public void testPolicyMarkers() {
		Policy allowAll = Policies.allowAll();
		assertTrue(allowAll.isAllowAll());
		assertFalse(allowAll.isWorkspaceOnly());

		Policy workspaceOnly = Policies.workspaceOnly();
		assertTrue(workspaceOnly.isWorkspaceOnly());
		assertFalse(workspaceOnly.isAllowAll());
	}

	@Test
	public void testAgentExecutionException() {
		AgentExecutionException ex1 = new AgentExecutionException("Quota exhausted", "RESOURCE_EXHAUSTED");
		assertEquals("Quota exhausted", ex1.getMessage());
		assertEquals("RESOURCE_EXHAUSTED", ex1.getErrorCode());

		AgentExecutionException ex2 = new AgentExecutionException("Internal error");
		assertEquals("Internal error", ex2.getMessage());
		assertNull(ex2.getErrorCode());
	}

	@Test
	public void testProtobufSkillsAndActionSkillLookup() {
		io.github.glaforge.antigravity.localharness.SkillsConfig protoSkills = io.github.glaforge.antigravity.localharness.SkillsConfig
				.newBuilder().setEnabled(true).addSkills(io.github.glaforge.antigravity.localharness.SkillSource
						.newBuilder().setDirectoryPath("/skills/java").build())
				.build();

		assertTrue(protoSkills.getEnabled());
		assertEquals(1, protoSkills.getSkillsCount());
		assertEquals("/skills/java", protoSkills.getSkills(0).getDirectoryPath());

		ActionSkillLookup action = ActionSkillLookup.newBuilder()
				.setOperation(ActionSkillLookup.Operation.OPERATION_LOOKUP_SKILLS)
				.addRequestedSkillNames("java-refactor").addResolvedSkillNames("java-refactor").build();

		assertEquals(ActionSkillLookup.Operation.OPERATION_LOOKUP_SKILLS, action.getOperation());
		assertEquals(1, action.getRequestedSkillNamesCount());
		assertEquals("java-refactor", action.getRequestedSkillNames(0));
	}

	@Test
	public void testTrajectoryStateUpdateErrorCode() {
		TrajectoryStateUpdate update = TrajectoryStateUpdate.newBuilder().setTrajectoryId("traj-123")
				.setState(TrajectoryStateUpdate.State.STATE_FULLY_IDLE).setError("Tool execution timed out")
				.setErrorCode("TOOL_TIMEOUT").build();

		assertEquals("TOOL_TIMEOUT", update.getErrorCode());
		assertEquals("Tool execution timed out", update.getError());
	}
}
