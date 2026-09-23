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

import io.github.glaforge.antigravity.triggers.AgentTrigger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.glaforge.antigravity.localharness.*;
import static io.github.glaforge.antigravity.localharness.AgentBehavior.AGENT_BEHAVIOR_AUTONOMOUS;
import static io.github.glaforge.antigravity.localharness.AgentBehavior.AGENT_BEHAVIOR_INTERACTIVE;
import static io.github.glaforge.antigravity.localharness.AgentBehavior.AGENT_BEHAVIOR_MINIMAL;
import static io.github.glaforge.antigravity.localharness.WorkspaceContainment.WORKSPACE_CONTAINMENT_ENABLED;
import static io.github.glaforge.antigravity.localharness.WorkspaceContainment.WORKSPACE_CONTAINMENT_DISABLED;
import static io.github.glaforge.antigravity.localharness.WorkspaceContainment.WORKSPACE_CONTAINMENT_UNSPECIFIED;
import static io.github.glaforge.antigravity.localharness.BudgetConfig.BudgetScope.BUDGET_SCOPE_LIFETIME;
import static io.github.glaforge.antigravity.localharness.BudgetConfig.BudgetScope.BUDGET_SCOPE_FORWARD_LOOKING;
import io.github.glaforge.antigravity.hooks.*;
import io.github.glaforge.antigravity.hooks.ToolCall;
import io.github.glaforge.antigravity.tools.ToolRegistry;
import io.github.glaforge.antigravity.tools.ToolDefinition;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.SubmissionPublisher;

import java.util.function.Consumer;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The main agent class that manages the lifecycle and interaction with the
 * local harness.
 */
public class Agent implements AutoCloseable, TriggerContext {
	private static final Logger log = LoggerFactory.getLogger(Agent.class);
	private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer().omittingInsignificantWhitespace();

	private final Process goProcess;
	private volatile WebSocket webSocket;
	private final ToolRegistry toolRegistry = new ToolRegistry();
	private final ExecutorService toolExecutor = Executors.newVirtualThreadPerTaskExecutor();
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private String conversationId;

	private CompletableFuture<AgentResponse> currentChatFuture;
	private Consumer<AgentResponseChunk> currentChunkConsumer;
	private volatile SubmissionPublisher<String> currentThoughtsPublisher;
	private volatile SubmissionPublisher<ToolCall> currentToolCallsPublisher;
	private volatile boolean clientCancelled = false;
	private StringBuilder currentText;
	private StringBuilder currentThoughts;
	private UsageMetadata currentUsage;
	private UsageMetadata cumulativeUsage = new UsageMetadata(0, 0, 0, 0, 0);
	private UsageMetadata turnStartUsage;
	private final ConcurrentMap<String, UsageMetadata> trajectoryUsages = new ConcurrentHashMap<>();
	private final List<Policy> policies;
	private boolean hasStructuredOutput;
	private StringBuilder wsBuffer = new StringBuilder();
	private final ConcurrentMap<String, Object> toolState = new ConcurrentHashMap<>();
	private final Set<String> handledQuestionRequests = ConcurrentHashMap.newKeySet();
	private final Set<String> handledToolConfirmations = ConcurrentHashMap.newKeySet();
	private final SessionContext sessionContext = new SessionContext();
	private volatile SandboxStatus sandboxStatus;

	/**
	 * Returns the OS command sandbox status reported at handshake, if any.
	 *
	 * @return the sandbox status, or null if uninitialized
	 */
	public SandboxStatus getSandboxStatus() {
		return sandboxStatus;
	}

	/**
	 * Returns the usage metadata from the most recent turn.
	 *
	 * @return the usage metadata
	 */
	public UsageMetadata getUsageMetadata() {
		if (currentUsage != null) {
			return currentUsage;
		}
		if (cumulativeUsage != null && turnStartUsage != null) {
			UsageMetadata diff = cumulativeUsage.subtract(turnStartUsage);
			if (diff.totalTokenCount() > 0) {
				return diff;
			}
		}
		return cumulativeUsage;
	}

	/**
	 * Returns the cumulative token usage across all turns in this session.
	 *
	 * @return the total cumulative usage metadata
	 */
	public UsageMetadata getTotalUsage() {
		return cumulativeUsage;
	}

