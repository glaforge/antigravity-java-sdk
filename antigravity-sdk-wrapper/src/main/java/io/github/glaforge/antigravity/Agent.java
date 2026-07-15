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
import io.github.glaforge.antigravity.hooks.*;
import io.github.glaforge.antigravity.hooks.ToolCall;
import io.github.glaforge.antigravity.tools.ToolRegistry;
import io.github.glaforge.antigravity.tools.ToolDefinition;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The main agent class that manages the lifecycle and interaction with the
 * local harness.
 */
public class Agent implements AutoCloseable, TriggerContext {
	private final Process goProcess;
	private WebSocket webSocket;
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
	private final List<Policy> policies;
	private boolean hasStructuredOutput;
	private StringBuilder wsBuffer = new StringBuilder();
	private final ConcurrentMap<String, Object> toolState = new ConcurrentHashMap<>();
	private final SessionContext sessionContext = new SessionContext();

	/**
	 * Returns the usage metadata from the most recent turn.
	 *
	 * @return the usage metadata
	 */
	UsageMetadata getUsageMetadata() {
		return currentUsage;
	}

	/**
	 * Returns the unique ID of the conversation.
	 *
	 * @return the conversation ID
	 */
	String getConversationId() {
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
			String payload = JsonFormat.printer().omittingInsignificantWhitespace().print(event);
			webSocket.sendText(payload, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Cancels the current agent execution.
	 */
	public void cancel() {
		try {
			this.clientCancelled = true;
			InputEvent event = InputEvent.newBuilder().setHaltRequest(true).build();
			String payload = JsonFormat.printer().omittingInsignificantWhitespace().print(event);
			webSocket.sendText(payload, true);
		} catch (Exception e) {
			e.printStackTrace();
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
		this.config = config;
		this.policies = config.getPolicies();
		for (Object tool : config.getToolInstances()) {
			this.registerTools(tool);
		}

		// 1. Detect environment variables
		String platformSlice = PlatformResolver.getPlatformSlice();
		boolean isWindows = platformSlice.startsWith("windows");
		String ext = isWindows ? ".exe" : "";
		String resourcePath = "/google/antigravity/bin/" + platformSlice + "/localharness" + ext;

		// 2. Extract binary to temp dir
		File tempExecutable = File.createTempFile("localharness-" + platformSlice + "-", isWindows ? ".exe" : ".tmp");
		tempExecutable.deleteOnExit();

		try (InputStream binaryStream = Agent.class.getResourceAsStream(resourcePath)) {
			if (binaryStream == null) {
				throw new FileNotFoundException("Embedded Go harness engine asset missing for slice: " + platformSlice);
			}
			Files.copy(binaryStream, tempExecutable.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		// 3. Set Unix file permissions natively
		if (!tempExecutable.setExecutable(true)) {
			throw new IllegalStateException(
					"Failed to grant execution rights to temp binary: " + tempExecutable.getAbsolutePath());
		}

		// 4. Spawn process
		this.goProcess = new ProcessBuilder(tempExecutable.getAbsolutePath())
				.redirectError(ProcessBuilder.Redirect.INHERIT).start();

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

			String apiKey = propKey != null
					? propKey
					: (envKey != null ? envKey : (localEnvKey != null ? localEnvKey : "placeholder"));

			Thread stdoutConsumer = new Thread(() -> {
				try {
					is.transferTo(System.err);
				} catch (Exception e) {
				}
			});
			stdoutConsumer.setDaemon(true);
			stdoutConsumer.start();

			Thread stderrConsumer = new Thread(() -> {
				try {
					this.goProcess.getErrorStream().transferTo(System.err);
				} catch (Exception e) {
				}
			});
			stderrConsumer.setDaemon(true);
			stderrConsumer.start();

			HarnessConfig.Builder configBuilder = HarnessConfig.newBuilder().setCascadeId(config.getConversationId())
					.setAppDataDir(config.getAppDataDir() != null ? config.getAppDataDir() : "");

			io.github.glaforge.antigravity.localharness.ModelConfig modelConfig = io.github.glaforge.antigravity.localharness.ModelConfig
					.newBuilder().setName(config.getModelName())
					.addTypes(io.github.glaforge.antigravity.localharness.ModelType.MODEL_TYPE_TEXT)
					.setGeminiApiEndpoint(io.github.glaforge.antigravity.localharness.GeminiAPIEndpoint.newBuilder()
							.setApiKey(apiKey).build())
					.build();
			configBuilder.addModels(modelConfig);

			configBuilder.setSystemInstructions(SystemInstructions.newBuilder()
					.setAppended(
							AppendedSystemInstructions.newBuilder().setCustomIdentity(config.getInstructions()).build())
					.build());

			int mcpIndex = 1;
			for (McpServerConfig mcp : config.getMcpServers()) {
				io.github.glaforge.antigravity.localharness.McpServerConfig.Builder mcpBuilder = io.github.glaforge.antigravity.localharness.McpServerConfig
						.newBuilder().setName("server-" + (mcpIndex++));

				if (mcp instanceof McpServerConfig.StdioMcpServerConfig stdio) {
					mcpBuilder.setStdio(io.github.glaforge.antigravity.localharness.McpStdioTransport.newBuilder()
							.setCommand(stdio.command()).addAllArgs(stdio.args()).build());
				} else if (mcp instanceof McpServerConfig.SseMcpServerConfig sse) {
					io.github.glaforge.antigravity.localharness.McpHttpTransport.Builder http = io.github.glaforge.antigravity.localharness.McpHttpTransport
							.newBuilder().setUrl(sse.url());
					if (sse.headers() != null) {
						http.putAllHeaders(sse.headers());
					}
					mcpBuilder.setHttp(http.build());
				}
				configBuilder.addMcpServers(mcpBuilder.build());
			}

			// Add Hooks tracking
			for (AgentHook hook : config.getHooks()) {
				if (hook instanceof PreTurnHook)
					configBuilder.addEnabledHooks(
							io.github.glaforge.antigravity.localharness.LifecycleHook.LIFECYCLE_HOOK_PRE_TURN);
				if (hook instanceof PostTurnHook)
					configBuilder.addEnabledHooks(
							io.github.glaforge.antigravity.localharness.LifecycleHook.LIFECYCLE_HOOK_POST_TURN);
				if (hook instanceof PreToolCallDecideHook)
					configBuilder.addEnabledHooks(
							io.github.glaforge.antigravity.localharness.LifecycleHook.LIFECYCLE_HOOK_PRE_TOOL);
				if (hook instanceof PostToolCallHook)
					configBuilder.addEnabledHooks(
							io.github.glaforge.antigravity.localharness.LifecycleHook.LIFECYCLE_HOOK_POST_TOOL);
				if (hook instanceof OnToolErrorHook)
					configBuilder.addEnabledHooks(
							io.github.glaforge.antigravity.localharness.LifecycleHook.LIFECYCLE_HOOK_ON_TOOL_ERROR);
				if (hook instanceof OnInteractionHook)
					configBuilder.addEnabledHooks(
							io.github.glaforge.antigravity.localharness.LifecycleHook.LIFECYCLE_HOOK_UNSPECIFIED); // Will
																													// use
																													// manually
																													// for
																													// now
			}

			for (Object obj : toolRegistry.getToolDefinitions()) {
				ToolDefinition toolDef = (ToolDefinition) obj;
				configBuilder.addTools(toolDef.toProtobuf());
			}
			configBuilder.addAllSkillsPaths(config.getSkillsPaths());

			if (config.getFinishToolSchemaJson() != null) {
				configBuilder.setFinishToolSchemaJson(config.getFinishToolSchemaJson());
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
					capBuilder.setRunCommand(RunCommandToolConfig.newBuilder().setEnabled(true).build());
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
				configBuilder.setHarnessSideTools(capBuilder.build());
			}

			HarnessConfig protoConfig = configBuilder.build();

			InitializeConversationEvent initEvent = InitializeConversationEvent.newBuilder().setConfig(protoConfig)
					.build();

			String initEventJson = JsonFormat.printer().omittingInsignificantWhitespace().print(initEvent);

			HttpClient client = HttpClient.newHttpClient();
			this.webSocket = client.newWebSocketBuilder().header("x-goog-api-key", securityToken)
					.buildAsync(URI.create("ws://localhost:" + runtimePort), new WebSocket.Listener() {
						@Override
						public void onOpen(WebSocket webSocket) {
							webSocket.sendText(initEventJson, true);
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
							if (currentChatFuture != null && !currentChatFuture.isDone()) {
								currentChatFuture.completeExceptionally(error);
							}
							WebSocket.Listener.super.onError(webSocket, error);
						}

						@Override
						public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
							if (currentChatFuture != null && !currentChatFuture.isDone()) {
								currentChatFuture.completeExceptionally(new RuntimeException(
										"WebSocket closed unexpectedly: " + statusCode + " " + reason));
							}
							return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
						}
					}).join();
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
	 * Sends a text message to the agent and streams the response chunks.
	 *
	 * @param text
	 *            the text message
	 * @param onChunk
	 *            a consumer to handle the incoming chunks
	 * @return a CompletableFuture containing the final AgentResponse
	 */

	/**
	 * Sends a text message to the agent and returns a Publisher of response chunks.
	 *
	 * @param prompt
	 *            the text prompt to send
	 * @return a Flow.Publisher emitting AgentResponseChunk items
	 */

	/**
	 * Sends multiple inputs to the agent and returns an AgentStream containing
	 * distinct publishers for chunks, thoughts, and tool calls.
	 *
	 * @param inputs
	 *            the list of inputs
	 * @return an AgentStream
	 */
	public AgentStream streamChat(List<AgentInput> inputs) {
		java.util.concurrent.SubmissionPublisher<AgentResponseChunk> chunksPublisher = new java.util.concurrent.SubmissionPublisher<>();
		this.currentThoughtsPublisher = new java.util.concurrent.SubmissionPublisher<>();
		this.currentToolCallsPublisher = new java.util.concurrent.SubmissionPublisher<>();

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

	public AgentStream streamChat(String text) {
		return streamChat(List.of(AgentInput.Text.of(text)));
	}

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

			InputEvent event = InputEvent.newBuilder().setComplexUserInput(userInputBuilder.build()).build();
			String payload = JsonFormat.printer().omittingInsignificantWhitespace().print(event);
			this.webSocket.sendText(payload, true);
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

	private void handleIncomingMessage(WebSocket webSocket, String message) {
		try {
			JsonNode payload = jsonMapper.readTree(message);

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
					String textDelta = stepUpdate.path("textDelta").asText("");
					String thinkingDelta = stepUpdate.path("thinkingDelta").asText("");

					if (currentText != null && !hasStructuredOutput)
						currentText.append(textDelta);
					if (currentThoughts != null)
						currentThoughts.append(thinkingDelta);

					if (currentChunkConsumer != null && (!textDelta.isEmpty() || !thinkingDelta.isEmpty())) {
						currentChunkConsumer.accept(new AgentResponseChunk(textDelta, thinkingDelta));
					}
				}

				if (stepUpdate.has("usageMetadata")) {
					JsonNode usage = stepUpdate.get("usageMetadata");
					currentUsage = new UsageMetadata(usage.path("promptTokenCount").asInt(),
							usage.path("cachedContentTokenCount").asInt(), usage.path("candidatesTokenCount").asInt(),
							usage.path("thoughtsTokenCount").asInt(), usage.path("totalTokenCount").asInt());
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
						String trajectoryId = stepUpdate.get("trajectoryId").asText();
						int stepIndex = stepUpdate.get("stepIndex").asInt();

						String responsePayload = String.format(
								"{\"toolConfirmation\": {\"trajectoryId\": \"%s\", \"stepIndex\": %d, \"accepted\": %b}}",
								trajectoryId, stepIndex, accepted);
						webSocket.sendText(responsePayload, true);
					} catch (Exception e) {
					}
				}

				if (stepUpdate.has("questionsRequest")) {
					try {
						String trajectoryId = stepUpdate.get("trajectoryId").asText();
						int stepIndex = stepUpdate.get("stepIndex").asInt();

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

												String payloadJson = JsonFormat.printer()
														.omittingInsignificantWhitespace().print(inputEvent);
												webSocket.sendText(payloadJson, true);
											} catch (Exception e) {
												e.printStackTrace();
											}
										});
								break;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
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
						ToolCall call = new ToolCall(toolName, jsonMapper.readTree(argumentsJson));
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
						ToolCall call = new ToolCall(toolName, jsonMapper.readTree(argumentsJson));
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
						ToolCall call = new ToolCall(toolName, jsonMapper.readTree(argumentsJson));
						hookFuture = triggerOnToolError(call,
								new RuntimeException(args.path("errorMessage").asText(""))).thenApply(recovery -> {
									if (recovery != null)
										return HookResult.denied();
									return HookResult.allowed();
								});
					} catch (Exception e) {
					}
				}

				hookFuture.whenComplete((res, err) -> {
					try {
						io.github.glaforge.antigravity.localharness.CallHookResponse.Builder respBuilder = io.github.glaforge.antigravity.localharness.CallHookResponse
								.newBuilder().setRequestId(requestId);

						if (err != null) {
							respBuilder.setErrorMessage(err.getMessage());
						} else if ("LIFECYCLE_HOOK_PRE_TURN".equals(typeStr)) {
							io.github.glaforge.antigravity.localharness.PreTurnResult.Builder ptr = io.github.glaforge.antigravity.localharness.PreTurnResult
									.newBuilder();
							if (!res.allow()) {
								ptr.setDecision(io.github.glaforge.antigravity.localharness.PreTurnResult.Decision.DENY)
										.setReason("Hook execution denied");
							} else {
								ptr.setDecision(
										io.github.glaforge.antigravity.localharness.PreTurnResult.Decision.ALLOW);
							}
							respBuilder.setPreTurnResult(ptr.build());
						} else if ("LIFECYCLE_HOOK_PRE_TOOL".equals(typeStr)) {
							io.github.glaforge.antigravity.localharness.PreToolResult.Builder ptr = io.github.glaforge.antigravity.localharness.PreToolResult
									.newBuilder();
							if (!res.allow()) {
								ptr.setDecision(io.github.glaforge.antigravity.localharness.PreToolResult.Decision.DENY)
										.setReason("Hook execution denied");
							} else {
								ptr.setDecision(
										io.github.glaforge.antigravity.localharness.PreToolResult.Decision.ALLOW);
							}
							respBuilder.setPreToolResult(ptr.build());
						} else if ("LIFECYCLE_HOOK_ON_TOOL_ERROR".equals(typeStr)) {
							if (!res.allow()) {
								respBuilder.setOnToolErrorResult(
										io.github.glaforge.antigravity.localharness.OnToolErrorResult.newBuilder()
												.setCustomErrorMessage("Hook execution denied").build());
							} else {
								respBuilder.setEmptyResult(
										io.github.glaforge.antigravity.localharness.EmptyResult.getDefaultInstance());
							}
						} else {
							respBuilder.setEmptyResult(
									io.github.glaforge.antigravity.localharness.EmptyResult.getDefaultInstance());
						}

						InputEvent inputEvent = InputEvent.newBuilder().setCallHookResponse(respBuilder.build())
								.build();
						String payloadJson = JsonFormat.printer().omittingInsignificantWhitespace().print(inputEvent);
						webSocket.sendText(payloadJson, true);
					} catch (Exception e) {
					}
				});
			}

			if (payload.has("trajectoryStateUpdate")) {
				String state = payload.get("trajectoryStateUpdate").path("state").asText();
				if ("STATE_IDLE".equals(state)) {
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
							currentChatFuture
									.complete(new AgentResponse(currentText != null ? currentText.toString() : "",
											currentThoughts != null ? currentThoughts.toString() : "", currentUsage));
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
					currentToolCallsPublisher.submit(new io.github.glaforge.antigravity.hooks.ToolCall(name, args));
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
						String responsePayload = JsonFormat.printer().omittingInsignificantWhitespace()
								.print(responseEvent);
						webSocket.sendText(responsePayload, true);
					} catch (Exception e) {
						e.printStackTrace();
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
						e.printStackTrace();
						sendToolResponse(callId, "{\"error\": \"Tool error: " + e.getMessage() + "\"}");
					}
				});
			}
		} catch (Exception e) {
			System.err.println("Error processing WS message: " + e.getMessage());
		}
	}

	private void sendToolResponse(String callId, String resultJson) {
		try {
			InputEvent responseEvent = InputEvent.newBuilder()
					.setToolResponse(ToolResponse.newBuilder().setId(callId).setResponseJson(resultJson).build())
					.build();
			String responsePayload = JsonFormat.printer().omittingInsignificantWhitespace().print(responseEvent);
			webSocket.sendText(responsePayload, true);
		} catch (Exception e) {
			e.printStackTrace();
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
	}
}
