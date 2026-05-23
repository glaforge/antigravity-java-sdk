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
import java.nio.file.Path;
import java.util.List;
import java.util.Collections;
import io.github.glaforge.antigravity.hooks.AgentHook;

/**
 * Configuration for the AntigravityAgent.
 */
public class AgentConfig {
	private final String persona;
	private final String modelName;
	private final List<Object> toolInstances;
	private final List<String> skillsPaths;
	private final boolean enableSubagents;
	private final boolean allowUserQuestions;
	private final Path workspaceDir;
	private final List<AgentHook> hooks;
	private final String saveDir;
	private final String appDataDir;
	private final String conversationId;
	private final List<Policy> policies;
	private final String finishToolSchemaJson;
	private final List<McpServerConfig> mcpServers;

	private AgentConfig(Builder builder) {
		this.persona = builder.persona;
		this.modelName = builder.modelName;
		this.toolInstances = new ArrayList<>(builder.toolInstances);
		this.skillsPaths = new ArrayList<>(builder.skillsPaths);
		this.enableSubagents = builder.enableSubagents;
		this.allowUserQuestions = builder.allowUserQuestions;
		this.workspaceDir = builder.workspaceDir;
		this.hooks = new ArrayList<>(builder.hooks);
		this.saveDir = builder.saveDir;
		this.appDataDir = builder.appDataDir;
		this.conversationId = builder.conversationId;
		this.policies = new ArrayList<>(builder.policies);
		this.finishToolSchemaJson = builder.finishToolSchemaJson;
		this.mcpServers = new ArrayList<>(builder.mcpServers);
	}

	/**
	 * Returns the persona.
	 *
	 * @return the persona
	 */
	public String getPersona() {
		return persona;
	}
	/**
	 * Returns the model name.
	 *
	 * @return the model name
	 */
	public String getModelName() {
		return modelName;
	}
	/**
	 * Returns the tool instances.
	 *
	 * @return the tool instances
	 */
	public List<Object> getToolInstances() {
		return Collections.unmodifiableList(toolInstances);
	}
	/**
	 * Returns the skill paths.
	 *
	 * @return the skill paths
	 */
	public List<String> getSkillsPaths() {
		return Collections.unmodifiableList(skillsPaths);
	}
	/**
	 * Returns whether subagents are enabled.
	 *
	 * @return whether subagents are enabled
	 */
	public boolean isEnableSubagents() {
		return enableSubagents;
	}
	/**
	 * Returns whether user questions are allowed.
	 *
	 * @return whether user questions are allowed
	 */
	public boolean isAllowUserQuestions() {
		return allowUserQuestions;
	}
	/**
	 * Returns the workspace directory.
	 *
	 * @return the workspace directory
	 */
	public Path getWorkspaceDir() {
		return workspaceDir;
	}
	/**
	 * Returns the registered hooks.
	 *
	 * @return the registered hooks
	 */
	public List<AgentHook> getHooks() {
		return Collections.unmodifiableList(hooks);
	}
	/**
	 * Returns the save directory.
	 *
	 * @return the save directory
	 */
	public String getSaveDir() {
		return saveDir;
	}
	/**
	 * Returns the app data directory.
	 *
	 * @return the app data directory
	 */
	public String getAppDataDir() {
		return appDataDir;
	}
	/**
	 * Returns the conversation ID.
	 *
	 * @return the conversation ID
	 */
	public String getConversationId() {
		return conversationId;
	}
	/**
	 * Returns the policies.
	 *
	 * @return the policies
	 */
	public List<Policy> getPolicies() {
		return Collections.unmodifiableList(policies);
	}
	/**
	 * Returns the finish tool schema JSON.
	 *
	 * @return the finish tool schema JSON
	 */
	public String getFinishToolSchemaJson() {
		return finishToolSchemaJson;
	}
	/**
	 * Returns the MCP server configurations.
	 *
	 * @return the MCP server configurations
	 */
	public List<McpServerConfig> getMcpServers() {
		return Collections.unmodifiableList(mcpServers);
	}

