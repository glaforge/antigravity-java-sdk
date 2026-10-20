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
import java.util.Collection;
import io.github.glaforge.antigravity.hooks.*;
import io.github.glaforge.antigravity.tools.SchemaGenerator;
import io.github.glaforge.antigravity.triggers.AgentTrigger;

/**
 * Configuration for the Agent.
 */
public class AgentConfig {
	/**
	 * Default model for new agents.
	 */
	public static final String DEFAULT_MODEL_NAME = "gemini-3.8-flash";

	/**
	 * Default lightweight model for image generation tasks.
	 */
	public static final String DEFAULT_IMAGE_GENERATION_MODEL = "gemini-3.1-flash-lite-image";

	private final String instructions;
	private final String modelName;
	private final List<Object> toolInstances;
	private final List<String> skillsPaths;
	private final List<InlineSkill> inlineSkills;
	private final SkillsConfig skillsConfig;
	private final CapabilitiesConfig capabilities;
	private final GenerationConfig generation;
	private final Path workspaceDir;
	private final List<String> workspaces;
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
	private final WorkspaceContainment workspaceContainment;
	private final CompactionConfig compactionConfig;
	private final List<SubagentConfig> subagents;
	private final SessionContinuationMode sessionContinuationMode;
	private final String apiKey;
	private final Boolean vertex;
	private final String project;
	private final String location;

