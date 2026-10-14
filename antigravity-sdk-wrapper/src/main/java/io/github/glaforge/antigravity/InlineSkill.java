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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Definition of an in-memory Agent Skill.
 *
 * @param name
 *            unique name of the skill (e.g. "custom-math")
 * @param description
 *            guidance for when the model should select this skill
 * @param content
 *            markdown/text body containing the skill instructions
 * @param allowedTools
 *            optional list of tool names permitted for this skill
 * @param dependentTools
 *            optional list of tool names that this skill depends on (loaded
 *            dynamically when the skill is looked up)
 * @param dependentSkills
 *            optional list of skill names that this skill depends on
 * @param metadata
 *            optional key-value metadata for the skill (e.g.
 *            {@code {"visibility": "hidden"}})
 */
public record InlineSkill(String name, String description, String content, List<String> allowedTools,
		List<String> dependentTools, List<String> dependentSkills, Map<String, String> metadata) {

	/**
	 * Compact constructor validating required fields and making collections
	 * unmodifiable.
	 */
	public InlineSkill {
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(description, "description must not be null");
		Objects.requireNonNull(content, "content must not be null");
		if (name.isBlank()) {
			throw new IllegalArgumentException("InlineSkill name must not be blank");
		}
		allowedTools = allowedTools != null ? List.copyOf(allowedTools) : List.of();
		dependentTools = dependentTools != null ? List.copyOf(dependentTools) : List.of();
		dependentSkills = dependentSkills != null ? List.copyOf(dependentSkills) : List.of();
		metadata = metadata != null ? Collections.unmodifiableMap(new LinkedHashMap<>(metadata)) : Map.of();
	}

	/**
	 * Convenience constructor with required fields only.
	 *
	 * @param name
	 *            unique name of the skill
	 * @param description
	 *            guidance for when the model should select this skill
	 * @param content
	 *            markdown/text body containing the skill instructions
	 */
	public InlineSkill(String name, String description, String content) {
		this(name, description, content, List.of(), List.of(), List.of(), Map.of());
	}

	/**
	 * Creates a new InlineSkill with the specified name, description, and content.
	 *
	 * @param name
	 *            unique name of the skill
	 * @param description
	 *            guidance for when the model should select this skill
	 * @param content
	 *            markdown/text body containing the skill instructions
	 * @return a new InlineSkill instance
	 */
	public static InlineSkill of(String name, String description, String content) {
		return new InlineSkill(name, description, content);
	}

	/**
	 * Creates a new Builder for {@link InlineSkill}.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link InlineSkill}.
	 */
	public static class Builder {
		private String name;
		private String description;
		private String content;
		private final List<String> allowedTools = new ArrayList<>();
		private final List<String> dependentTools = new ArrayList<>();
		private final List<String> dependentSkills = new ArrayList<>();
		private final Map<String, String> metadata = new LinkedHashMap<>();

		/** Default constructor. */
		public Builder() {
		}

		/**
		 * Sets the unique skill name.
		 *
		 * @param name
		 *            skill name
		 * @return this builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Sets the skill description.
		 *
		 * @param description
		 *            skill description
		 * @return this builder
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Sets the markdown/text body of the skill.
		 *
		 * @param content
		 *            skill instructions content
		 * @return this builder
		 */
		public Builder content(String content) {
			this.content = content;
			return this;
		}

		/**
		 * Sets the list of allowed tool names for this skill.
		 *
		 * @param allowedTools
		 *            allowed tool names
		 * @return this builder
		 */
		public Builder allowedTools(List<String> allowedTools) {
			this.allowedTools.clear();
			if (allowedTools != null) {
				this.allowedTools.addAll(allowedTools);
			}
			return this;
		}

		/**
		 * Adds an allowed tool name for this skill.
		 *
		 * @param toolName
		 *            allowed tool name
		 * @return this builder
		 */
		public Builder addAllowedTool(String toolName) {
			if (toolName != null) {
				this.allowedTools.add(toolName);
			}
			return this;
		}

		/**
		 * Sets the list of dependent tool names for this skill.
		 *
		 * @param dependentTools
		 *            dependent tool names
		 * @return this builder
		 */
		public Builder dependentTools(List<String> dependentTools) {
			this.dependentTools.clear();
			if (dependentTools != null) {
				this.dependentTools.addAll(dependentTools);
			}
			return this;
		}

		/**
		 * Adds a dependent tool name for this skill.
		 *
		 * @param toolName
		 *            dependent tool name
		 * @return this builder
		 */
		public Builder addDependentTool(String toolName) {
			if (toolName != null) {
				this.dependentTools.add(toolName);
			}
			return this;
		}

		/**
		 * Sets the list of dependent skill names for this skill.
		 *
		 * @param dependentSkills
		 *            dependent skill names
		 * @return this builder
		 */
		public Builder dependentSkills(List<String> dependentSkills) {
			this.dependentSkills.clear();
			if (dependentSkills != null) {
				this.dependentSkills.addAll(dependentSkills);
			}
			return this;
		}

		/**
		 * Adds a dependent skill name for this skill.
		 *
		 * @param skillName
		 *            dependent skill name
		 * @return this builder
		 */
		public Builder addDependentSkill(String skillName) {
			if (skillName != null) {
				this.dependentSkills.add(skillName);
			}
			return this;
		}

		/**
		 * Sets key-value metadata for this skill.
		 *
		 * @param metadata
		 *            metadata map
		 * @return this builder
		 */
		public Builder metadata(Map<String, String> metadata) {
			this.metadata.clear();
			if (metadata != null) {
				this.metadata.putAll(metadata);
			}
			return this;
		}

		/**
		 * Adds a single key-value metadata entry for this skill.
		 *
		 * @param key
		 *            metadata key
		 * @param value
		 *            metadata value
		 * @return this builder
		 */
		public Builder addMetadata(String key, String value) {
			if (key != null && value != null) {
				this.metadata.put(key, value);
			}
			return this;
		}

		/**
		 * Builds the {@link InlineSkill} instance.
		 *
		 * @return new InlineSkill
		 */
		public InlineSkill build() {
			return new InlineSkill(name, description, content, allowedTools, dependentTools, dependentSkills, metadata);
		}
	}
}
