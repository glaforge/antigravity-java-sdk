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
 * Replaces parent skills with an explicit set of filesystem directory paths.
 *
 * @param skillsPaths
 *            filesystem directories containing SKILL.md files for this subagent
 */
public record SubagentOverrideSkillsConfig(List<String> skillsPaths) implements SubagentSkillsConfig {

	public SubagentOverrideSkillsConfig {
		Objects.requireNonNull(skillsPaths, "skillsPaths must not be null");
		if (skillsPaths.isEmpty()) {
			throw new IllegalArgumentException(
					"SubagentOverrideSkillsConfig requires at least one skill path. Use SubagentNoneSkillsConfig to disable skills.");
		}
		skillsPaths = List.copyOf(skillsPaths);
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
}
