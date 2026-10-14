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

import java.util.List;
import java.util.Objects;

/**
 * Replaces parent skills with an explicit set of filesystem directory paths or
 * in-memory {@link InlineSkill} definitions.
 *
 * @param skillsPaths
 *            filesystem directories containing SKILL.md files for this subagent
 * @param inlineSkills
 *            in-memory skill definitions for this subagent
 */
public record SubagentOverrideSkillsConfig(List<String> skillsPaths,
		List<InlineSkill> inlineSkills) implements SubagentSkillsConfig {

	public SubagentOverrideSkillsConfig {
		skillsPaths = skillsPaths != null ? List.copyOf(skillsPaths) : List.of();
		inlineSkills = inlineSkills != null ? List.copyOf(inlineSkills) : List.of();
		if (skillsPaths.isEmpty() && inlineSkills.isEmpty()) {
			throw new IllegalArgumentException(
					"SubagentOverrideSkillsConfig requires at least one skill path or inline skill. Use SubagentNoneSkillsConfig to disable skills.");
		}
		if (!skillsPaths.isEmpty() && !inlineSkills.isEmpty()) {
			throw new IllegalArgumentException(
					"combining inline_skills and skills_paths is not supported by LocalHarness; provide either inline_skills or skills_paths, not both.");
		}
	}

	/**
	 * Creates an override config with the given skill paths.
	 *
	 * @param skillsPaths
	 *            directories containing SKILL.md files
	 */
	public SubagentOverrideSkillsConfig(List<String> skillsPaths) {
		this(Objects.requireNonNull(skillsPaths, "skillsPaths must not be null"), List.of());
	}

	/**
	 * Creates an override config with the given skill paths.
	 *
	 * @param skillsPaths
	 *            directories containing SKILL.md files
	 * @return override skills config
	 */
	public static SubagentOverrideSkillsConfig of(List<String> skillsPaths) {
		return new SubagentOverrideSkillsConfig(skillsPaths);
	}

	/**
	 * Creates an override config with the given skill paths.
	 *
	 * @param skillsPaths
	 *            directories containing SKILL.md files
	 * @return override skills config
	 */
	public static SubagentOverrideSkillsConfig of(String... skillsPaths) {
		return new SubagentOverrideSkillsConfig(List.of(skillsPaths));
	}

	/**
	 * Creates an override config with the given in-memory skills.
	 *
	 * @param inlineSkills
	 *            in-memory skill definitions
	 * @return override skills config
	 */
	public static SubagentOverrideSkillsConfig ofInlineSkills(List<InlineSkill> inlineSkills) {
		Objects.requireNonNull(inlineSkills, "inlineSkills must not be null");
		return new SubagentOverrideSkillsConfig(List.of(), inlineSkills);
	}

	/**
	 * Creates an override config with the given in-memory skills.
	 *
	 * @param inlineSkills
	 *            in-memory skill definitions
	 * @return override skills config
	 */
	public static SubagentOverrideSkillsConfig ofInlineSkills(InlineSkill... inlineSkills) {
		return new SubagentOverrideSkillsConfig(List.of(), List.of(inlineSkills));
	}
}
