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

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import io.github.glaforge.antigravity.hooks.*;
import io.github.glaforge.antigravity.tools.SchemaGenerator;
import io.github.glaforge.antigravity.triggers.AgentTrigger;

/**
 * Configuration for the Agent.
 */
public class AgentConfig {
	/**
	 * Default lightweight model for image generation tasks.
	 */
	public static final String DEFAULT_IMAGE_GENERATION_MODEL = "gemini-3.1-flash-lite-image";

	private final String instructions;
	private final String modelName;
	private final List<Object> toolInstances;
	private final List<String> skillsPaths;
	private final CapabilitiesConfig capabilities;
	private final GenerationConfig generation;
	private final Path workspaceDir;
	private final List<AgentHook> hooks;
	private final List<AgentTrigger> triggers;
	private final String saveDir;
	private final String appDataDir;
	private final String conversationId;
	private final List<Policy> policies;
	private final String finishToolSchemaJson;
	private final List<McpServerConfig> mcpServers;
	private final Map<String, String> environmentVariables;
	private final String baseUrl;
	private final RetryConfig retryConfig;
	private final DebugConfig debugConfig;
	private final BudgetConfig budgetConfig;
	private final AgentBehavior agentBehavior;

	private AgentConfig(Builder builder) {
		this.instructions = builder.instructions;
		this.modelName = builder.modelName;
		this.toolInstances = new ArrayList<>(builder.toolInstances);
		this.skillsPaths = new ArrayList<>(builder.skillsPaths);
		this.capabilities = builder.capabilities != null ? builder.capabilities : CapabilitiesConfig.builder().build();
		this.generation = builder.generation;
		this.workspaceDir = builder.workspaceDir;
		this.hooks = new ArrayList<>(builder.hooks);
		this.triggers = new ArrayList<>(builder.triggers);
		this.saveDir = builder.saveDir;
		this.appDataDir = builder.appDataDir;
		this.conversationId = builder.conversationId;
		this.policies = new ArrayList<>(builder.policies);
		this.finishToolSchemaJson = builder.finishToolSchemaJson;
		this.mcpServers = new ArrayList<>(builder.mcpServers);
		this.environmentVariables = new HashMap<>(builder.environmentVariables);
		this.baseUrl = builder.baseUrl;
		this.retryConfig = builder.retryConfig;
		this.debugConfig = builder.debugConfig;
		this.budgetConfig = builder.budgetConfig;
		this.agentBehavior = builder.agentBehavior;
	}

