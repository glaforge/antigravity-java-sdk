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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration helper for connecting to local OpenAI-compatible endpoints
 * (e.g. Ollama, LM Studio, vLLM).
 */
public class LocalOpenAIAgentConfig {
	private final String baseUrl;
	private final String modelName;
	private final String apiKey;
	private final Map<String, String> httpHeaders;
	private final AgentConfig agentConfig;

	private LocalOpenAIAgentConfig(Builder builder) {
		this.baseUrl = builder.baseUrl;
		this.modelName = builder.modelName;
		this.apiKey = builder.apiKey;
		this.httpHeaders = new HashMap<>(builder.httpHeaders);
		this.agentConfig = builder.agentConfigBuilder.modelName(builder.modelName).baseUrl(builder.baseUrl).build();
	}

	/**
	 * Returns the base URL of the local OpenAI endpoint.
	 *
	 * @return base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Returns the model name.
	 *
	 * @return model name
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Returns the API key.
	 *
	 * @return API key
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * Returns HTTP headers.
	 *
	 * @return HTTP headers
	 */
	public Map<String, String> getHttpHeaders() {
		return httpHeaders;
	}

	/**
	 * Returns the underlying AgentConfig.
	 *
	 * @return agent config
	 */
	public AgentConfig getAgentConfig() {
		return agentConfig;
	}

	/**
	 * Returns the session budget configuration.
	 *
	 * @return budget configuration
	 */
	public BudgetConfig getBudgetConfig() {
		return agentConfig.getBudgetConfig();
	}

	/**
	 * Returns the session continuation mode.
	 *
	 * @return session continuation mode
	 */
	public SessionContinuationMode getSessionContinuationMode() {
		return agentConfig.getSessionContinuationMode();
	}

	/**
	 * Returns the configured security policies.
	 *
	 * @return list of policies
	 */
	public List<Policy> getPolicies() {
		return agentConfig.getPolicies();
	}

	/**
	 * Returns the configured skill directory paths.
	 *
	 * @return list of skill paths
	 */
	public List<String> getSkillsPaths() {
		return agentConfig.getSkillsPaths();
	}

	/**
	 * Returns the configured in-memory skills.
	 *
	 * @return list of inline skills
	 */
	public List<InlineSkill> getInlineSkills() {
		return agentConfig.getInlineSkills();
	}

	/**
	 * Creates a new Builder for LocalOpenAIAgentConfig.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for LocalOpenAIAgentConfig.
	 */
	public static class Builder {
		private String baseUrl = "";
		private String modelName = "";
		private String apiKey = "";
		private Map<String, String> httpHeaders = new HashMap<>();
		private final AgentConfig.Builder agentConfigBuilder = AgentConfig.builder();

		/**
		 * Default constructor.
		 */
		public Builder() {
		}

		/**
		 * Sets the base URL of the local OpenAI-compatible endpoint.
		 *
		 * @param baseUrl
		 *            base URL string
		 * @return this builder
		 */
		public Builder baseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

		/**
		 * Sets the model name.
		 *
		 * @param modelName
		 *            model name string
		 * @return this builder
		 */
		public Builder modelName(String modelName) {
			this.modelName = modelName;
			return this;
		}

		/**
		 * Sets the API key.
		 *
		 * @param apiKey
		 *            API key string
		 * @return this builder
		 */
		public Builder apiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		/**
		 * Adds a custom HTTP header.
		 *
		 * @param name
		 *            header name
		 * @param value
		 *            header value
		 * @return this builder
		 */
		public Builder addHeader(String name, String value) {
			this.httpHeaders.put(name, value);
			return this;
		}

		/**
		 * Sets system instructions.
		 *
		 * @param instructions
		 *            system prompt
		 * @return this builder
		 */
		public Builder instructions(String instructions) {
			this.agentConfigBuilder.instructions(instructions);
			return this;
		}

		/**
		 * Adds a tool instance.
		 *
		 * @param toolInstance
		 *            tool object instance
		 * @return this builder
		 */
		public Builder addTool(Object toolInstance) {
			this.agentConfigBuilder.addTool(toolInstance);
			return this;
		}

		/**
		 * Sets agent capabilities.
		 *
		 * @param capabilities
		 *            capabilities config
		 * @return this builder
		 */
		public Builder capabilities(CapabilitiesConfig capabilities) {
			this.agentConfigBuilder.capabilities(capabilities);
			return this;
		}

