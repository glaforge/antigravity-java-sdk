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
 * Inherits parent skills, optionally filtered by skill name or augmented with
 * extra filesystem paths.
 *
 * @param skillNames
 *            allowlist of parent skill names to inherit (empty inherits all
 *            parent skills)
 * @param extraSkillsPaths
 *            additional directory paths containing SKILL.md files
 */
public record SubagentInheritSkillsConfig(List<String> skillNames,
		List<String> extraSkillsPaths) implements SubagentSkillsConfig {

	public SubagentInheritSkillsConfig {
		skillNames = skillNames != null ? List.copyOf(skillNames) : List.of();
		extraSkillsPaths = extraSkillsPaths != null ? List.copyOf(extraSkillsPaths) : List.of();
	}

	/**
	 * Creates an inherit config with all parent skills.
	 *
	 * @return inherit config
	 */
	public static SubagentInheritSkillsConfig all() {
		return new SubagentInheritSkillsConfig(List.of(), List.of());
	}

	/**
	 * Creates an inherit config filtered by allowed skill names.
	 *
	 * @param skillNames
	 *            allowlist of skill names
	 * @return inherit config
	 */
	public static SubagentInheritSkillsConfig of(List<String> skillNames) {
		return new SubagentInheritSkillsConfig(skillNames, List.of());
	}

	/**
	 * Creates an inherit config with extra filesystem paths.
	 *
	 * @param extraSkillsPaths
	 *            extra filesystem paths
	 * @return inherit config
	 */
	public static SubagentInheritSkillsConfig withExtraPaths(List<String> extraSkillsPaths) {
		return new SubagentInheritSkillsConfig(List.of(), extraSkillsPaths);
	}

	/**
	 * Creates an inherit config with both allowed names and extra paths.
	 *
	 * @param skillNames
	 *            allowed skill names
	 * @param extraSkillsPaths
	 *            extra filesystem paths
	 * @return inherit config
	 */
	public static SubagentInheritSkillsConfig of(List<String> skillNames, List<String> extraSkillsPaths) {
		return new SubagentInheritSkillsConfig(skillNames, extraSkillsPaths);
	}
}
