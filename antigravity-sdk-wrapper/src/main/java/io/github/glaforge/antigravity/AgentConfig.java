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

	public String getPersona() {
		return persona;
	}
	public String getModelName() {
		return modelName;
	}
	public List<Object> getToolInstances() {
		return Collections.unmodifiableList(toolInstances);
	}
	public List<String> getSkillsPaths() {
		return Collections.unmodifiableList(skillsPaths);
	}
	public boolean isEnableSubagents() {
		return enableSubagents;
	}
	public boolean isAllowUserQuestions() {
		return allowUserQuestions;
	}
	public Path getWorkspaceDir() {
		return workspaceDir;
	}
	public List<AgentHook> getHooks() {
		return Collections.unmodifiableList(hooks);
	}
	public String getSaveDir() {
		return saveDir;
	}
	public String getAppDataDir() {
		return appDataDir;
	}
	public String getConversationId() {
		return conversationId;
	}
	public List<Policy> getPolicies() {
		return Collections.unmodifiableList(policies);
	}
	public String getFinishToolSchemaJson() {
		return finishToolSchemaJson;
	}
	public List<McpServerConfig> getMcpServers() {
		return Collections.unmodifiableList(mcpServers);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
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

		public Builder persona(String persona) {
			this.persona = persona;
			return this;
		}

		public Builder modelName(String modelName) {
			this.modelName = modelName;
			return this;
		}

		public Builder addTool(Object toolInstance) {
			this.toolInstances.add(toolInstance);
			return this;
		}

		public Builder addSkillPath(String skillPath) {
			this.skillsPaths.add(skillPath);
			return this;
		}

		public Builder enableSubagents(boolean enableSubagents) {
			this.enableSubagents = enableSubagents;
			return this;
		}

		public Builder allowUserQuestions(boolean allowUserQuestions) {
			this.allowUserQuestions = allowUserQuestions;
			return this;
		}

		public Builder addHook(AgentHook hook) {
			this.hooks.add(hook);
			return this;
		}

		public Builder saveDir(String saveDir) {
			this.saveDir = saveDir;
			return this;
		}

		public Builder appDataDir(String appDataDir) {
			this.appDataDir = appDataDir;
			return this;
		}

		public Builder conversationId(String conversationId) {
			this.conversationId = conversationId;
			return this;
		}

		public Builder addPolicy(Policy policy) {
			this.policies.add(policy);
			return this;
		}

		public Builder finishToolSchemaJson(String finishToolSchemaJson) {
			this.finishToolSchemaJson = finishToolSchemaJson;
			return this;
		}

		public Builder addMcpServer(McpServerConfig mcpServerConfig) {
			this.mcpServers.add(mcpServerConfig);
			return this;
		}

		public AgentConfig build() {
			return new AgentConfig(this);
		}
	}
}