		/**
		 * Adds an MCP server configuration.
		 *
		 * @param mcpServerConfig
		 *            MCP server config
		 * @return this builder
		 */
		public Builder addMcpServer(McpServerConfig mcpServerConfig) {
			this.agentConfigBuilder.addMcpServer(mcpServerConfig);
			return this;
		}

		/**
		 * Sets workspaces paths.
		 *
		 * @param workspaces
		 *            list of workspace paths
		 * @return this builder
		 */
		public Builder workspaces(List<String> workspaces) {
			this.agentConfigBuilder.workspaces(workspaces);
			return this;
		}

		/**
		 * Adds a workspace path.
		 *
		 * @param workspace
		 *            workspace directory path
		 * @return this builder
		 */
		public Builder addWorkspace(String workspace) {
			this.agentConfigBuilder.addWorkspace(workspace);
			return this;
		}

		/**
		 * Sets security policies.
		 *
		 * @param policies
		 *            list of policies
		 * @return this builder
		 */
		public Builder policies(List<Policy> policies) {
			this.agentConfigBuilder.policies(policies);
			return this;
		}

		/**
		 * Adds a security policy.
		 *
		 * @param policy
		 *            policy instance
		 * @return this builder
		 */
		public Builder addPolicy(Policy policy) {
			this.agentConfigBuilder.addPolicy(policy);
			return this;
		}

		/**
		 * Sets the session budget configuration for call counts and token caps.
		 *
		 * @param budgetConfig
		 *            the budget configuration
		 * @return this builder
		 */
		public Builder budgetConfig(BudgetConfig budgetConfig) {
			this.agentConfigBuilder.budgetConfig(budgetConfig);
			return this;
		}

		/**
		 * Sets the session continuation mode.
		 *
		 * @param sessionContinuationMode
		 *            the session continuation mode
		 * @return this builder
		 */
		public Builder sessionContinuationMode(SessionContinuationMode sessionContinuationMode) {
			this.agentConfigBuilder.sessionContinuationMode(sessionContinuationMode);
			return this;
		}

		/**
		 * Sets the conversation ID.
		 *
		 * @param conversationId
		 *            the conversation identifier
		 * @return this builder
		 */
		public Builder conversationId(String conversationId) {
			this.agentConfigBuilder.conversationId(conversationId);
			return this;
		}

		/**
		 * Adds a skill directory path.
		 *
		 * @param skillPath
		 *            skill directory path
		 * @return this builder
		 */
		public Builder addSkillPath(String skillPath) {
			this.agentConfigBuilder.addSkillPath(skillPath);
			return this;
		}

		/**
		 * Sets the list of skill directory paths.
		 *
		 * @param skillsPaths
		 *            skill directory paths
		 * @return this builder
		 */
		public Builder skillsPaths(List<String> skillsPaths) {
			this.agentConfigBuilder.skillsPaths(skillsPaths);
			return this;
		}

		/**
		 * Adds an in-memory {@link InlineSkill}.
		 *
		 * @param inlineSkill
		 *            inline skill definition
		 * @return this builder
		 */
		public Builder addInlineSkill(InlineSkill inlineSkill) {
			this.agentConfigBuilder.addInlineSkill(inlineSkill);
			return this;
		}

		/**
		 * Sets the list of in-memory {@link InlineSkill} definitions.
		 *
		 * @param inlineSkills
		 *            inline skill definitions
		 * @return this builder
		 */
		public Builder inlineSkills(List<InlineSkill> inlineSkills) {
			this.agentConfigBuilder.inlineSkills(inlineSkills);
			return this;
		}

		/**
		 * Applies lightweight presets for local execution.
		 *
		 * @return this builder
		 */
		public Builder lightweight() {
			this.agentConfigBuilder.lightweight();
			return this;
		}

		/**
		 * Builds the LocalOpenAIAgentConfig.
		 *
		 * @return new LocalOpenAIAgentConfig instance
		 */
		public LocalOpenAIAgentConfig build() {
			if (baseUrl == null || baseUrl.isEmpty()) {
				throw new IllegalArgumentException("baseUrl must be specified for LocalOpenAIAgentConfig");
			}
			if (modelName == null || modelName.isEmpty()) {
				throw new IllegalArgumentException("modelName must be specified for LocalOpenAIAgentConfig");
			}
			return new LocalOpenAIAgentConfig(this);
		}
	}
}
