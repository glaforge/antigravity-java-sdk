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
 * Configuration helper for executing local Gemma models via the LiteRT backend.
 */
public class LiteRTAgentConfig {
	/**
	 * Hardware backend acceleration options for LiteRT.
	 */
	public enum Backend {
		/** CPU execution backend. */
		CPU("cpu"),
		/** GPU acceleration backend (Metal/CUDA/Vulkan). */
		GPU("gpu"),
		/** NPU hardware acceleration backend. */
		NPU("npu");

		private final String value;

		Backend(String value) {
			this.value = value;
		}

		/**
		 * Returns the string value for the backend.
		 *
		 * @return backend string
		 */
		public String getValue() {
			return value;
		}
	}

	private final String modelPath;
	private final Backend backend;
	private final int port;
	private final String baseUrl;
	private final AgentConfig agentConfig;

	private LiteRTAgentConfig(Builder builder) {
		this.modelPath = builder.modelPath;
		this.backend = builder.backend;
		this.port = builder.port;
		int effectivePort = builder.port > 0 ? builder.port : 9379;
		this.baseUrl = (builder.baseUrl != null && !builder.baseUrl.isEmpty())
				? builder.baseUrl
				: "http://127.0.0.1:" + effectivePort + "/v1";
		this.agentConfig = builder.agentConfigBuilder
				.modelName(
						builder.modelName != null && !builder.modelName.isEmpty() ? builder.modelName : "gemma-local")
				.baseUrl(this.baseUrl).build();
	}

	/**
	 * Returns the local model file path.
	 *
	 * @return model path
	 */
	public String getModelPath() {
		return modelPath;
	}

	/**
	 * Returns the hardware backend.
	 *
	 * @return backend
	 */
	public Backend getBackend() {
		return backend;
	}

	/**
	 * Returns the local server port.
	 *
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns the base URL of the local LiteRT OpenAI-compatible endpoint.
	 *
	 * @return base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
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
	 * Creates a new Builder for LiteRTAgentConfig.
	 *
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for LiteRTAgentConfig.
	 */
	public static class Builder {
		private String modelPath = "";
		private Backend backend = Backend.GPU;
		private int port = 0;
		private String baseUrl = "";
		private String modelName = "gemma-local";
		private final AgentConfig.Builder agentConfigBuilder = AgentConfig.builder();

		/**
		 * Default constructor applying lightweight presets.
		 */
		public Builder() {
			this.agentConfigBuilder.lightweight();
		}

		/**
		 * Sets the local model file path.
		 *
		 * @param modelPath
		 *            path to the model
		 * @return this builder
		 */
		public Builder modelPath(String modelPath) {
			this.modelPath = modelPath;
			return this;
		}

		/**
		 * Sets the hardware acceleration backend.
		 *
		 * @param backend
		 *            backend option
		 * @return this builder
		 */
		public Builder backend(Backend backend) {
			this.backend = backend;
			return this;
		}

		/**
		 * Sets the local server port.
		 *
		 * @param port
		 *            local port number
		 * @return this builder
		 */
		public Builder port(int port) {
			this.port = port;
			return this;
		}

		/**
		 * Sets the base URL of the LiteRT server endpoint.
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
		 * Sets system instructions.
		 *
		 * @param instructions
		 *            prompt instructions
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
		 *            tool instance object
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
		 * Adds an MCP server config.
		 *
		 * @param mcpServerConfig
		 *            MCP server configuration
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
		 * Applies lightweight presets for local execution.
		 *
		 * @return this builder
		 */
		public Builder lightweight() {
			this.agentConfigBuilder.lightweight();
			return this;
		}

		/**
		 * Builds the LiteRTAgentConfig.
		 *
		 * @return new LiteRTAgentConfig instance
		 */
		public LiteRTAgentConfig build() {
			if (modelPath == null || modelPath.isEmpty()) {
				throw new IllegalArgumentException("modelPath must be specified for LiteRTAgentConfig");
			}
			return new LiteRTAgentConfig(this);
		}
	}
}