	/**
	 * Returns the instructions.
	 *
	 * @return the instructions
	 */
	public String getInstructions() {
		return instructions;
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
	 * Returns the capabilities configuration.
	 *
	 * @return the capabilities configuration
	 */
	public CapabilitiesConfig getCapabilities() {
		return capabilities;
	}
	/**
	 * Returns the generation configuration.
	 *
	 * @return the generation configuration
	 */
	public GenerationConfig getGeneration() {
		return generation;
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
	 * Returns the triggers.
	 *
	 * @return the triggers
	 */
	public List<AgentTrigger> getTriggers() {
		return Collections.unmodifiableList(triggers);
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
	 * Returns the custom environment variables to pass to the agent process.
	 *
	 * @return custom environment variables
	 */
	public Map<String, String> getEnvironmentVariables() {
		return Collections.unmodifiableMap(environmentVariables);
	}

	/**
	 * Returns the base URL for local Gemma/OpenAI models.
	 *
	 * @return base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Returns the model API and output retry configuration.
	 *
	 * @return retry configuration, or null if not configured
	 */
	public RetryConfig getRetryConfig() {
		return retryConfig;
	}

	/**
	 * Returns the debug and observability configuration.
	 *
	 * @return debug configuration, or null if not configured
	 */
	public DebugConfig getDebugConfig() {
		return debugConfig;
	}

	/**
	 * Returns the session budget configuration.
	 *
	 * @return budget configuration, or null if not configured
	 */
	public BudgetConfig getBudgetConfig() {
		return budgetConfig;
	}

	/**
	 * Returns the agent behavior mode.
	 *
	 * @return agent behavior mode, or null if not configured
	 */
	public AgentBehavior getAgentBehavior() {
		return agentBehavior;
	}

	/**
	 * Hydrates the GCP/Vertex project ID from explicit config or standard
	 * GOOGLE_CLOUD_PROJECT environment variable.
	 *
	 * @param explicitProject
	 *            the explicitly specified project ID, or null
	 * @return the hydrated project ID
	 */
	public static String hydrateVertexProject(String explicitProject) {
		if (explicitProject != null && !explicitProject.isEmpty()) {
			return explicitProject;
		}
		return System.getenv("GOOGLE_CLOUD_PROJECT");
	}

	/**
	 * Hydrates the GCP/Vertex location from explicit config or standard
	 * GOOGLE_CLOUD_LOCATION / GOOGLE_CLOUD_REGION environment variables.
	 *
	 * @param explicitLocation
	 *            the explicitly specified location, or null
	 * @return the hydrated location
	 */
	public static String hydrateVertexLocation(String explicitLocation) {
		if (explicitLocation != null && !explicitLocation.isEmpty()) {
			return explicitLocation;
		}
		String loc = System.getenv("GOOGLE_CLOUD_LOCATION");
		if (loc != null && !loc.isEmpty()) {
			return loc;
		}
		return System.getenv("GOOGLE_CLOUD_REGION");
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
		private String instructions = "";
		private String modelName = "gemini-flash-latest";
		private List<Object> toolInstances = new ArrayList<>();
		private List<String> skillsPaths = new ArrayList<>();
		private CapabilitiesConfig capabilities = CapabilitiesConfig.builder().build();
		private GenerationConfig generation = null;
		private Path workspaceDir = Path.of(System.getProperty("user.dir"));
		private List<AgentHook> hooks = new ArrayList<>();
		private List<AgentTrigger> triggers = new ArrayList<>();
		private String saveDir = System.getProperty("java.io.tmpdir") + "/antigravity-java";
		private String appDataDir = null;
		private String conversationId = "";
		private List<Policy> policies = new ArrayList<>();
		private String finishToolSchemaJson = null;
		private List<McpServerConfig> mcpServers = new ArrayList<>();
		private Map<String, String> environmentVariables = new HashMap<>();
		private String baseUrl;
		private RetryConfig retryConfig;
		private DebugConfig debugConfig;
		private BudgetConfig budgetConfig;
		private AgentBehavior agentBehavior;

		/**
		 * Sets the instructions.
		 *
		 * @param instructions
		 *            the instructions
		 * @return this builder
		 */
		public Builder instructions(String instructions) {
			this.instructions = instructions;
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
		 * Sets the capabilities configuration.
		 *
		 * @param capabilities
		 *            the capabilities configuration
		 * @return this builder
		 */
		public Builder capabilities(CapabilitiesConfig capabilities) {
			this.capabilities = capabilities;
			return this;
		}

		/**
		 * Sets the generation configuration.
		 *
		 * @param generation
		 *            the generation configuration
		 * @return this builder
		 */
		public Builder generation(GenerationConfig generation) {
			this.generation = generation;
			return this;
		}

		/**
		 * Sets the base URL for local Gemma/OpenAI endpoints.
		 *
		 * @param baseUrl
		 *            the base URL
		 * @return this builder
		 */
		public Builder baseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

		/**
		 * Adds a trigger.
		 *
		 * @param trigger
		 *            the trigger
		 * @return this builder
		 */
		public Builder addTrigger(AgentTrigger trigger) {
			this.triggers.add(trigger);
			return this;
		}

		/**
		 * Adds a generic agent hook.
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
		 * Adds a pre-turn hook.
		 *
		 * @param hook
		 *            the pre-turn hook
		 * @return this builder
		 */
		public Builder addPreTurnHook(PreTurnHook hook) {
			this.hooks.add(hook);
			return this;
		}

		/**
		 * Adds a post-turn hook.
		 *
		 * @param hook
		 *            the post-turn hook
		 * @return this builder
		 */
		public Builder addPostTurnHook(PostTurnHook hook) {
			this.hooks.add(hook);
			return this;
		}

		/**
		 * Adds a pre-tool-call decision hook.
		 *
		 * @param hook
		 *            the pre-tool-call decide hook
		 * @return this builder
		 */
		public Builder addPreToolCallDecideHook(PreToolCallDecideHook hook) {
			this.hooks.add(hook);
			return this;
		}

		/**
		 * Adds a post-tool-call hook.
		 *
		 * @param hook
		 *            the post-tool-call hook
		 * @return this builder
		 */
		public Builder addPostToolCallHook(PostToolCallHook hook) {
			this.hooks.add(hook);
			return this;
		}

		/**
		 * Adds an on-tool-error hook.
		 *
		 * @param hook
		 *            the on-tool-error hook
		 * @return this builder
		 */
		public Builder addOnToolErrorHook(OnToolErrorHook hook) {
			this.hooks.add(hook);
			return this;
		}

		/**
		 * Adds an on-interaction hook.
		 *
		 * @param hook
		 *            the on-interaction hook
		 * @return this builder
		 */
		public Builder addOnInteractionHook(OnInteractionHook hook) {
			this.hooks.add(hook);
			return this;
		}

		/**
		 * Adds an on-session-start hook.
		 *
		 * @param hook
		 *            the on-session-start hook
		 * @return this builder
		 */
		public Builder addOnSessionStartHook(OnSessionStartHook hook) {
			this.hooks.add(hook);
			return this;
		}

		/**
		 * Adds an on-session-end hook.
		 *
		 * @param hook
		 *            the on-session-end hook
		 * @return this builder
		 */
		public Builder addOnSessionEndHook(OnSessionEndHook hook) {
			this.hooks.add(hook);
			return this;
		}

		/**
		 * Adds an on-compaction hook.
		 *
		 * @param hook
		 *            the on-compaction hook
		 * @return this builder
		 */
		public Builder addOnCompactionHook(OnCompactionHook hook) {
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
		 * Sets the finish tool schema JSON by auto-generating it from the given class.
		 *
		 * @param targetClass
		 *            the Java class to generate a JSON Schema from
		 * @return this builder
		 */
		public Builder finishToolSchema(Class<?> targetClass) {
			try {
				ObjectNode schemaNode = SchemaGenerator.generateSchema(targetClass);
				this.finishToolSchemaJson = SchemaGenerator.getMapper().writeValueAsString(schemaNode);
			} catch (Exception e) {
				throw new RuntimeException("Failed to generate JSON schema for class: " + targetClass.getName(), e);
			}
			return this;
		}

		/**
		 * Sets the finish tool schema JSON string directly.
		 *
		 * @param finishToolSchemaJson
		 *            the JSON schema string
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
		 * Sets custom environment variables for the agent process.
		 *
		 * @param environmentVariables
		 *            map of key-value environment variables
		 * @return this builder
		 */
		public Builder environmentVariables(Map<String, String> environmentVariables) {
			if (environmentVariables != null) {
				this.environmentVariables = new HashMap<>(environmentVariables);
			}
			return this;
		}

		/**
		 * Adds a single custom environment variable for the agent process.
		 *
		 * @param key
		 *            environment variable key
		 * @param value
		 *            environment variable value
		 * @return this builder
		 */
		public Builder addEnvironmentVariable(String key, String value) {
			this.environmentVariables.put(key, value);
			return this;
		}

		/**
		 * Sets the retry configuration for transient model API errors and output
		 * retries.
		 *
		 * @param retryConfig
		 *            the retry configuration
		 * @return this builder
		 */
		public Builder retryConfig(RetryConfig retryConfig) {
			this.retryConfig = retryConfig;
			return this;
		}

		/**
		 * Sets the debug configuration for client logging and server-side tracing.
		 *
		 * @param debugConfig
		 *            the debug configuration
		 * @return this builder
		 */
		public Builder debugConfig(DebugConfig debugConfig) {
			this.debugConfig = debugConfig;
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
			this.budgetConfig = budgetConfig;
			return this;
		}

		/**
		 * Sets the agent behavior mode (e.g. AUTONOMOUS or INTERACTIVE).
		 *
		 * @param agentBehavior
		 *            the agent behavior mode
		 * @return this builder
		 */
		public Builder agentBehavior(AgentBehavior agentBehavior) {
			this.agentBehavior = agentBehavior;
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
