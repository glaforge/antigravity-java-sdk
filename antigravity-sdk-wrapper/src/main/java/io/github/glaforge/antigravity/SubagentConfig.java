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
import java.util.List;
import java.util.Objects;

/**
 * Configuration for a custom subagent.
 *
 * @param name
 *            unique name identifier of the subagent
 * @param description
 *            description of the subagent's role and capabilities
 * @param instructions
 *            optional prompt instructions for the subagent
 * @param model
 *            optional custom model target for the subagent (e.g.
 *            "gemini-2.5-pro")
 * @param tools
 *            optional list of tool names to enable for the subagent
 * @param capabilities
 *            optional capabilities config controlling allowed tools
 * @param agentBehavior
 *            optional agent behavior mode
 */
public record SubagentConfig(String name, String description, String instructions, String model, List<String> tools,
		CapabilitiesConfig capabilities, AgentBehavior agentBehavior) {

	/**
	 * Canonical constructor with defensive null and list handling.
	 */
	public SubagentConfig {
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(description, "description must not be null");
		tools = tools != null ? List.copyOf(tools) : List.of();
	}

	/**
	 * Convenience constructor with minimal fields.
	 *
	 * @param name
	 *            unique name of the subagent
	 * @param description
	 *            description of the subagent
	 */
	public SubagentConfig(String name, String description) {
		this(name, description, null, null, List.of(), null, null);
	}

	/**
	 * Convenience constructor with model override.
	 *
	 * @param name
	 *            unique name of the subagent
	 * @param description
	 *            description of the subagent
	 * @param model
	 *            model target name
	 */
	public SubagentConfig(String name, String description, String model) {
		this(name, description, null, model, List.of(), null, null);
	}

	/**
	 * Returns an unmodifiable list of tools.
	 *
	 * @return tool names
	 */
	@Override
	public List<String> tools() {
		return Collections.unmodifiableList(tools);
	}

	/**
	 * Creates a new builder for {@link SubagentConfig}.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link SubagentConfig}.
	 */
	public static class Builder {
		private String name;
		private String description;
		private String instructions;
		private String model;
		private final List<String> tools = new ArrayList<>();
		private CapabilitiesConfig capabilities;
		private AgentBehavior agentBehavior;

		/** Default constructor. */
		public Builder() {
		}

		/**
		 * Sets the unique name of the subagent.
		 *
		 * @param name
		 *            subagent name
		 * @return this builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Sets the description of the subagent.
		 *
		 * @param description
		 *            subagent description
		 * @return this builder
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * Sets system prompt instructions for the subagent.
		 *
		 * @param instructions
		 *            prompt instructions
		 * @return this builder
		 */
		public Builder instructions(String instructions) {
			this.instructions = instructions;
			return this;
		}

		/**
		 * Sets a custom model target for the subagent.
		 *
		 * @param model
		 *            model name (e.g. "gemini-2.5-pro")
		 * @return this builder
		 */
		public Builder model(String model) {
			this.model = model;
			return this;
		}

		/**
		 * Adds a tool name for the subagent.
		 *
		 * @param toolName
		 *            tool name
		 * @return this builder
		 */
		public Builder addTool(String toolName) {
			if (toolName != null && !toolName.isBlank()) {
				this.tools.add(toolName);
			}
			return this;
		}

		/**
		 * Adds a builtin tool for the subagent.
		 *
		 * @param tool
		 *            builtin tool
		 * @return this builder
		 */
		public Builder addTool(BuiltinTools tool) {
			if (tool != null) {
				this.tools.add(tool.getValue());
			}
			return this;
		}

		/**
		 * Sets tools for the subagent.
		 *
		 * @param tools
		 *            list of tool names
		 * @return this builder
		 */
		public Builder tools(List<String> tools) {
			this.tools.clear();
			if (tools != null) {
				this.tools.addAll(tools);
			}
			return this;
		}

		/**
		 * Sets capabilities for the subagent.
		 *
		 * @param capabilities
		 *            capabilities config
		 * @return this builder
		 */
		public Builder capabilities(CapabilitiesConfig capabilities) {
			this.capabilities = capabilities;
			return this;
		}

		/**
		 * Sets the agent behavior mode for the subagent.
		 *
		 * @param agentBehavior
		 *            behavior mode
		 * @return this builder
		 */
		public Builder agentBehavior(AgentBehavior agentBehavior) {
			this.agentBehavior = agentBehavior;
			return this;
		}

		/**
		 * Builds a new {@link SubagentConfig}.
		 *
		 * @return new SubagentConfig instance
		 */
		public SubagentConfig build() {
			if (name == null || name.isBlank()) {
				throw new IllegalArgumentException("Subagent name must not be blank");
			}
			if (description == null || description.isBlank()) {
				throw new IllegalArgumentException("Subagent description must not be blank");
			}
			return new SubagentConfig(name, description, instructions, model, tools, capabilities, agentBehavior);
		}
	}
}