	private AgentConfig(Builder builder) {
		this.instructions = builder.instructions;
		this.modelName = builder.modelName;
		this.toolInstances = new ArrayList<>(builder.toolInstances);
		List<String> allSkillsPaths = new ArrayList<>(builder.skillsPaths);
		List<InlineSkill> allInlineSkills = new ArrayList<>(builder.inlineSkills);
		if (builder.skillsConfig != null) {
			for (SkillSource src : builder.skillsConfig.skills()) {
				if (src.directoryPath() != null && !allSkillsPaths.contains(src.directoryPath())) {
					allSkillsPaths.add(src.directoryPath());
				}
				if (src.inlineSkill() != null && !allInlineSkills.contains(src.inlineSkill())) {
					allInlineSkills.add(src.inlineSkill());
				}
			}
		}
		if (!allSkillsPaths.isEmpty() && !allInlineSkills.isEmpty()) {
			throw new IllegalArgumentException(
					"combining inline_skills and skills_paths is not supported by LocalHarness; provide either inline_skills or skills_paths, not both.");
		}
		this.skillsPaths = allSkillsPaths;
		this.inlineSkills = allInlineSkills;
		if (builder.skillsConfig != null) {
			this.skillsConfig = builder.skillsConfig;
		} else if (!allInlineSkills.isEmpty()) {
			this.skillsConfig = SkillsConfig.ofInlineSkills(allInlineSkills);
		} else if (!allSkillsPaths.isEmpty()) {
			this.skillsConfig = SkillsConfig.builder()
					.skills(allSkillsPaths.stream().map(SkillSource::fromDirectory).toList()).build();
		} else {
			this.skillsConfig = null;
		}
		this.capabilities = builder.capabilities != null ? builder.capabilities : CapabilitiesConfig.builder().build();
		this.generation = builder.generation;
		if (!builder.workspaces.isEmpty()) {
			this.workspaces = new ArrayList<>(builder.workspaces);
			this.workspaceDir = builder.workspaceDirSet ? builder.workspaceDir : Path.of(this.workspaces.get(0));
		} else if (!builder.workspacesExplicitlySet && builder.workspaceDir != null) {
			this.workspaceDir = builder.workspaceDir;
			this.workspaces = new ArrayList<>(List.of(builder.workspaceDir.toAbsolutePath().normalize().toString()));
		} else {
			this.workspaceDir = builder.workspaceDir;
			this.workspaces = new ArrayList<>(builder.workspaces);
		}
		this.hooks = new ArrayList<>(builder.hooks);
		this.triggers = new ArrayList<>(builder.triggers);
		this.saveDir = builder.saveDir;
		if (builder.appDataDir != null && !builder.appDataDir.isEmpty() && !Path.of(builder.appDataDir).isAbsolute()) {
			throw new IllegalArgumentException("appDataDir must be an absolute path, got '" + builder.appDataDir + "'");
		}
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
		this.workspaceContainment = builder.workspaceContainment;
		this.compactionConfig = builder.compactionConfig;
		this.subagents = new ArrayList<>(builder.subagents);
		this.sessionContinuationMode = builder.sessionContinuationMode;
		this.apiKey = builder.apiKey;
		if (builder.vertex != null) {
			this.vertex = builder.vertex;
		} else if (isTruthyEnv("GOOGLE_GENAI_USE_VERTEXAI", this.environmentVariables)
				|| isTruthyEnv("GOOGLE_GENAI_USE_ENTERPRISE", this.environmentVariables)) {
			this.vertex = Boolean.TRUE;
		} else {
			this.vertex = null;
		}
		boolean hydrateAmbientVertex = Boolean.TRUE.equals(this.vertex)
				&& (this.baseUrl == null || this.baseUrl.isEmpty()) && (this.apiKey == null || this.apiKey.isEmpty());
		this.project = hydrateAmbientVertex
				? hydrateVertexProject(builder.project, this.environmentVariables)
				: builder.project;
		this.location = hydrateAmbientVertex
				? hydrateVertexLocation(builder.location, this.environmentVariables)
				: builder.location;
		if (this.sessionContinuationMode == SessionContinuationMode.RESUME
				&& (this.conversationId == null || this.conversationId.isBlank())) {
			throw new IllegalArgumentException(
					"conversationId must be specified when sessionContinuationMode is RESUME");
		}
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
	 * Returns the in-memory skill definitions.
	 *
	 * @return unmodifiable list of inline skills
	 */
	public List<InlineSkill> getInlineSkills() {
		return Collections.unmodifiableList(inlineSkills);
	}
	/**
	 * Returns the Agent Skills configuration.
	 *
	 * @return the skills configuration, or null if not configured
	 */
	public SkillsConfig getSkillsConfig() {
		return skillsConfig;
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
	 * Returns the workspace directories.
	 *
	 * @return list of workspace directory paths
	 */
	public List<String> getWorkspaces() {
		return Collections.unmodifiableList(workspaces);
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
	 * Returns the response schema JSON.
	 *
	 * @return the response schema JSON
	 */
	public String getResponseSchema() {
		return finishToolSchemaJson;
	}

	/**
	 * Returns the finish tool schema JSON.
	 *
	 * @return the finish tool schema JSON
	 * @deprecated Use {@link #getResponseSchema()} instead.
	 */
	@Deprecated
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
	 * Returns the workspace containment policy.
	 *
	 * @return workspace containment policy, or null if not configured
	 */
	public WorkspaceContainment getWorkspaceContainment() {
		return workspaceContainment;
	}

	/**
	 * Returns the conversation compaction configuration.
	 *
	 * @return compaction configuration, or null if not configured
	 */
	public CompactionConfig getCompactionConfig() {
		return compactionConfig;
	}

	/**
	 * Returns the list of configured custom subagents.
	 *
	 * @return unmodifiable list of custom subagents
	 */
	public List<SubagentConfig> getSubagents() {
		return Collections.unmodifiableList(subagents);
	}

	/**
	 * Returns the session continuation mode.
	 *
	 * @return session continuation mode, or null if not configured
	 */
	public SessionContinuationMode getSessionContinuationMode() {
		return sessionContinuationMode;
	}

	/**
	 * Returns the explicitly configured API key, if any.
	 *
	 * @return the API key, or null if not explicitly configured
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * Returns whether the Vertex AI backend is enabled.
	 *
	 * @return true if Vertex AI is enabled, false if explicitly disabled, or null
	 *         if unspecified
	 */
	public Boolean getVertex() {
		return vertex;
	}

	/**
	 * Returns true if the Vertex AI backend is enabled.
	 *
	 * @return true if Vertex AI is enabled
	 */
	public boolean isVertex() {
		return Boolean.TRUE.equals(vertex);
	}

	/**
	 * Returns the configured or hydrated GCP project ID for Vertex AI.
	 *
	 * @return the GCP project ID, or null
	 */
	public String getProject() {
		return project;
	}

	/**
	 * Returns the configured or hydrated GCP location/region for Vertex AI.
	 *
	 * @return the GCP location, or null
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Creates a new AgentConfig with standardized evaluation presets applied.
	 *
	 * @return a new AgentConfig configured for evaluations and benchmarks
	 */
	public static AgentConfig eval() {
		return builder().eval().build();
	}

	private static boolean isTruthyEnv(String key, Map<String, String> customEnv) {
		String val = customEnv != null ? customEnv.get(key) : null;
		if (val == null) {
			val = System.getProperty(key);
		}
		if (val == null) {
			val = System.getenv(key);
		}
		if (val == null) {
			return false;
		}
		String normalized = val.trim().toLowerCase();
		return "true".equals(normalized) || "1".equals(normalized);
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
		return hydrateVertexProject(explicitProject, null);
	}

	static String hydrateVertexProject(String explicitProject, Map<String, String> customEnv) {
		if (explicitProject != null && !explicitProject.isEmpty()) {
			return explicitProject;
		}
		if (customEnv != null) {
			String envProj = customEnv.get("GOOGLE_CLOUD_PROJECT");
			if (envProj != null && !envProj.isEmpty()) {
				return envProj;
			}
		}
		String propProj = System.getProperty("GOOGLE_CLOUD_PROJECT");
		if (propProj != null && !propProj.isEmpty()) {
			return propProj;
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
		return hydrateVertexLocation(explicitLocation, null);
	}

	static String hydrateVertexLocation(String explicitLocation, Map<String, String> customEnv) {
		if (explicitLocation != null && !explicitLocation.isEmpty()) {
			return explicitLocation;
		}
		if (customEnv != null) {
			String envLoc = customEnv.get("GOOGLE_CLOUD_LOCATION");
			if (envLoc != null && !envLoc.isEmpty()) {
				return envLoc;
			}
			String envReg = customEnv.get("GOOGLE_CLOUD_REGION");
			if (envReg != null && !envReg.isEmpty()) {
				return envReg;
			}
		}
		String propLoc = System.getProperty("GOOGLE_CLOUD_LOCATION");
		if (propLoc != null && !propLoc.isEmpty()) {
			return propLoc;
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
		private String modelName = DEFAULT_MODEL_NAME;
		private List<Object> toolInstances = new ArrayList<>();
		private List<String> skillsPaths = new ArrayList<>();
		private List<InlineSkill> inlineSkills = new ArrayList<>();
		private SkillsConfig skillsConfig;
		private CapabilitiesConfig capabilities = CapabilitiesConfig.builder().build();
		private GenerationConfig generation = null;
		private Path workspaceDir = Path.of(System.getProperty("user.dir"));
		private boolean workspaceDirSet = false;
		private List<String> workspaces = new ArrayList<>();
		private boolean workspacesExplicitlySet = false;
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
		private DebugConfig debugConfig = DebugConfig.defaults();
		private BudgetConfig budgetConfig;
		private AgentBehavior agentBehavior;
		private WorkspaceContainment workspaceContainment;
		private CompactionConfig compactionConfig;
		private List<SubagentConfig> subagents = new ArrayList<>();
		private SessionContinuationMode sessionContinuationMode;
		private String apiKey;
		private Boolean vertex;
		private String project;
		private String location;

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
		 * Sets the list of skill paths.
		 *
		 * @param skillsPaths
		 *            list of skill directory paths
		 * @return this builder
		 */
		public Builder skillsPaths(List<String> skillsPaths) {
			this.skillsPaths.clear();
			if (skillsPaths != null) {
				this.skillsPaths.addAll(skillsPaths);
			}
			return this;
		}

		/**
		 * Adds an in-memory {@link InlineSkill}.
		 *
		 * @param inlineSkill
		 *            the inline skill definition
		 * @return this builder
		 */
		public Builder addInlineSkill(InlineSkill inlineSkill) {
			if (inlineSkill != null) {
				this.inlineSkills.add(inlineSkill);
			}
			return this;
		}

		/**
		 * Sets the list of in-memory {@link InlineSkill} definitions.
		 *
		 * @param inlineSkills
		 *            list of inline skill definitions
		 * @return this builder
		 */
		public Builder inlineSkills(List<InlineSkill> inlineSkills) {
			this.inlineSkills.clear();
			if (inlineSkills != null) {
				this.inlineSkills.addAll(inlineSkills);
			}
			return this;
		}

		/**
		 * Sets the Agent Skills configuration.
		 *
		 * @param skillsConfig
		 *            the skills configuration
		 * @return this builder
		 */
		public Builder skillsConfig(SkillsConfig skillsConfig) {
			this.skillsConfig = skillsConfig;
			return this;
		}

		/**
		 * Sets the list of skill sources.
		 *
		 * @param skills
		 *            the list of skill sources
		 * @return this builder
		 */
		public Builder skills(List<SkillSource> skills) {
			this.skillsConfig = SkillsConfig.of(skills);
			return this;
		}

		/**
		 * Adds a skill source.
		 *
		 * @param skill
		 *            the skill source
		 * @return this builder
		 */
		public Builder addSkill(SkillSource skill) {
			if (this.skillsConfig == null) {
				this.skillsConfig = SkillsConfig.builder().addSkill(skill).build();
			} else {
				List<SkillSource> list = new ArrayList<>(this.skillsConfig.skills());
				list.add(skill);
				this.skillsConfig = new SkillsConfig(this.skillsConfig.enabled(), list);
			}
			return this;
		}

		/**
		 * Adds a skill directory path.
		 *
		 * @param path
		 *            the path to the skill directory
		 * @return this builder
		 */
		public Builder addSkillPath(Path path) {
			return addSkill(SkillSource.fromPath(path));
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
		 * Sets the collection of hooks, replacing any previously registered hooks.
		 *
		 * @param hooks
		 *            collection of hooks
		 * @return this builder
		 */
		public Builder hooks(Collection<? extends AgentHook> hooks) {
			this.hooks.clear();
			if (hooks != null) {
				this.hooks.addAll(hooks);
			}
			return this;
		}

		/**
		 * Sets the hooks, replacing any previously registered hooks.
		 *
		 * @param hooks
		 *            varargs of hooks
		 * @return this builder
		 */
		public Builder hooks(AgentHook... hooks) {
			this.hooks.clear();
			if (hooks != null) {
				for (AgentHook hook : hooks) {
					if (hook != null) {
						this.hooks.add(hook);
					}
				}
			}
			return this;
		}

		/**
		 * Adds a collection of hooks to the agent configuration.
		 *
		 * @param hooks
		 *            collection of hooks to add
		 * @return this builder
		 */
		public Builder addHooks(Collection<? extends AgentHook> hooks) {
			if (hooks != null) {
				this.hooks.addAll(hooks);
			}
			return this;
		}

		/**
		 * Adds multiple hooks to the agent configuration.
		 *
		 * @param hooks
		 *            varargs of hooks to add
		 * @return this builder
		 */
		public Builder addHooks(AgentHook... hooks) {
			if (hooks != null) {
				for (AgentHook hook : hooks) {
					if (hook != null) {
						this.hooks.add(hook);
					}
				}
			}
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
		 * Adds an on-stop hook triggered when agent execution is stopped.
		 *
		 * @param hook
		 *            the on-stop hook
		 * @return this builder
		 */
		public Builder addOnStopHook(OnStopHook hook) {
			this.hooks.add(hook);
			return this;
		}

		/**
		 * Configures the agent with minimal lightweight defaults optimized for smaller,
		 * local-running models (e.g., LiteRT / Gemma). Sets
		 * {@link AgentBehavior#MINIMAL}, disables subagents, and configures a minimal
		 * tool set.
		 *
		 * @return this builder
		 */
		public Builder lightweight() {
			this.agentBehavior = AgentBehavior.MINIMAL;
			this.capabilities = CapabilitiesConfig.builder().enableSubagents(false).allowUserQuestions(false)
					.enableWebSearch(false).enableUrlReading(false).enableShell(false).enableViewFile(true)
					.enableWriteFile(false).enableFileEdit(false).enableListDir(true).enableGrepSearch(true).build();
			return this;
		}

		/**
		 * Configures the agent with standardized evaluation presets intended to
		 * represent Gemini's core coding ability on benchmarks and evaluation suites.
		 *
		 * Disables image generation, disables subagents, enables daemon commands in
		 * {@link RunCommandConfig}, sets {@link Policies#allowAll()} for autonomous
		 * tool execution, and sets resilient API retry behavior via
		 * {@link RetryConfig#benchmark()}.
		 *
		 * @return this builder
		 */
		public Builder eval() {
			RunCommandConfig runCmd = RunCommandConfig.builder().enableDaemons(true).build();
			this.capabilities = CapabilitiesConfig.builder().enableGenerateImage(false).enableSubagents(false)
					.runCommandConfig(runCmd).build();
			this.policies.clear();
			this.policies.add(Policies.allowAll());
			this.retryConfig = RetryConfig.benchmark();
			return this;
		}

		/**
		 * Adds a custom subagent configuration.
		 *
		 * @param subagent
		 *            the subagent configuration
		 * @return this builder
		 */
		public Builder addSubagent(SubagentConfig subagent) {
			if (subagent != null) {
				this.subagents.add(subagent);
			}
			return this;
		}

		/**
		 * Sets the list of custom subagents.
		 *
		 * @param subagents
		 *            list of subagents
		 * @return this builder
		 */
		public Builder subagents(List<SubagentConfig> subagents) {
			this.subagents.clear();
			if (subagents != null) {
				this.subagents.addAll(subagents);
			}
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
		 * Sets security policies.
		 *
		 * @param policies
		 *            list of policies
		 * @return this builder
		 */
		public Builder policies(List<Policy> policies) {
			this.policies = new ArrayList<>(policies);
			return this;
		}

		/**
		 * Sets the primary workspace directory.
		 *
		 * @param workspaceDir
		 *            the primary workspace directory path
		 * @return this builder
		 */
		public Builder workspaceDir(Path workspaceDir) {
			if (workspaceDir != null) {
				this.workspaceDir = workspaceDir;
				this.workspaceDirSet = true;
			}
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
			this.workspacesExplicitlySet = true;
			this.workspaces = workspaces != null ? new ArrayList<>(workspaces) : new ArrayList<>();
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
			this.workspacesExplicitlySet = true;
			this.workspaces.add(workspace);
			return this;
		}

		/**
		 * Sets the response schema by auto-generating JSON Schema from the given class.
		 *
		 * @param targetClass
		 *            the Java class to generate a JSON Schema from
		 * @return this builder
		 */
		public Builder responseSchema(Class<?> targetClass) {
			this.finishToolSchemaJson = SchemaGenerator.generateSchemaJson(targetClass);
			return this;
		}

		/**
		 * Sets the response schema JSON string directly.
		 *
		 * @param responseSchema
		 *            the JSON schema string
		 * @return this builder
		 */
		public Builder responseSchema(String responseSchema) {
			this.finishToolSchemaJson = responseSchema;
			return this;
		}

		/**
		 * Sets the finish tool schema JSON by auto-generating it from the given class.
		 *
		 * @param targetClass
		 *            the Java class to generate a JSON Schema from
		 * @return this builder
		 * @deprecated Use {@link #responseSchema(Class)} instead.
		 */
		@Deprecated
		public Builder finishToolSchema(Class<?> targetClass) {
			return responseSchema(targetClass);
		}

		/**
		 * Sets the finish tool schema JSON string directly.
		 *
		 * @param finishToolSchemaJson
		 *            the JSON schema string
		 * @return this builder
		 * @deprecated Use {@link #responseSchema(String)} instead.
		 */
		@Deprecated
		public Builder finishToolSchemaJson(String finishToolSchemaJson) {
			return responseSchema(finishToolSchemaJson);
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
		 * Sets the workspace containment policy.
		 *
		 * @param workspaceContainment
		 *            the workspace containment policy
		 * @return this builder
		 */
		public Builder workspaceContainment(WorkspaceContainment workspaceContainment) {
			this.workspaceContainment = workspaceContainment;
			return this;
		}

		/**
		 * Sets the conversation trajectory compaction configuration.
		 *
		 * @param compactionConfig
		 *            the compaction configuration
		 * @return this builder
		 */
		public Builder compactionConfig(CompactionConfig compactionConfig) {
			this.compactionConfig = compactionConfig;
			return this;
		}

		/**
		 * Sets the conversation compaction threshold in tokens.
		 *
		 * @param tokenThreshold
		 *            token threshold limit before compaction
		 * @return this builder
		 */
		public Builder compactionThreshold(int tokenThreshold) {
			this.compactionConfig = CompactionConfig.of(tokenThreshold);
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
			this.sessionContinuationMode = sessionContinuationMode;
			return this;
		}

		/**
		 * Sets the explicit API key for Gemini or Vertex AI Express Mode.
		 *
		 * @param apiKey
		 *            the API key string
		 * @return this builder
		 */
		public Builder apiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		/**
		 * Enables or disables the Vertex AI backend.
		 *
		 * @param vertex
		 *            true to use Vertex AI, false to use the Gemini Developer API
		 * @return this builder
		 */
		public Builder vertex(Boolean vertex) {
			this.vertex = vertex;
			return this;
		}

		/**
		 * Sets the GCP project ID for Vertex AI.
		 *
		 * @param project
		 *            the GCP project ID
		 * @return this builder
		 */
		public Builder project(String project) {
			this.project = project;
			return this;
		}

		/**
		 * Sets the GCP location/region for Vertex AI (e.g. {@code "us-central1"}).
		 *
		 * @param location
		 *            the GCP location
		 * @return this builder
		 */
		public Builder location(String location) {
			this.location = location;
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