	/**
	 * Creates a new Builder for AgentConfig.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for AgentConfig.
	 */
	public static class Builder {
		/** Default constructor. */
		public Builder() {
		}
		private String persona = "";
		private String modelName = "gemini-2.5-flash";
		private List<Object> toolInstances = new ArrayList<>();
		private List<String> skillsPaths = new ArrayList<>();
		private boolean enableSubagents = false;
		private boolean allowUserQuestions = false;
		private Path workspaceDir = Path.of(System.getProperty("user.dir"));
		private List<AgentHook> hooks = new ArrayList<>();
		private String saveDir = System.getProperty("java.io.tmpdir") + "/antigravity-java";
		private String appDataDir = null;
		private String conversationId = "";
		private List<Policy> policies = new ArrayList<>();
		private String finishToolSchemaJson = null;
		private List<McpServerConfig> mcpServers = new ArrayList<>();

		/**
		 * Sets the persona.
		 *
		 * @param persona
		 *            the persona
		 * @return this builder
		 */
		public Builder persona(String persona) {
			this.persona = persona;
			return this;
		}

		/**
		 * Sets the model name.
		 *
		 * @param modelName
		 *            the model name
		 * @return this builder
		 */
		public Builder modelName(String modelName) {
			this.modelName = modelName;
			return this;
		}

		/**
		 * Adds a tool instance.
		 *
		 * @param toolInstance
		 *            the tool instance
		 * @return this builder
		 */
		public Builder addTool(Object toolInstance) {
			this.toolInstances.add(toolInstance);
			return this;
		}

		/**
		 * Adds a skill path.
		 *
		 * @param skillPath
		 *            the skill path
		 * @return this builder
		 */
		public Builder addSkillPath(String skillPath) {
			this.skillsPaths.add(skillPath);
			return this;
		}

		/**
		 * Enables or disables subagents.
		 *
		 * @param enableSubagents
		 *            true to enable subagents
		 * @return this builder
		 */
		public Builder enableSubagents(boolean enableSubagents) {
			this.enableSubagents = enableSubagents;
			return this;
		}

		/**
		 * Enables or disables user questions.
		 *
		 * @param allowUserQuestions
		 *            true to allow user questions
		 * @return this builder
		 */
		public Builder allowUserQuestions(boolean allowUserQuestions) {
			this.allowUserQuestions = allowUserQuestions;
			return this;
		}

		/**
		 * Adds a hook.
		 *
		 * @param hook
		 *            the hook
		 * @return this builder
		 */
		public Builder addHook(AgentHook hook) {
			this.hooks.add(hook);
			return this;
		}

		/**
		 * Sets the save directory.
		 *
		 * @param saveDir
		 *            the save directory
		 * @return this builder
		 */
		public Builder saveDir(String saveDir) {
			this.saveDir = saveDir;
			return this;
		}

		/**
		 * Sets the app data directory.
		 *
		 * @param appDataDir
		 *            the app data directory
		 * @return this builder
		 */
		public Builder appDataDir(String appDataDir) {
			this.appDataDir = appDataDir;
			return this;
		}

		/**
		 * Sets the conversation ID.
		 *
		 * @param conversationId
		 *            the conversation ID
		 * @return this builder
		 */
		public Builder conversationId(String conversationId) {
			this.conversationId = conversationId;
			return this;
		}

		/**
		 * Adds a policy.
		 *
		 * @param policy
		 *            the policy
		 * @return this builder
		 */
		public Builder addPolicy(Policy policy) {
			this.policies.add(policy);
			return this;
		}

		/**
		 * Sets the finish tool schema JSON.
		 *
		 * @param finishToolSchemaJson
		 *            the JSON schema
		 * @return this builder
		 */
		public Builder finishToolSchemaJson(String finishToolSchemaJson) {
			this.finishToolSchemaJson = finishToolSchemaJson;
			return this;
		}

		/**
		 * Adds an MCP server configuration.
		 *
		 * @param mcpServerConfig
		 *            the MCP server configuration
		 * @return this builder
		 */
		public Builder addMcpServer(McpServerConfig mcpServerConfig) {
			this.mcpServers.add(mcpServerConfig);
			return this;
		}

		/**
		 * Builds the AgentConfig.
		 *
		 * @return a new AgentConfig
		 */
		public AgentConfig build() {
			return new AgentConfig(this);
		}
	}
}
