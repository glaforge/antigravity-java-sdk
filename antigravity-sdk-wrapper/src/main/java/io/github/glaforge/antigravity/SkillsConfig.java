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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for Agent Skills available to the agent.
 *
 * @param enabled
 *            whether skills are enabled (default: true)
 * @param skills
 *            the list of skill sources
 */
public record SkillsConfig(boolean enabled, List<SkillSource> skills) {

	public SkillsConfig {
		skills = skills != null ? Collections.unmodifiableList(new ArrayList<>(skills)) : List.of();
	}

	/**
	 * Creates a new Builder for SkillsConfig.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Creates a default enabled SkillsConfig with the given skill sources.
	 *
	 * @param skills
	 *            the skill sources
	 * @return a new SkillsConfig
	 */
	public static SkillsConfig of(List<SkillSource> skills) {
		return new SkillsConfig(true, skills);
	}

	/**
	 * Builder for {@link SkillsConfig}.
	 */
	public static class Builder {
		private boolean enabled = true;
		private final List<SkillSource> skills = new ArrayList<>();

		public Builder enabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder addSkill(SkillSource skill) {
			if (skill != null) {
				this.skills.add(skill);
			}
			return this;
		}

		public Builder addSkillPath(Path path) {
			if (path != null) {
				this.skills.add(SkillSource.fromPath(path));
			}
			return this;
		}

		public Builder addSkillDirectory(String directoryPath) {
			if (directoryPath != null) {
				this.skills.add(SkillSource.fromDirectory(directoryPath));
			}
			return this;
		}

		public Builder skills(List<SkillSource> skills) {
			this.skills.clear();
			if (skills != null) {
				this.skills.addAll(skills);
			}
			return this;
		}

		public SkillsConfig build() {
			return new SkillsConfig(enabled, skills);
		}
	}
}
