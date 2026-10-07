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

/**
 * Configuration for how a subagent discovers and accesses skills.
 *
 * Subagents can either inherit parent skills (optionally filtered by name or
 * augmented with extra paths), disable all skills, or override parent skills
 * with explicit paths.
 */
public sealed interface SubagentSkillsConfig
		permits SubagentInheritSkillsConfig, SubagentNoneSkillsConfig, SubagentOverrideSkillsConfig {

	/**
	 * Inherits all parent skills without filters or extra paths.
	 *
	 * @return inherit skills config
	 */
	static SubagentInheritSkillsConfig inherit() {
		return new SubagentInheritSkillsConfig(List.of(), List.of());
	}

	/**
	 * Inherits only the specified parent skills by name.
	 *
	 * @param skillNames
	 *            allowlist of parent skill names
	 * @return inherit skills config
	 */
	static SubagentInheritSkillsConfig inherit(List<String> skillNames) {
		return new SubagentInheritSkillsConfig(skillNames, List.of());
	}

	/**
	 * Inherits parent skills filtered by name, plus additional skill directory
	 * paths.
	 *
	 * @param skillNames
	 *            allowlist of parent skill names
	 * @param extraSkillsPaths
	 *            additional directory paths containing SKILL.md files
	 * @return inherit skills config
	 */
	static SubagentInheritSkillsConfig inherit(List<String> skillNames, List<String> extraSkillsPaths) {
		return new SubagentInheritSkillsConfig(skillNames, extraSkillsPaths);
	}

	/**
	 * Inherits parent skills plus additional skill directory paths.
	 *
	 * @param extraSkillsPaths
	 *            additional directory paths containing SKILL.md files
	 * @return inherit skills config
	 */
	static SubagentInheritSkillsConfig inheritWithExtraPaths(List<String> extraSkillsPaths) {
		return new SubagentInheritSkillsConfig(List.of(), extraSkillsPaths);
	}

	/**
	 * Inherits only the specified parent skills by name.
	 *
	 * @param skillNames
	 *            allowlist of parent skill names
	 * @return inherit skills config
	 */
	static SubagentInheritSkillsConfig inherit(String... skillNames) {
		return new SubagentInheritSkillsConfig(List.of(skillNames), List.of());
	}

	/**
	 * Disables all skills (and lookup_skill) for the subagent.
	 *
	 * @return none skills config
	 */
	static SubagentNoneSkillsConfig none() {
		return SubagentNoneSkillsConfig.INSTANCE;
	}

	/**
	 * Overrides and replaces parent skills with the specified filesystem directory
	 * paths.
	 *
	 * @param skillsPaths
	 *            filesystem paths containing SKILL.md files
	 * @return override skills config
	 */
	static SubagentOverrideSkillsConfig override(List<String> skillsPaths) {
		return new SubagentOverrideSkillsConfig(skillsPaths);
	}

	/**
	 * Overrides and replaces parent skills with the specified filesystem directory
	 * paths.
	 *
	 * @param skillsPaths
	 *            filesystem paths containing SKILL.md files
	 * @return override skills config
	 */
	static SubagentOverrideSkillsConfig override(String... skillsPaths) {
		return new SubagentOverrideSkillsConfig(List.of(skillsPaths));
	}
}