	/**
	 * Returns a map of trajectory ID to cumulative token usage for subagents and
	 * main agent.
	 *
	 * @return an unmodifiable map of trajectory usage metadata
	 */
	public Map<String, UsageMetadata> getTrajectoryUsages() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(trajectoryUsages));
	}

	/**
	 * Returns the unique ID of the conversation.
	 *
	 * @return the conversation ID
	 */
	public String getConversationId() {
		return conversationId;
	}

	@Override
	/**
	 * Fires a trigger with the specified text to interrupt the agent and supply new
	 * information.
	 *
	 * @param triggerText
	 *            the text content of the trigger
	 */
	public void fireTrigger(String triggerText) {
		try {
			InputEvent event = InputEvent.newBuilder().setAutomatedTrigger(triggerText).build();
			String payload = JSON_PRINTER.print(event);
			sendWebSocketMessage(payload);
		} catch (Exception e) {
			log.error("Failed to fire trigger", e);
		}
	}

	/**
	 * Cancels the current agent execution.
	 */
	public void cancel() {
		try {
			this.clientCancelled = true;
			InputEvent event = InputEvent.newBuilder().setHaltRequest(true).build();
			String payload = JSON_PRINTER.print(event);
			sendWebSocketMessage(payload);
		} catch (Exception e) {
			log.error("Failed to cancel agent execution", e);
		}
	}

	private Policy.Decision evaluatePolicies(String toolName, JsonNode arguments) {
		for (Policy policy : policies) {
			Policy.Decision d = policy.evaluate(toolName, arguments);
			if (d == Policy.Decision.DENY)
				return Policy.Decision.DENY;
			if (d == Policy.Decision.ALLOW)
				return Policy.Decision.ALLOW;
		}
		return Policy.Decision.PASS;
	}

	private final AgentConfig config;

	/**
	 * Creates a new builder for the Agent.
	 *
	 * @return a new Agent.Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for Agent. delegates to AgentConfig.Builder internally.
	 */
	public static class Builder {
		/**
		 * Default constructor.
		 */
		public Builder() {
		}
		private final AgentConfig.Builder configBuilder = AgentConfig.builder();

		/**
		 * Sets the instructions of the agent.
		 *
		 * @param instructions
		 *            the system instructions
		 * @return this builder
		 */
		public Builder instructions(String instructions) {
			configBuilder.instructions(instructions);
			return this;
		}

		/**
		 * Sets the model name.
		 *
		 * @param modelName
		 *            the model to use
		 * @return this builder
		 */
		public Builder modelName(String modelName) {
			configBuilder.modelName(modelName);
			return this;
		}

		/**
		 * Adds a tool to the agent.
		 *
		 * @param toolInstance
		 *            the tool instance
		 * @return this builder
		 */
		public Builder addTool(Object toolInstance) {
			configBuilder.addTool(toolInstance);
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
			configBuilder.addSkillPath(skillPath);
			return this;
		}

		/**
		 * Sets capabilities.
		 *
		 * @param capabilities
		 *            the capabilities config
		 * @return this builder
		 */
		public Builder capabilities(CapabilitiesConfig capabilities) {
			configBuilder.capabilities(capabilities);
			return this;
		}

		/**
		 * Sets generation config.
		 *
		 * @param generation
		 *            the generation config
		 * @return this builder
		 */
		public Builder generation(GenerationConfig generation) {
			configBuilder.generation(generation);
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
			configBuilder.addHook(hook);
			return this;
		}

		/**
		 * Sets the save directory.
		 *
		 * @param saveDir
		 *            the directory to save
		 * @return this builder
		 */
		public Builder saveDir(String saveDir) {
			configBuilder.saveDir(saveDir);
			return this;
		}

		/**
		 * Sets app data directory.
		 *
		 * @param appDataDir
		 *            the dir
		 * @return this builder
		 */
		public Builder appDataDir(String appDataDir) {
			configBuilder.appDataDir(appDataDir);
			return this;
		}

		/**
		 * Sets conversation ID.
		 *
		 * @param conversationId
		 *            the ID
		 * @return this builder
		 */
		public Builder conversationId(String conversationId) {
			configBuilder.conversationId(conversationId);
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
			configBuilder.addPolicy(policy);
			return this;
		}

		/**
		 * Sets finish tool schema JSON.
		 *
		 * @param finishToolSchemaJson
		 *            the JSON schema
		 * @return this builder
		 */
		public Builder finishToolSchemaJson(String finishToolSchemaJson) {
			configBuilder.finishToolSchemaJson(finishToolSchemaJson);
			return this;
		}

		/**
		 * Adds an MCP server config.
		 *
		 * @param mcpServerConfig
		 *            the config
		 * @return this builder
		 */
		public Builder addMcpServer(McpServerConfig mcpServerConfig) {
			configBuilder.addMcpServer(mcpServerConfig);
			return this;
		}

		/**
		 * Sets the budget configuration for session limits.
		 *
		 * @param budgetConfig
		 *            the budget configuration
		 * @return this builder
		 */
		public Builder budgetConfig(BudgetConfig budgetConfig) {
			configBuilder.budgetConfig(budgetConfig);
			return this;
		}

		/**
		 * Sets the agent behavior mode (AUTONOMOUS or INTERACTIVE).
		 *
		 * @param agentBehavior
		 *            the agent behavior
		 * @return this builder
		 */
		public Builder agentBehavior(AgentBehavior agentBehavior) {
			configBuilder.agentBehavior(agentBehavior);
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
			configBuilder.workspaceContainment(workspaceContainment);
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
			configBuilder.compactionConfig(compactionConfig);
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
			configBuilder.compactionThreshold(tokenThreshold);
			return this;
		}

		/**
		 * Builds the Agent.
		 *
		 * @return the configured Agent
		 * @throws Exception
		 *             if an error occurs
		 */
		public Agent build() throws Exception {
			return new Agent(configBuilder.build());
		}
	}

	/**
	 * Constructs a new Agent with the specified configuration.
	 *
	 * @param config
	 *            the configuration for the agent
	 * @throws Exception
	 *             if an error occurs during initialization
	 */
	public Agent(AgentConfig config) throws Exception {
		this(config, true);
	}

	Agent(AgentConfig config, boolean startProcess) throws Exception {
		this.config = config;
		this.policies = config.getPolicies();
		for (Object tool : config.getToolInstances()) {
			this.registerTools(tool);
		}

		if (!startProcess) {
			this.goProcess = null;
			return;
		}

		// 1. Resolve and extract (or reuse cached) localharness binary
		File binaryFile = PlatformResolver.resolveBinary();

		// 2. Spawn process
		ProcessBuilder pb = new ProcessBuilder(binaryFile.getAbsolutePath())
				.redirectError(ProcessBuilder.Redirect.PIPE);
		if (config.getEnvironmentVariables() != null && !config.getEnvironmentVariables().isEmpty()) {
			pb.environment().putAll(config.getEnvironmentVariables());
		}
		String resolvedApiKey = resolveGeminiApiKey();
		if (resolvedApiKey != null && !resolvedApiKey.isEmpty()) {
			pb.environment().putIfAbsent("GEMINI_API_KEY", resolvedApiKey);
		}
		this.goProcess = pb.start();

		try {
			// Handshake Outbound
			String storageDir = config.getSaveDir();
			if (storageDir == null || storageDir.isEmpty()) {
				storageDir = System.getProperty("java.io.tmpdir");
				if (!storageDir.endsWith("/"))
					storageDir += "/";
				storageDir += "antigravity-java";
			}
			new File(storageDir).mkdirs();

			InputConfig inputConfig = InputConfig.newBuilder().setStorageDirectory(storageDir).build();

			byte[] serializedInput = inputConfig.toByteArray();
			ByteBuffer lengthBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
					.putInt(serializedInput.length);

			OutputStream os = this.goProcess.getOutputStream();
			os.write(lengthBuffer.array());
			os.write(serializedInput);
			os.flush();

			// Handshake Inbound
			InputStream is = this.goProcess.getInputStream();
			byte[] inLengthBytes = new byte[4];
			int readBytes = is.read(inLengthBytes);
			if (readBytes < 4) {
				String err = new String(this.goProcess.getErrorStream().readAllBytes());
				throw new IllegalStateException("Failed to read length from stdout. Error: " + err);
			}

			int inLength = ByteBuffer.wrap(inLengthBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
			byte[] outConfigBytes = new byte[inLength];
			int totalRead = 0;
			while (totalRead < inLength) {
				int r = is.read(outConfigBytes, totalRead, inLength - totalRead);
				if (r < 0)
					break;
				totalRead += r;
			}

			OutputConfig outputConfig = OutputConfig.parseFrom(outConfigBytes);

			// Upgrade to WebSocket
			int runtimePort = outputConfig.getPort();
			String securityToken = outputConfig.getApiKey();

			String apiKey = resolvedApiKey != null ? resolvedApiKey : "placeholder";

			Thread stdoutConsumer = new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						log.debug("[localharness stdout] {}", line);
					}
				} catch (Exception e) {
				}
			}, "antigravity-harness-stdout");
			stdoutConsumer.setDaemon(true);
			stdoutConsumer.start();

			Thread stderrConsumer = new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(this.goProcess.getErrorStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.contains("ERROR: logging before google.Init: I") || line.contains("[CDP Discovery]")
								|| line.contains("permissions: skipping check")) {
							log.debug("[localharness] {}", line);
						} else if (line.contains("ERROR: logging before google.Init: W")) {
							log.warn("[localharness] {}", line);
						} else if (line.contains("ERROR: logging before google.Init: E")
								|| line.contains("ERROR: logging before google.Init: F")) {
							log.error("[localharness] {}", line);
						} else {
							log.debug("[localharness stderr] {}", line);
						}
					}
				} catch (Exception e) {
				}
			}, "antigravity-harness-stderr");
			stderrConsumer.setDaemon(true);
			stderrConsumer.start();

			HarnessConfig.Builder configBuilder = HarnessConfig.newBuilder().setCascadeId(config.getConversationId())
					.setAppDataDir(config.getAppDataDir() != null ? config.getAppDataDir() : "");

			ModelConfig.Builder modelConfigBuilder = ModelConfig.newBuilder()
					.setName(config.getModelName() != null ? config.getModelName() : "gemma-local")
					.addTypes(ModelType.MODEL_TYPE_TEXT);

			if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
				modelConfigBuilder.setGemmaEndpoint(GemmaEndpoint.newBuilder().setBaseUrl(config.getBaseUrl()).build());
			} else {
				GeminiAPIEndpoint.Builder apiEndpointBuilder = GeminiAPIEndpoint.newBuilder().setApiKey(apiKey);
				if (config.getGeneration() != null) {
					GeminiModelOptions.Builder optionsBuilder = GeminiModelOptions.newBuilder();
					if (config.getGeneration().thinkingLevel() != null) {
						optionsBuilder.setThinkingLevel(config.getGeneration().thinkingLevel().getValue());
					}
					if (config.getGeneration().serviceTier() != null) {
						optionsBuilder.setServiceTier(config.getGeneration().serviceTier().getValue());
					}
					apiEndpointBuilder.setOptions(optionsBuilder.build());
				}
				modelConfigBuilder.setGeminiApiEndpoint(apiEndpointBuilder.build());
			}
			configBuilder.addModels(modelConfigBuilder.build());

			configBuilder.setSystemInstructions(SystemInstructions.newBuilder()
					.setAppended(
							AppendedSystemInstructions.newBuilder().setCustomIdentity(config.getInstructions()).build())
					.build());

			int mcpIndex = 1;
			for (McpServerConfig mcp : config.getMcpServers()) {
				var mcpBuilder = configBuilder.addMcpServersBuilder().setName("server-" + (mcpIndex++));

				if (mcp instanceof McpServerConfig.StdioMcpServerConfig stdio) {
					mcpBuilder.setStdio(McpStdioTransport.newBuilder().setCommand(stdio.command())
							.addAllArgs(stdio.args()).build());
				} else if (mcp instanceof McpServerConfig.SseMcpServerConfig sse) {
					McpHttpTransport.Builder http = McpHttpTransport.newBuilder().setUrl(sse.url());
					if (sse.headers() != null) {
						http.putAllHeaders(sse.headers());
					}
					mcpBuilder.setHttp(http.build());
				} else if (mcp instanceof McpServerConfig.HttpMcpServerConfig httpConfig) {
					McpHttpTransport.Builder http = McpHttpTransport.newBuilder().setUrl(httpConfig.url());
					if (httpConfig.headers() != null) {
						http.putAllHeaders(httpConfig.headers());
					}
					mcpBuilder.setHttp(http.build());
				}
			}

			// Add Hooks tracking
			for (AgentHook hook : config.getHooks()) {
				if (hook instanceof PreTurnHook)
					configBuilder.addEnabledHooks(LifecycleHook.LIFECYCLE_HOOK_PRE_TURN);
				if (hook instanceof PostTurnHook)
					configBuilder.addEnabledHooks(LifecycleHook.LIFECYCLE_HOOK_POST_TURN);
				if (hook instanceof PreToolCallDecideHook)
					configBuilder.addEnabledHooks(LifecycleHook.LIFECYCLE_HOOK_PRE_TOOL);
				if (hook instanceof PostToolCallHook)
					configBuilder.addEnabledHooks(LifecycleHook.LIFECYCLE_HOOK_POST_TOOL);
				if (hook instanceof OnToolErrorHook)
					configBuilder.addEnabledHooks(LifecycleHook.LIFECYCLE_HOOK_ON_TOOL_ERROR);
				if (hook instanceof OnCompactionHook)
					configBuilder.addEnabledHooks(LifecycleHook.LIFECYCLE_HOOK_ON_COMPACTION);
				if (hook instanceof OnStopHook)
					configBuilder.addEnabledHooks(LifecycleHook.LIFECYCLE_HOOK_STOP);
				if (hook instanceof OnInteractionHook)
					configBuilder.addEnabledHooks(LifecycleHook.LIFECYCLE_HOOK_UNSPECIFIED); // Will
																								// use
																								// manually
																								// for
																								// now
			}

			for (ToolDefinition toolDef : toolRegistry.getToolDefinitions()) {
				configBuilder.addTools(toolDef.toProtobuf());
			}
			configBuilder.addAllSkillsPaths(config.getSkillsPaths());

			if (config.getFinishToolSchemaJson() != null) {
				configBuilder.setFinishToolSchemaJson(config.getFinishToolSchemaJson());
			}

			if (config.getRetryConfig() != null) {
				var retryBuilder = configBuilder.getRetryConfigBuilder();
				if (config.getRetryConfig().apiRetry() != null) {
					retryBuilder.setApiRetry(config.getRetryConfig().apiRetry().toProtobuf());
				}
				if (config.getRetryConfig().modelOutputRetry() != null) {
					retryBuilder.setModelOutputRetry(config.getRetryConfig().modelOutputRetry().toProtobuf());
				}
			}

			if (config.getBudgetConfig() != null) {
				BudgetConfig b = config.getBudgetConfig();
				var budgetBuilder = configBuilder.getBudgetConfigBuilder();
				if (b.maxModelCalls() != null) {
					budgetBuilder.setMaxModelCalls(b.maxModelCalls());
				}
				if (b.maxToolCalls() != null) {
					budgetBuilder.setMaxToolCalls(b.maxToolCalls());
				}
				if (b.maxInputTokens() != null) {
					budgetBuilder.setMaxInputTokens(b.maxInputTokens());
				}
				if (b.maxOutputTokens() != null) {
					budgetBuilder.setMaxOutputTokens(b.maxOutputTokens());
				}
				if (b.maxTotalTokens() != null) {
					budgetBuilder.setMaxTotalTokens(b.maxTotalTokens());
				}
				if (b.scope() != null) {
					switch (b.scope()) {
						case LIFETIME -> budgetBuilder.setScope(BUDGET_SCOPE_LIFETIME);
						case FORWARD_LOOKING -> budgetBuilder.setScope(BUDGET_SCOPE_FORWARD_LOOKING);
					}
				}
			}

			if (config.getCompactionConfig() != null) {
				CompactionConfig cc = config.getCompactionConfig();
				var compactionBuilder = configBuilder.getCompactionConfigBuilder();
				if (cc.tokenThreshold() != null) {
					compactionBuilder.setTokenThreshold(cc.tokenThreshold());
					configBuilder.setCompactionThreshold(cc.tokenThreshold());
				}
				if (cc.checkpointIntervalTokens() != null) {
					compactionBuilder.setCheckpointIntervalTokens(cc.checkpointIntervalTokens());
				}
				if (cc.maxContextTokens() != null) {
					compactionBuilder.setMaxContextTokens(cc.maxContextTokens());
				}
			}

			if (config.getAgentBehavior() != null) {
				switch (config.getAgentBehavior()) {
					case AUTONOMOUS -> configBuilder.setAgentBehavior(AGENT_BEHAVIOR_AUTONOMOUS);
					case INTERACTIVE -> configBuilder.setAgentBehavior(AGENT_BEHAVIOR_INTERACTIVE);
					case MINIMAL -> configBuilder.setAgentBehavior(AGENT_BEHAVIOR_MINIMAL);
				}
			}

			if (config.getWorkspaceContainment() != null) {
				var policyBuilder = configBuilder.getPolicyConfigBuilder();
				switch (config.getWorkspaceContainment()) {
					case ENABLED -> policyBuilder.setWorkspaceContainment(WORKSPACE_CONTAINMENT_ENABLED);
					case DISABLED -> policyBuilder.setWorkspaceContainment(WORKSPACE_CONTAINMENT_DISABLED);
					case UNSPECIFIED -> policyBuilder.setWorkspaceContainment(WORKSPACE_CONTAINMENT_UNSPECIFIED);
				}
			}

			if (config.getCapabilities().enableSubagents() || config.getCapabilities().allowUserQuestions()
					|| config.getCapabilities().enableWebSearch() || config.getCapabilities().enableUrlReading()
					|| config.getCapabilities().enableShell() || config.getCapabilities().enableViewFile()
					|| config.getCapabilities().enableWriteFile() || config.getCapabilities().enableFileEdit()
					|| config.getCapabilities().enableListDir() || config.getCapabilities().enableGrepSearch()) {
				HarnessSideTools.Builder capBuilder = HarnessSideTools.newBuilder();
				if (config.getCapabilities().enableSubagents()) {
					capBuilder.setSubagents(SubagentsConfig.newBuilder().setEnabled(true).build());
				}
				if (config.getCapabilities().allowUserQuestions()) {
					capBuilder.setUserQuestions(UserQuestionsConfig.newBuilder().setEnabled(true).build());
				}
				if (config.getCapabilities().enableWebSearch()) {
					capBuilder.setSearchWeb(SearchWebToolConfig.newBuilder().setEnabled(true).build());
				}
				if (config.getCapabilities().enableUrlReading()) {
					capBuilder.setReadUrlContent(ReadUrlContentToolConfig.newBuilder().setEnabled(true).build());
				}
				if (config.getCapabilities().enableShell()) {
					var runCmdBuilder = RunCommandToolConfig.newBuilder().setEnabled(true);
					if (config.getCapabilities().runCommandConfig() != null) {
						RunCommandConfig rcc = config.getCapabilities().runCommandConfig();
						runCmdBuilder.setEnableDaemonCommands(rcc.enableDaemons());
						runCmdBuilder.setEnableSandbox(rcc.enableSandbox());
						if (rcc.timeoutSeconds() != null) {
							runCmdBuilder.setMaxTimeoutMs((int) (rcc.timeoutSeconds() * 1000));
						}
					}
					capBuilder.setRunCommand(runCmdBuilder.build());
				}
				if (config.getCapabilities().enableViewFile()) {
					capBuilder.setViewFile(ViewFileToolConfig.newBuilder().setEnabled(true).build());
				}
				if (config.getCapabilities().enableWriteFile()) {
					capBuilder.setWriteToFile(WriteToFileToolConfig.newBuilder().setEnabled(true).build());
				}
				if (config.getCapabilities().enableFileEdit()) {
					capBuilder.setFileEdit(FileEditToolConfig.newBuilder().setEnabled(true).build());
				}
				if (config.getCapabilities().enableListDir()) {
					capBuilder.setListDir(ListDirToolConfig.newBuilder().setEnabled(true).build());
				}
				if (config.getCapabilities().enableGrepSearch()) {
					capBuilder.setGrepSearch(GrepSearchToolConfig.newBuilder().setEnabled(true).build());
				}
				if (config.getCapabilities().enableGenerateImage()) {
					capBuilder.setGenerateImage(GenerateImageToolConfig.newBuilder().setEnabled(true).build());
				}
				configBuilder.setHarnessSideTools(capBuilder.build());
			}

			HarnessConfig protoConfig = configBuilder.build();

			InitializeConversationEvent initEvent = InitializeConversationEvent.newBuilder().setConfig(protoConfig)
					.build();

			String initEventJson = JSON_PRINTER.print(initEvent);

			HttpClient client = HttpClient.newHttpClient();
			WebSocket.Listener wsListener = new WebSocket.Listener() {
				@Override
				public void onOpen(WebSocket webSocket) {
					webSocket.sendText(initEventJson, true).join();
					WebSocket.Listener.super.onOpen(webSocket);
				}

				@Override
				public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
					wsBuffer.append(data);
					if (last) {
						handleIncomingMessage(webSocket, wsBuffer.toString());
						wsBuffer.setLength(0);
					}
					webSocket.request(1);
					return null;
				}

				@Override
				public void onError(WebSocket webSocket, Throwable error) {
					wsBuffer.setLength(0);
					if (currentChatFuture != null && !currentChatFuture.isDone()) {
						currentChatFuture.completeExceptionally(error);
					}
					WebSocket.Listener.super.onError(webSocket, error);
				}

				@Override
				public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
					wsBuffer.setLength(0);
					if (currentChatFuture != null && !currentChatFuture.isDone()) {
						currentChatFuture.completeExceptionally(
								new IllegalStateException("WebSocket closed unexpectedly: " + reason));
					}
					WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
					return null;
				}
			};

			String[] targetHosts = new String[]{"localhost", "127.0.0.1"};
			WebSocket connectedWs = null;
			Throwable lastWsErr = null;

			for (String host : targetHosts) {
				try {
					connectedWs = client.newWebSocketBuilder().header("x-goog-api-key", securityToken)
							.buildAsync(URI.create("ws://" + host + ":" + runtimePort), wsListener)
							.get(5, TimeUnit.SECONDS);
					break;
				} catch (Exception e) {
					lastWsErr = e;
				}
			}

			if (connectedWs == null) {
				throw new IllegalStateException("Failed to connect to localharness WebSocket", lastWsErr);
			}
			this.webSocket = connectedWs;
		} catch (Exception e) {
			if (goProcess.isAlive())
				goProcess.destroyForcibly();
			throw e;
		}
		triggerSessionStart().join();
	}

	/**
	 * Registers all tools defined in the given service instance. Tools are
	 * discovered by scanning the instance for methods annotated with
	 * {@literal @}AntigravityTool.
	 *
	 * @param serviceInstance
	 *            the instance containing tool methods
	 */
	public void registerTools(Object serviceInstance) {
		toolRegistry.registerToolsFromObject(serviceInstance);
	}

	/**
	 * Sends a chat message and waits for the full response.
	 *
	 * @param prompt
	 *            the text prompt to send
	 * @return a CompletableFuture containing the response
	 */
	public CompletableFuture<AgentResponse> chat(String prompt) {
		return chatStream(List.of(AgentInput.Text.of(prompt)), null);
	}

	/**
	 * Sends multiple inputs to the agent and waits for the final response.
	 *
	 * @param inputs
	 *            the inputs to send
	 * @return a CompletableFuture containing the AgentResponse
	 */
	public CompletableFuture<AgentResponse> chat(AgentInput... inputs) {
		return chatStream(List.of(inputs), null);
	}

	/**
	 * Sends a structured chat message with multiple inputs and waits for the full
	 * response.
	 *
	 * @param prompt
	 *            the list of inputs to send
	 * @return a CompletableFuture containing the response
	 */
	public CompletableFuture<AgentResponse> chat(List<AgentInput> prompt) {
		return chatStream(prompt, null);
	}

	/**
	 * Sends multiple inputs to the agent and returns an AgentStream containing
	 * distinct publishers for chunks, thoughts, and tool calls.
	 *
	 * @param inputs
	 *            the list of inputs
	 * @return an AgentStream
	 */
	public AgentStream streamChat(List<AgentInput> inputs) {
		SubmissionPublisher<AgentResponseChunk> chunksPublisher = new SubmissionPublisher<>();
		this.currentThoughtsPublisher = new SubmissionPublisher<>();
		this.currentToolCallsPublisher = new SubmissionPublisher<>();

		CompletableFuture<AgentResponse> result = chatStream(inputs, chunksPublisher::submit)
				.whenComplete((response, error) -> {
					if (error != null) {
						chunksPublisher.closeExceptionally(error);
					} else {
						chunksPublisher.close();
					}
				});

		return new AgentStream(chunksPublisher, currentThoughtsPublisher, currentToolCallsPublisher, result);
	}

	/**
	 * Sends a text prompt to the agent and returns an AgentStream for monitoring
	 * chunks, thoughts, and tool calls.
	 *
	 * @param text
	 *            prompt text
	 * @return AgentStream instance
	 */
	public AgentStream streamChat(String text) {
		return streamChat(List.of(AgentInput.Text.of(text)));
	}

	/**
	 * Sends a text prompt and returns a Flow.Publisher emitting response chunks.
	 *
	 * @param prompt
	 *            prompt text
	 * @return Flow.Publisher emitting AgentResponseChunk items
	 */
	public Publisher<AgentResponseChunk> chatPublisher(String prompt) {
		return chatPublisher(List.of(AgentInput.Text.of(prompt)));
	}

	/**
	 * Sends multiple inputs to the agent and returns a Publisher of response
	 * chunks.
	 *
	 * @param inputs
	 *            the inputs to send
	 * @return a Flow.Publisher emitting AgentResponseChunk items
	 */
	public Publisher<AgentResponseChunk> chatPublisher(AgentInput... inputs) {
		return chatPublisher(List.of(inputs));
	}

	/**
	 * Sends a list of inputs to the agent and returns a Publisher of response
	 * chunks.
	 *
	 * @param inputs
	 *            the list of inputs
	 * @return a Flow.Publisher emitting AgentResponseChunk items
	 */
	public Publisher<AgentResponseChunk> chatPublisher(List<AgentInput> inputs) {
		SubmissionPublisher<AgentResponseChunk> publisher = new SubmissionPublisher<>();
		chatStream(inputs, publisher::submit).whenComplete((response, error) -> {
			if (error != null) {
				publisher.closeExceptionally(error);
			} else {
				publisher.close();
			}
		});
		return publisher;
	}

	/**
	 * Sends a text prompt to the agent and streams response chunks to a consumer
	 * callback.
	 *
	 * @param text
	 *            prompt text
	 * @param onChunk
	 *            consumer callback for response chunks
	 * @return CompletableFuture resolving to the final AgentResponse
	 */
	public CompletableFuture<AgentResponse> chatStream(String text, Consumer<AgentResponseChunk> onChunk) {
		return chatStream(List.of(AgentInput.Text.of(text)), onChunk);
	}

	/**
	 * Sends a list of inputs to the agent and streams the response chunks.
	 *
	 * @param inputs
	 *            the list of inputs
	 * @param onChunk
	 *            a consumer to handle the incoming chunks
	 * @return a CompletableFuture containing the final AgentResponse
	 */
	public CompletableFuture<AgentResponse> chatStream(List<AgentInput> inputs, Consumer<AgentResponseChunk> onChunk) {
		if (this.currentChatFuture != null && !this.currentChatFuture.isDone()) {
			throw new IllegalStateException("An interaction is already in progress.");
		}

		this.clientCancelled = false;
		this.currentChatFuture = new CompletableFuture<>();
		this.currentChunkConsumer = onChunk;
		this.currentText = new StringBuilder();
		this.currentThoughts = new StringBuilder();
		this.hasStructuredOutput = false;
		this.turnStartUsage = this.cumulativeUsage;
		this.currentUsage = null;

		StringBuilder combinedText = new StringBuilder();
		for (AgentInput input : inputs) {
			if (input instanceof AgentInput.Text t) {
				combinedText.append(t.text()).append("\n");
			}
		}
		String combinedPrompt = combinedText.toString().trim();

		try {
			UserInput.Builder userInputBuilder = UserInput.newBuilder();
			for (AgentInput input : inputs) {
				if (input instanceof AgentInput.Text t) {
					userInputBuilder.addParts(UserInput.Part.newBuilder().setText(t.text()).build());
				} else if (input instanceof AgentInput.Media m) {
					UserInput.Media.Builder mediaBuilder = UserInput.Media.newBuilder().setMimeType(m.mimeType())
							.setData(ByteString.copyFrom(m.data()));
					if (m.description() != null) {
						mediaBuilder.setDescription(m.description());
					}
					userInputBuilder.addParts(UserInput.Part.newBuilder().setMedia(mediaBuilder.build()).build());
				} else if (input instanceof AgentInput.SlashCommand s) {
					userInputBuilder.addParts(UserInput.Part.newBuilder()
							.setSlashCommand(UserInput.SlashCommand.newBuilder().setName(s.name()).build()).build());
				}
			}

			InputEvent event = InputEvent.newBuilder().setUserInput(userInputBuilder.build()).build();
			String payload = JSON_PRINTER.print(event);
			sendWebSocketMessage(payload);
		} catch (Exception e) {
			this.currentChatFuture.completeExceptionally(e);
		}
		return currentChatFuture;
	}

	private CompletableFuture<Void> triggerSessionStart() {
		CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof OnSessionStartHook ssh) {
				future = future.thenCompose(v -> ssh.onSessionStart());
			}
		}

		future = future.thenRun(() -> {
			for (AgentTrigger trigger : config.getTriggers()) {
				trigger.start(this);
			}
		});
		return future;
	}

	private CompletableFuture<Void> triggerSessionEnd() {
		CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof OnSessionEndHook seh) {
				future = future.thenCompose(v -> seh.onSessionEnd());
			}
		}
		return future;
	}

	private CompletableFuture<HookResult> triggerPreTurn(String prompt) {
		CompletableFuture<HookResult> future = CompletableFuture.completedFuture(HookResult.allowed());
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof PreTurnHook pth) {
				future = future.thenCompose(res -> {
					if (!res.allow())
						return CompletableFuture.completedFuture(res);
					return pth.onPreTurn(prompt, sessionContext);
				});
			}
		}
		return future;
	}

	private CompletableFuture<Void> triggerPostTurn(String response) {
		CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof PostTurnHook pth) {
				future = future.thenCompose(v -> pth.onPostTurn(response, sessionContext));
			}
		}
		return future;
	}

	private CompletableFuture<HookResult> triggerPreToolCallDecide(ToolCall call) {
		CompletableFuture<HookResult> future = CompletableFuture.completedFuture(HookResult.allowed());
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof PreToolCallDecideHook ptcd) {
				future = future.thenCompose(res -> {
					if (!res.allow())
						return CompletableFuture.completedFuture(res);
					return ptcd.onPreToolCallDecide(call, sessionContext);
				});
			}
		}
		return future;
	}

	private CompletableFuture<Void> triggerPostToolCall(ToolCall call, Object result) {
		CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof PostToolCallHook pth) {
				future = future.thenCompose(v -> pth.onPostToolCall(call, result, sessionContext));
			}
		}
		return future;
	}

	private CompletableFuture<Object> triggerOnToolError(ToolCall call, Throwable err) {
		CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof OnToolErrorHook oteh) {
				future = future.thenCompose(recovery -> {
					if (recovery != null)
						return CompletableFuture.completedFuture(recovery);
					return oteh.onToolError(call, err, sessionContext);
				});
			}
		}
		return future;
	}

	private CompletableFuture<Void> triggerOnCompaction(Object stepData) {
		CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof OnCompactionHook och) {
				future = future.thenCompose(v -> och.onCompaction(stepData, sessionContext));
			}
		}
		return future;
	}

	private CompletableFuture<Void> triggerOnStop(StopArgs stopArgs) {
		CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof OnStopHook osh) {
				future = future.thenCompose(v -> osh.onStop(stopArgs, sessionContext));
			}
		}
		return future;
	}

	private void warnIfSandboxUnavailable(SandboxStatus status) {
		if (config.getCapabilities() != null && config.getCapabilities().enableShell()
				&& config.getCapabilities().runCommandConfig() != null
				&& config.getCapabilities().runCommandConfig().enableSandbox()) {
			if (status != null && !status.available()) {
				log.warn(
						"enable_sandbox=True but the OS sandbox is unavailable in this harness environment ({}); run_command will execute UNSANDBOXED.",
						status.unavailableReason() != null ? status.unavailableReason() : "reason unknown");
			}
		}
	}

	void handleIncomingMessage(WebSocket webSocket, String message) {
		try {
			JsonNode payload = jsonMapper.readTree(message);

			if (payload.has("initializeConversationResponse") || payload.has("initialize_conversation_response")) {
				JsonNode initResp = payload.has("initializeConversationResponse")
						? payload.get("initializeConversationResponse")
						: payload.get("initialize_conversation_response");

				if (initResp.has("cascadeId")) {
					this.conversationId = initResp.get("cascadeId").asText();
				} else if (initResp.has("cascade_id")) {
					this.conversationId = initResp.get("cascade_id").asText();
				}

				if (initResp.has("sandboxStatus") || initResp.has("sandbox_status")) {
					JsonNode sbNode = initResp.has("sandboxStatus")
							? initResp.get("sandboxStatus")
							: initResp.get("sandbox_status");
					boolean available = sbNode.path("available").asBoolean(false);
					String reason = sbNode.has("unavailableReason")
							? sbNode.get("unavailableReason").asText()
							: (sbNode.has("unavailable_reason") ? sbNode.get("unavailable_reason").asText() : null);
					this.sandboxStatus = new SandboxStatus(available, reason);
					warnIfSandboxUnavailable(this.sandboxStatus);
				}

				if (initResp.has("cumulativeUsage") || initResp.has("cumulative_usage")) {
					JsonNode cumNode = initResp.has("cumulativeUsage")
							? initResp.get("cumulativeUsage")
							: initResp.get("cumulative_usage");
					UsageMetadata parsed = parseUsageMetadata(cumNode);
					if (parsed != null) {
						this.cumulativeUsage = parsed;
						this.turnStartUsage = parsed;
					}
				}

				JsonNode trajUsageNode = initResp.has("trajectoryUsage")
						? initResp.get("trajectoryUsage")
						: initResp.get("trajectory_usage");
				if (trajUsageNode != null && trajUsageNode.isArray()) {
					for (JsonNode entry : trajUsageNode) {
						String trajId = entry.has("trajectoryId")
								? entry.get("trajectoryId").asText()
								: entry.path("trajectory_id").asText();
						JsonNode uNode = entry.has("usage") ? entry.get("usage") : null;
						if (trajId != null && !trajId.isEmpty() && uNode != null) {
							UsageMetadata parsed = parseUsageMetadata(uNode);
							if (parsed != null) {
								trajectoryUsages.put(trajId, parsed);
							}
						}
					}
				}
			}

			if (payload.has("usageUpdate") || payload.has("usage_update")) {
				JsonNode usageUpdate = payload.has("usageUpdate")
						? payload.get("usageUpdate")
						: payload.get("usage_update");

				if (usageUpdate.has("total")) {
					UsageMetadata newTotal = parseUsageMetadata(usageUpdate.get("total"));
					if (newTotal != null) {
						this.cumulativeUsage = newTotal;
						this.currentUsage = (turnStartUsage != null) ? newTotal.subtract(turnStartUsage) : newTotal;
					}
				}

				JsonNode agentsNode = usageUpdate.has("agents") ? usageUpdate.get("agents") : null;
				if (agentsNode != null && agentsNode.isArray()) {
					for (JsonNode entry : agentsNode) {
						String trajId = entry.has("trajectoryId")
								? entry.get("trajectoryId").asText()
								: entry.path("trajectory_id").asText();
						JsonNode uNode = entry.has("usage") ? entry.get("usage") : null;
						if (trajId != null && !trajId.isEmpty() && uNode != null) {
							UsageMetadata parsed = parseUsageMetadata(uNode);
							if (parsed != null) {
								trajectoryUsages.put(trajId, parsed);
							}
						}
					}
				}
			}

			if (payload.has("usageMetadata") || payload.has("usage_metadata")) {
				JsonNode topUsage = payload.has("usageMetadata")
						? payload.get("usageMetadata")
						: payload.get("usage_metadata");
				UsageMetadata parsed = parseUsageMetadata(topUsage);
				if (parsed != null) {
					this.cumulativeUsage = parsed;
					this.currentUsage = (turnStartUsage != null) ? parsed.subtract(turnStartUsage) : parsed;
				}
			}

			if (payload.has("stepUpdate")) {
				JsonNode stepUpdate = payload.get("stepUpdate");

				if (stepUpdate.has("cascadeId")) {
					this.conversationId = stepUpdate.get("cascadeId").asText();
				}

				if (stepUpdate.has("finish")) {
					String outputString = stepUpdate.get("finish").path("outputString").asText("");
					if (!outputString.isEmpty()) {
						hasStructuredOutput = true;
						if (currentText != null) {
							currentText.setLength(0);
							currentText.append(outputString);
						}
					}
				}

				if (stepUpdate.has("textDelta") || stepUpdate.has("thinkingDelta")) {
					String source = stepUpdate.path("source").asText("");
					if (!"SOURCE_USER".equals(source) && !"SOURCE_SYSTEM".equals(source)) {
						String target = stepUpdate.path("target").asText("");
						String textDelta = stepUpdate.path("textDelta").asText("");
						String thinkingDelta = stepUpdate.path("thinkingDelta").asText("");

						if ("TARGET_ENVIRONMENT".equals(target)) {
							if (currentThoughts != null && !textDelta.isEmpty()) {
								if (currentThoughts.length() > 0 && !currentThoughts.toString().endsWith("\n")) {
									currentThoughts.append("\n");
								}
								currentThoughts.append(textDelta);
							}
							if (currentThoughtsPublisher != null && !textDelta.isEmpty()) {
								currentThoughtsPublisher.submit(textDelta);
							}
							if (currentChunkConsumer != null && !textDelta.isEmpty()) {
								currentChunkConsumer.accept(new AgentResponseChunk("", textDelta));
							}
						} else {
							if (currentText != null && !hasStructuredOutput)
								currentText.append(textDelta);

							if (currentChunkConsumer != null && (!textDelta.isEmpty() || !thinkingDelta.isEmpty())) {
								currentChunkConsumer.accept(new AgentResponseChunk(textDelta, thinkingDelta));
							}
						}

						if (currentThoughts != null && !thinkingDelta.isEmpty()) {
							currentThoughts.append(thinkingDelta);
						}
						if (currentThoughtsPublisher != null && !thinkingDelta.isEmpty()) {
							currentThoughtsPublisher.submit(thinkingDelta);
						}
					}
				}

				if (stepUpdate.has("usageMetadata") || stepUpdate.has("usage_metadata")) {
					JsonNode usage = stepUpdate.has("usageMetadata")
							? stepUpdate.get("usageMetadata")
							: stepUpdate.get("usage_metadata");
					UsageMetadata parsed = parseUsageMetadata(usage);
					if (parsed != null) {
						this.currentUsage = parsed;
					}
				}

				if (stepUpdate.has("state") && "STATE_ERROR".equals(stepUpdate.path("state").asText())) {
					String errorMessage = stepUpdate.path("errorMessage").asText("Unknown error");
					if (currentChatFuture != null && !currentChatFuture.isDone()) {
						currentChatFuture.completeExceptionally(
								new RuntimeException("Agent execution terminated: " + errorMessage));
						currentChatFuture = null;
						currentChunkConsumer = null;
						if (currentThoughtsPublisher != null) {
							currentThoughtsPublisher.close();
							currentThoughtsPublisher = null;
						}
						if (currentToolCallsPublisher != null) {
							currentToolCallsPublisher.close();
							currentToolCallsPublisher = null;
						}
					}
				}

				if (stepUpdate.has("toolConfirmationRequest")) {
					String trajectoryId = stepUpdate.path("trajectoryId").asText("");
					int stepIndex = stepUpdate.path("stepIndex").asInt(0);
					String key = trajectoryId + ":" + stepIndex;

					if (handledToolConfirmations.add(key)) {
						JsonNode req = stepUpdate.get("toolConfirmationRequest");
						String toolName = "unknown";
						JsonNode args = null;
						if (req.has("invokeSubagent"))
							toolName = "invoke_subagent";
						else if (req.has("runCommand"))
							toolName = "run_command";
						else if (req.has("fileEdit"))
							toolName = "file_edit";
						else if (req.has("finish"))
							toolName = "finish";
						else if (req.has("customToolCall")) {
							toolName = req.get("customToolCall").path("name").asText("unknown");
							try {
								String argsStr = req.get("customToolCall").path("argumentsJson").asText("{}");
								args = jsonMapper.readTree(argsStr);
							} catch (Exception e) {
							}
						}

						Policy.Decision decision = evaluatePolicies(toolName, args);
						boolean accepted = (decision != Policy.Decision.DENY);

						try {
							String responsePayload = String.format(
									"{\"toolConfirmation\": {\"trajectoryId\": \"%s\", \"stepIndex\": %d, \"accepted\": %b}}",
									trajectoryId, stepIndex, accepted);
							sendWebSocketMessage(responsePayload);
						} catch (Exception e) {
						}
					}
				}

				if (stepUpdate.has("questionsRequest")) {
					String trajectoryId = stepUpdate.path("trajectoryId").asText("");
					int stepIndex = stepUpdate.path("stepIndex").asInt(0);
					String key = trajectoryId + ":" + stepIndex;

					if (handledQuestionRequests.add(key)) {
						try {
							String json = stepUpdate.get("questionsRequest").toString();
							UserQuestionsRequest.Builder reqBuilder = UserQuestionsRequest.newBuilder();
							JsonFormat.parser().ignoringUnknownFields().merge(json, reqBuilder);
							UserQuestionsRequest req = reqBuilder.build();

							for (AgentHook hook : config.getHooks()) {
								if (hook instanceof OnInteractionHook) {
									((OnInteractionHook) hook).onInteraction(InteractionRequest.fromProtobuf(req))
											.thenAccept(resp -> {
												try {
													List<UserQuestionAnswer> answers = resp.stream()
															.map(InteractionAnswer::toProtobuf).toList();
													UserQuestionsResponse.QuestionsResponse questionsResp = UserQuestionsResponse.QuestionsResponse
															.newBuilder().addAllAnswers(answers).build();

													UserQuestionsResponse fullResp = UserQuestionsResponse.newBuilder()
															.setTrajectoryId(trajectoryId).setStepIndex(stepIndex)
															.setResponse(questionsResp).build();

													InputEvent inputEvent = InputEvent.newBuilder()
															.setQuestionResponse(fullResp).build();

													String payloadJson = JSON_PRINTER.print(inputEvent);
													sendWebSocketMessage(payloadJson);
												} catch (Exception e) {
													log.error("Failed to send question response", e);
												}
											});
									break;
								}
							}
						} catch (Exception e) {
							log.error("Error processing questions request", e);
						}
					}
				}
			}

			if (payload.has("callHookRequest")) {
				JsonNode req = payload.get("callHookRequest");
				String requestId = req.path("requestId").asText();
				String typeStr = req.path("type").asText("");

				CompletableFuture<HookResult> hookFuture = CompletableFuture.completedFuture(HookResult.allowed());

				if ("LIFECYCLE_HOOK_PRE_TURN".equals(typeStr) && req.has("preTurnArgs")) {
					System.out.println("HOOK REQUEST PRE TURN: " + req.toString());
					StringBuilder promptBuilder = new StringBuilder();
					JsonNode parts = req.get("preTurnArgs").path("userInput").path("parts");
					if (parts.isArray()) {
						for (JsonNode part : parts) {
							if (part.has("text"))
								promptBuilder.append(part.path("text").asText());
						}
					}
					hookFuture = triggerPreTurn(promptBuilder.toString());
				} else if ("LIFECYCLE_HOOK_POST_TURN".equals(typeStr) && req.has("postTurnArgs")) {
					String response = req.get("postTurnArgs").path("responseText").asText("");
					hookFuture = triggerPostTurn(response).thenApply(v -> HookResult.allowed());
				} else if ("LIFECYCLE_HOOK_PRE_TOOL".equals(typeStr) && req.has("preToolArgs")) {
					JsonNode args = req.get("preToolArgs");
					try {
						String toolName = args.has("toolName")
								? args.path("toolName").asText()
								: args.path("call").path("name").asText("");
						String argumentsJson = args.has("argumentsJson")
								? args.path("argumentsJson").asText("{}")
								: (args.path("call").has("arguments")
										? args.path("call").path("arguments").toString()
										: "{}");
						String callId = args.has("callId") ? args.path("callId").asText(null) : null;
						String serverName = args.has("serverName") ? args.path("serverName").asText(null) : null;
						String trajectoryId = args.has("trajectoryId") ? args.path("trajectoryId").asText(null) : null;
						Integer stepIndex = args.has("stepIndex") ? args.path("stepIndex").asInt() : null;
						String stepId = (trajectoryId != null && stepIndex != null)
								? (trajectoryId + ":" + stepIndex)
								: null;
						ToolCall call = new ToolCall(toolName, jsonMapper.readTree(argumentsJson), callId, stepId,
								serverName);
						hookFuture = triggerPreToolCallDecide(call);
					} catch (Exception e) {
					}
				} else if ("LIFECYCLE_HOOK_POST_TOOL".equals(typeStr) && req.has("postToolArgs")) {
					JsonNode args = req.get("postToolArgs");
					try {
						String toolName = args.has("toolName")
								? args.path("toolName").asText()
								: args.path("call").path("name").asText("");
						String argumentsJson = args.has("argumentsJson")
								? args.path("argumentsJson").asText("{}")
								: (args.path("call").has("arguments")
										? args.path("call").path("arguments").toString()
										: "{}");
						String callId = args.has("callId") ? args.path("callId").asText(null) : null;
						String serverName = args.has("serverName") ? args.path("serverName").asText(null) : null;
						String trajectoryId = args.has("trajectoryId") ? args.path("trajectoryId").asText(null) : null;
						Integer stepIndex = args.has("stepIndex") ? args.path("stepIndex").asInt() : null;
						String stepId = (trajectoryId != null && stepIndex != null)
								? (trajectoryId + ":" + stepIndex)
								: null;
						ToolCall call = new ToolCall(toolName, jsonMapper.readTree(argumentsJson), callId, stepId,
								serverName);
						String result = args.has("result")
								? args.path("result").asText("")
								: args.path("toolResult").asText("");
						hookFuture = triggerPostToolCall(call, result).thenApply(v -> HookResult.allowed());
					} catch (Exception e) {
					}
				} else if ("LIFECYCLE_HOOK_ON_TOOL_ERROR".equals(typeStr) && req.has("onToolErrorArgs")) {
					JsonNode args = req.get("onToolErrorArgs");
					try {
						String toolName = args.has("toolName")
								? args.path("toolName").asText()
								: args.path("call").path("name").asText("");
						String argumentsJson = args.has("argumentsJson")
								? args.path("argumentsJson").asText("{}")
								: (args.path("call").has("arguments")
										? args.path("call").path("arguments").toString()
										: "{}");
						String callId = args.has("callId") ? args.path("callId").asText(null) : null;
						String serverName = args.has("serverName") ? args.path("serverName").asText(null) : null;
						String trajectoryId = args.has("trajectoryId") ? args.path("trajectoryId").asText(null) : null;
						Integer stepIndex = args.has("stepIndex") ? args.path("stepIndex").asInt() : null;
						String stepId = (trajectoryId != null && stepIndex != null)
								? (trajectoryId + ":" + stepIndex)
								: null;
						ToolCall call = new ToolCall(toolName, jsonMapper.readTree(argumentsJson), callId, stepId,
								serverName);
						Throwable error = new ToolExecutionError(toolName, argumentsJson, serverName, callId, stepId,
								args.path("errorMessage").asText(""), null);
						hookFuture = triggerOnToolError(call, error).thenApply(recovery -> {
							if (recovery != null)
								return HookResult.denied();
							return HookResult.allowed();
						});
					} catch (Exception e) {
					}
				} else if ("LIFECYCLE_HOOK_ON_COMPACTION".equals(typeStr) && req.has("onCompactionArgs")) {
					JsonNode args = req.get("onCompactionArgs");
					hookFuture = triggerOnCompaction(args).thenApply(v -> HookResult.allowed());
				} else if ("LIFECYCLE_HOOK_STOP".equals(typeStr) && req.has("stopArgs")) {
					JsonNode args = req.get("stopArgs");
					StopArgs stopArgs = StopArgs.newBuilder().setResponseText(args.path("responseText").asText(""))
							.setTrajectoryId(args.path("trajectoryId").asText(""))
							.setContinuationCount(args.path("continuationCount").asInt(0))
							.setStopReason(args.path("stopReason").asText(""))
							.setErrorMessage(args.path("errorMessage").asText("")).build();
					hookFuture = triggerOnStop(stopArgs).thenApply(v -> HookResult.allowed());
				}

				hookFuture.whenComplete((res, err) -> {
					try {
						CallHookResponse.Builder respBuilder = CallHookResponse.newBuilder().setRequestId(requestId);

						if (err != null) {
							respBuilder.setErrorMessage(err.getMessage());
						} else if ("LIFECYCLE_HOOK_PRE_TURN".equals(typeStr)) {
							PreTurnResult.Builder ptr = PreTurnResult.newBuilder();
							if (!res.allow()) {
								ptr.setDecision(PreTurnResult.Decision.DENY)
										.setReason(res.reason() != null ? res.reason() : "Hook execution denied");
							} else {
								ptr.setDecision(PreTurnResult.Decision.ALLOW);
								if (res.reason() != null) {
									ptr.setReason(res.reason());
								}
							}
							respBuilder.setPreTurnResult(ptr.build());
						} else if ("LIFECYCLE_HOOK_PRE_TOOL".equals(typeStr)) {
							PreToolResult.Builder ptr = PreToolResult.newBuilder();
							if (!res.allow()) {
								ptr.setDecision(PreToolResult.Decision.DENY)
										.setReason(res.reason() != null ? res.reason() : "Hook execution denied");
							} else {
								ptr.setDecision(PreToolResult.Decision.ALLOW);
								if (res.reason() != null) {
									ptr.setReason(res.reason());
								}
								if (res.modifiedArgumentsJson() != null) {
									ptr.setModifiedArgumentsJson(res.modifiedArgumentsJson());
								}
							}
							respBuilder.setPreToolResult(ptr.build());
						} else if ("LIFECYCLE_HOOK_ON_TOOL_ERROR".equals(typeStr)) {
							if (!res.allow()) {
								respBuilder.setOnToolErrorResult(OnToolErrorResult.newBuilder()
										.setCustomErrorMessage("Hook execution denied").build());
							} else {
								respBuilder.setEmptyResult(EmptyResult.getDefaultInstance());
							}
						} else if ("LIFECYCLE_HOOK_STOP".equals(typeStr)) {
							StopResult.Builder sr = StopResult.newBuilder();
							if (!res.allow()) {
								sr.setDecision(StopResult.Decision.DENY)
										.setReason(res.reason() != null ? res.reason() : "Hook execution denied");
							} else {
								sr.setDecision(StopResult.Decision.ALLOW);
								if (res.reason() != null) {
									sr.setReason(res.reason());
								}
							}
							respBuilder.setStopResult(sr.build());
						} else {
							respBuilder.setEmptyResult(EmptyResult.getDefaultInstance());
						}

						InputEvent inputEvent = InputEvent.newBuilder().setCallHookResponse(respBuilder.build())
								.build();
						String payloadJson = JSON_PRINTER.print(inputEvent);
						sendWebSocketMessage(payloadJson);
					} catch (Exception e) {
						log.error("Failed to send call hook response", e);
					}
				});
			}

			if (payload.has("trajectoryStateUpdate")) {
				String state = payload.get("trajectoryStateUpdate").path("state").asText();
				if ("STATE_IDLE".equals(state) || "STATE_FULLY_IDLE".equals(state)) {
					if (currentChatFuture != null && !currentChatFuture.isDone()) {
						if (clientCancelled) {
							currentChatFuture.completeExceptionally(new AgentCancelledException());
							if (currentThoughtsPublisher != null) {
								currentThoughtsPublisher.closeExceptionally(new AgentCancelledException());
							}
							if (currentToolCallsPublisher != null) {
								currentToolCallsPublisher.closeExceptionally(new AgentCancelledException());
							}
						} else {
							UsageMetadata finalUsage = currentUsage;
							if (finalUsage == null && cumulativeUsage != null) {
								UsageMetadata diff = (turnStartUsage != null)
										? cumulativeUsage.subtract(turnStartUsage)
										: cumulativeUsage;
								if (diff.totalTokenCount() > 0) {
									finalUsage = diff;
								}
							}
							currentChatFuture
									.complete(new AgentResponse(currentText != null ? currentText.toString() : "",
											currentThoughts != null ? currentThoughts.toString() : "", finalUsage));
							if (currentThoughtsPublisher != null) {
								currentThoughtsPublisher.close();
							}
							if (currentToolCallsPublisher != null) {
								currentToolCallsPublisher.close();
							}
						}
						currentChatFuture = null;
						currentChunkConsumer = null;
						currentThoughtsPublisher = null;
						currentToolCallsPublisher = null;
					}
				} else if ("STATE_CANCELLED".equals(state)) {
					if (currentChatFuture != null && !currentChatFuture.isDone()) {
						currentChatFuture.completeExceptionally(new AgentCancelledException());
						currentChatFuture = null;
						currentChunkConsumer = null;
						if (currentThoughtsPublisher != null) {
							currentThoughtsPublisher.closeExceptionally(new AgentCancelledException());
							currentThoughtsPublisher = null;
						}
						if (currentToolCallsPublisher != null) {
							currentToolCallsPublisher.closeExceptionally(new AgentCancelledException());
							currentToolCallsPublisher = null;
						}
					}
				}
			}

			if (payload.has("toolCall")) {
				JsonNode toolCallNode = payload.get("toolCall");
				String callId = toolCallNode.path("id").asText();
				String name = toolCallNode.path("name").asText();
				String argsJsonString = toolCallNode.path("argumentsJson").asText();
				JsonNode args = jsonMapper.readTree(argsJsonString);

				if (currentToolCallsPublisher != null) {
					currentToolCallsPublisher.submit(new ToolCall(name, args));
				}
				if ("finish".equals(name)) {
					if (currentText != null) {
						currentText.setLength(0);
						currentText.append(argsJsonString);
					}
					try {
						InputEvent responseEvent = InputEvent.newBuilder()
								.setToolResponse(ToolResponse.newBuilder().setId(callId).setResponseJson("{}").build())
								.build();
						String responsePayload = JSON_PRINTER.print(responseEvent);
						sendWebSocketMessage(responsePayload);
					} catch (Exception e) {
						log.error("Failed to send finish tool response", e);
					}
					return;
				}

				ToolCall parsedCall = new ToolCall(name, args);

				Policy.Decision decision = evaluatePolicies(name, args);
				if (decision == Policy.Decision.DENY) {
					sendToolResponse(callId, "{\"error\": \"Execution denied by policy\"}");
					return;
				}

				toolExecutor.submit(() -> {
					try {
						String resultJson = toolRegistry.execute(name, args, new ToolContext() {
							@Override
							public String getConversationId() {
								return Agent.this.getConversationId();
							}
							@Override
							public boolean isIdle() {
								return currentChatFuture == null;
							}
							@Override
							public void send(String message) {
								Agent.this.fireTrigger(message);
							}
							@Override
							public Object getState(String key, Object defaultValue) {
								return toolState.getOrDefault(key, defaultValue);
							}
							@Override
							public void setState(String key, Object value) {
								toolState.put(key, value);
							}
							@Override
							public ConcurrentMap<String, Object> getStateMap() {
								return toolState;
							}
						});
						sendToolResponse(callId, resultJson);
					} catch (Exception e) {
						log.error("Tool execution error for {}", name, e);
						sendToolResponse(callId, "{\"error\": \"Tool error: " + e.getMessage() + "\"}");
					}
				});
			}
		} catch (Exception e) {
			log.error("Error processing WS message: {}", e.getMessage(), e);
		}
	}

	private void sendToolResponse(String callId, String resultJson) {
		try {
			InputEvent responseEvent = InputEvent.newBuilder()
					.setToolResponse(ToolResponse.newBuilder().setId(callId).setResponseJson(resultJson).build())
					.build();
			String responsePayload = JSON_PRINTER.print(responseEvent);
			sendWebSocketMessage(responsePayload);
		} catch (Exception e) {
			log.error("Failed to send tool response for callId {}", callId, e);
			if (currentChatFuture != null && !currentChatFuture.isDone()) {
				currentChatFuture.completeExceptionally(e);
			}
		}
	}

	private synchronized void sendWebSocketMessage(String payload) {
		try {
			if (this.webSocket != null) {
				this.webSocket.sendText(payload, true).join();
			}
		} catch (Exception e) {
			log.error("Failed to send WebSocket payload: {}", payload, e);
			if (currentChatFuture != null && !currentChatFuture.isDone()) {
				currentChatFuture.completeExceptionally(e);
			}
		}
	}

	@Override
	public void close() throws Exception {
		triggerSessionEnd().join();

		for (AgentTrigger trigger : config.getTriggers()) {
			trigger.stop();
		}

		if (webSocket != null) {
			try {
				webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "").join();
			} catch (Exception e) {
				// Ignore
			}
		}

		if (goProcess != null && goProcess.isAlive()) {
			boolean exited = goProcess.waitFor(2, TimeUnit.SECONDS);
			if (!exited) {
				goProcess.getOutputStream().close();
				exited = goProcess.waitFor(3, TimeUnit.SECONDS);
			}

			if (!exited) {
				goProcess.destroyForcibly();
			}
		}
		toolExecutor.shutdown();
		try {
			if (!toolExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
				toolExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			toolExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	void initTurnForTest(Consumer<AgentResponseChunk> chunkConsumer) {
		this.currentText = new StringBuilder();
		this.currentThoughts = new StringBuilder();
		this.currentChunkConsumer = chunkConsumer;
	}

	String getCurrentTextForTest() {
		return this.currentText != null ? this.currentText.toString() : "";
	}

	String getCurrentThoughtsForTest() {
		return this.currentThoughts != null ? this.currentThoughts.toString() : "";
	}

	private static String resolveGeminiApiKey() {
		String envKey = System.getenv("GEMINI_API_KEY");
		String propKey = System.getProperty("GEMINI_API_KEY");
		String localEnvKey = null;
		try {
			Path envPath = Paths.get(".local.env");
			if (!Files.exists(envPath)) {
				envPath = Paths.get("../.local.env");
			}
			if (Files.exists(envPath)) {
				for (String line : Files.readAllLines(envPath)) {
					if (line.startsWith("GEMINI_API_KEY=")) {
						localEnvKey = line.substring("GEMINI_API_KEY=".length()).trim();
						break;
					}
				}
			}
		} catch (Exception e) {
		}
		return propKey != null ? propKey : (envKey != null ? envKey : localEnvKey);
	}

	/**
	 * Parses a {@link UsageMetadata} object from a Jackson {@link JsonNode},
	 * supporting both camelCase and snake_case field variants.
	 *
	 * @param usage
	 *            the JSON node representing UsageMetadata
	 * @return the parsed UsageMetadata record, or null if node is null/missing
	 */
	static UsageMetadata parseUsageMetadata(JsonNode usage) {
		if (usage == null || usage.isMissingNode() || usage.isNull()) {
			return null;
		}
		int promptTokenCount = getIntField(usage, "promptTokenCount", "prompt_token_count");
		int cachedContentTokenCount = getIntField(usage, "cachedContentTokenCount", "cached_content_token_count");
		int candidatesTokenCount = getIntField(usage, "candidatesTokenCount", "candidates_token_count");
		int thoughtsTokenCount = getIntField(usage, "thoughtsTokenCount", "thoughts_token_count");
		int totalTokenCount = getIntField(usage, "totalTokenCount", "total_token_count");

		String serviceTier = null;
		if (usage.has("serviceTier")) {
			serviceTier = usage.get("serviceTier").asText();
		} else if (usage.has("service_tier")) {
			serviceTier = usage.get("service_tier").asText();
		}

		JsonNode promptDetailsNode = usage.has("promptTokensDetails")
				? usage.get("promptTokensDetails")
				: usage.get("prompt_tokens_details");
		JsonNode cacheDetailsNode = usage.has("cacheTokensDetails")
				? usage.get("cacheTokensDetails")
				: usage.get("cache_tokens_details");
		JsonNode candidateDetailsNode = usage.has("candidatesTokensDetails")
				? usage.get("candidatesTokensDetails")
				: usage.get("candidates_tokens_details");
		JsonNode toolUseDetailsNode = usage.has("toolUsePromptTokensDetails")
				? usage.get("toolUsePromptTokensDetails")
				: usage.get("tool_use_prompt_tokens_details");

		List<ModalityTokenCount> promptDetails = parseModalityDetails(promptDetailsNode);
		List<ModalityTokenCount> cacheDetails = parseModalityDetails(cacheDetailsNode);
		List<ModalityTokenCount> candidateDetails = parseModalityDetails(candidateDetailsNode);
		List<ModalityTokenCount> toolUseDetails = parseModalityDetails(toolUseDetailsNode);

		return new UsageMetadata(promptTokenCount, cachedContentTokenCount, candidatesTokenCount, thoughtsTokenCount,
				totalTokenCount, serviceTier, promptDetails, cacheDetails, candidateDetails, toolUseDetails);
	}

	private static int getIntField(JsonNode node, String camelCaseName, String snakeCaseName) {
		if (node.has(camelCaseName)) {
			return node.get(camelCaseName).asInt(0);
		} else if (node.has(snakeCaseName)) {
			return node.get(snakeCaseName).asInt(0);
		}
		return 0;
	}

	/**
	 * Parses a JSON array of modality token details into a list of
	 * {@link ModalityTokenCount} records.
	 *
	 * @param node
	 *            the JSON node containing the modality details array
	 * @return an unmodifiable list of ModalityTokenCount records
	 */
	static List<ModalityTokenCount> parseModalityDetails(JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		List<ModalityTokenCount> list = new ArrayList<>();
		for (JsonNode item : node) {
			String modStr = item.path("modality").asText("");
			long count = item.has("tokenCount") ? item.get("tokenCount").asLong(0) : item.path("token_count").asLong(0);
			Modality mod = Modality.fromString(modStr);
			list.add(new ModalityTokenCount(mod, count));
		}
		return Collections.unmodifiableList(list);
	}
}
