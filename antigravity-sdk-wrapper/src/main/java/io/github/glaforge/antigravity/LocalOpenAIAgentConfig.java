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
