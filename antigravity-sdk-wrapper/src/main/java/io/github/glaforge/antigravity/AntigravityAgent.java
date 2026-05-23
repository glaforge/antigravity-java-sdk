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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.glaforge.antigravity.localharness.*;
import io.github.glaforge.antigravity.hooks.*;
import io.github.glaforge.antigravity.tools.ToolRegistry;
import com.google.protobuf.util.JsonFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.OutputStream;

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
import java.util.function.Consumer;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The main agent class that manages the lifecycle and interaction with the
 * local harness.
 */
public class AntigravityAgent implements AutoCloseable, TriggerContext {
	private final Process goProcess;
	private WebSocket webSocket;
	private final ToolRegistry toolRegistry = new ToolRegistry();
	private final ExecutorService toolExecutor = Executors.newVirtualThreadPerTaskExecutor();
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private String conversationId;

	private CompletableFuture<AgentResponse> currentChatFuture;
	private Consumer<AgentResponseChunk> currentChunkConsumer;
	private StringBuilder currentText;
	private StringBuilder currentThoughts;
	private UsageMetadata currentUsage;
	private final List<Policy> policies;
	private boolean hasStructuredOutput;
	private final McpBridge mcpBridge;
	private StringBuilder wsBuffer = new StringBuilder();

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
	 * Constructs a new AntigravityAgent with the specified configuration.
	 *
	 * @param config
	 *            the configuration for the agent
	 * @throws Exception
	 *             if an error occurs during initialization
	 */
	public AntigravityAgent(AgentConfig config) throws Exception {
		this.config = config;
		this.policies = config.getPolicies();
		for (Object tool : config.getToolInstances()) {
			this.registerTools(tool);
		}

		this.mcpBridge = new McpBridge();
		this.mcpBridge.connect(config.getMcpServers());
		for (DynamicTool dynamicTool : this.mcpBridge.getDiscoveredTools()) {
			this.toolRegistry.registerDynamicTool(dynamicTool);
		}

		// 1. Detect environment variables
		String platformSlice = PlatformResolver.getPlatformSlice();
		String resourcePath = "/google/antigravity/bin/" + platformSlice + "/localharness";

		// 2. Extract binary to temp dir
		File tempExecutable = File.createTempFile("localharness-" + platformSlice + "-", ".tmp");
		tempExecutable.deleteOnExit();

		try (InputStream binaryStream = AntigravityAgent.class.getResourceAsStream(resourcePath)) {
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
					.setAppDataDir(config.getAppDataDir() != null ? config.getAppDataDir() : "")
					.setGeminiConfig(
							GeminiConfig.newBuilder().setModelName(config.getModelName()).setApiKey(apiKey).build())
					.setSystemInstructions(SystemInstructions.newBuilder().setAppended(
							AppendedSystemInstructions.newBuilder().setCustomIdentity(config.getPersona()).build())
							.build());
			for (Object obj : toolRegistry.getToolDefinitions()) {
				Tool toolDef = (Tool) obj;
				configBuilder.addTools(toolDef);
			}
			configBuilder.addAllSkillsPaths(config.getSkillsPaths());

			if (config.getFinishToolSchemaJson() != null) {
				configBuilder.setFinishToolSchemaJson(config.getFinishToolSchemaJson());
			}

			if (config.isEnableSubagents() || config.isAllowUserQuestions()) {
				HarnessSideTools.Builder capBuilder = HarnessSideTools.newBuilder();
				if (config.isEnableSubagents()) {
					capBuilder.setSubagents(SubagentsConfig.newBuilder().setEnabled(true).build());
				}
				if (config.isAllowUserQuestions()) {
					capBuilder.setUserQuestions(UserQuestionsConfig.newBuilder().setEnabled(true).build());
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
	 * Sends a single text message to the agent and waits for the final response.
	 *
	 * @param text
	 *            the text message
	 * @return a CompletableFuture containing the AgentResponse
	 */
	public CompletableFuture<AgentResponse> chat(String text) {
		return chatStream(List.of(AgentInput.Text.of(text)), null);
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
	 * Sends a list of inputs to the agent and waits for the final response.
	 *
	 * @param inputs
	 *            the list of inputs
	 * @return a CompletableFuture containing the AgentResponse
	 */
	public CompletableFuture<AgentResponse> chat(List<AgentInput> inputs) {
		return chatStream(inputs, null);
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
		if (currentChatFuture != null && !currentChatFuture.isDone()) {
			throw new IllegalStateException("An existing chat request is still processing.");
		}
		this.currentChatFuture = new CompletableFuture<>();
		this.currentChunkConsumer = onChunk;
		this.currentText = new StringBuilder();
		this.currentThoughts = new StringBuilder();
		this.currentUsage = null;
		this.hasStructuredOutput = false;

		String combinedPrompt = inputs.stream().filter(i -> i instanceof AgentInput.Text)
				.map(i -> ((AgentInput.Text) i).text()).reduce("", (a, b) -> a + b);

		triggerPreTurn(combinedPrompt).thenAccept(res -> {
			if (!res.allow()) {
				currentChatFuture.completeExceptionally(new RuntimeException("Turn denied by hook"));
				return;
			}
			try {
				UserInput.Builder userInputBuilder = UserInput.newBuilder();
				for (AgentInput input : inputs) {
					if (input instanceof AgentInput.Text t) {
						userInputBuilder.addParts(UserInput.Part.newBuilder().setText(t.text()).build());
					} else if (input instanceof AgentInput.Media m) {
						UserInput.Media.Builder mediaBuilder = UserInput.Media.newBuilder().setMimeType(m.mimeType())
								.setData(com.google.protobuf.ByteString.copyFrom(m.data()));
						if (m.description() != null) {
							mediaBuilder.setDescription(m.description());
						}
						userInputBuilder.addParts(UserInput.Part.newBuilder().setMedia(mediaBuilder.build()).build());
					}
				}

				InputEvent event = InputEvent.newBuilder().setComplexUserInput(userInputBuilder.build()).build();
				String payload = JsonFormat.printer().omittingInsignificantWhitespace().print(event);
				webSocket.sendText(payload, true);
			} catch (Exception e) {
				currentChatFuture.completeExceptionally(e);
			}
		});

		return currentChatFuture.thenCompose(resp -> triggerPostTurn(resp.getText()).thenApply(v -> resp));
	}

	private CompletableFuture<Void> triggerSessionStart() {
		CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof OnSessionStartHook ssh) {
				future = future.thenCompose(v -> ssh.onSessionStart());
			}
		}
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
					return pth.onPreTurn(prompt);
				});
			}
		}
		return future;
	}

	private CompletableFuture<Void> triggerPostTurn(String response) {
		CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof PostTurnHook pth) {
				future = future.thenCompose(v -> pth.onPostTurn(response));
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
					return ptcd.onPreToolCallDecide(call);
				});
			}
		}
		return future;
	}

	private CompletableFuture<Void> triggerPostToolCall(ToolCall call, Object result) {
		CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof PostToolCallHook ptch) {
				future = future.thenCompose(v -> ptch.onPostToolCall(call, result));
			}
		}
		return future;
	}

	private CompletableFuture<Object> triggerOnToolError(ToolCall call, Throwable err) {
		CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof OnToolErrorHook teh) {
				future = future.thenCompose(res -> {
					if (res != null)
						return CompletableFuture.completedFuture(res);
					return teh.onToolError(call, err);
				});
			}
		}
		return future;
	}

	private CompletableFuture<Void> triggerOnCompaction(Object stepData) {
		CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
		for (AgentHook hook : config.getHooks()) {
			if (hook instanceof OnCompactionHook cch) {
				future = future.thenCompose(v -> cch.onCompaction(stepData));
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
						e.printStackTrace();
					}
				}

				if (stepUpdate.has("questionsRequest")) {
					try {
						String trajectoryId = stepUpdate.get("trajectoryId").asText();
						int stepIndex = stepUpdate.get("stepIndex").asInt();

						String json = stepUpdate.get("questionsRequest").toString();
						UserQuestionsRequest.Builder reqBuilder = UserQuestionsRequest.newBuilder();
						com.google.protobuf.util.JsonFormat.parser().ignoringUnknownFields().merge(json, reqBuilder);
						UserQuestionsRequest req = reqBuilder.build();

						for (AgentHook hook : config.getHooks()) {
							if (hook instanceof OnInteractionHook) {
								((OnInteractionHook) hook).onInteraction(req).thenAccept(resp -> {
									try {
										UserQuestionsResponse.QuestionsResponse questionsResp = UserQuestionsResponse.QuestionsResponse
												.newBuilder().addAllAnswers(resp).build();

										UserQuestionsResponse fullResp = UserQuestionsResponse.newBuilder()
												.setTrajectoryId(trajectoryId).setStepIndex(stepIndex)
												.setResponse(questionsResp).build();

										InputEvent inputEvent = InputEvent.newBuilder().setQuestionResponse(fullResp)
												.build();

										String payloadJson = com.google.protobuf.util.JsonFormat.printer()
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

			if (payload.has("trajectoryStateUpdate")) {
				String state = payload.get("trajectoryStateUpdate").path("state").asText();
				if ("STATE_IDLE".equals(state)) {
					if (currentChatFuture != null && !currentChatFuture.isDone()) {
						currentChatFuture.complete(new AgentResponse(currentText != null ? currentText.toString() : "",
								currentThoughts != null ? currentThoughts.toString() : "", currentUsage));
						currentChatFuture = null;
						currentChunkConsumer = null;
					}
				}
			}

			if (payload.has("toolCall")) {
				JsonNode toolCallNode = payload.get("toolCall");
				String callId = toolCallNode.path("id").asText();
				String name = toolCallNode.path("name").asText();
				String argsJsonString = toolCallNode.path("argumentsJson").asText();
				JsonNode args = jsonMapper.readTree(argsJsonString);

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
				triggerPreToolCallDecide(parsedCall).thenAccept(res -> {
					if (!res.allow()) {
						sendToolResponse(callId, "{\"error\": \"Execution denied by hook\"}");
						return;
					}

					Policy.Decision decision = evaluatePolicies(name, args);
					if (decision == Policy.Decision.DENY) {
						sendToolResponse(callId, "{\"error\": \"Execution denied by policy\"}");
						return;
					}

					toolExecutor.submit(() -> {
						try {
							String resultJson = toolRegistry.execute(name, args);
							triggerPostToolCall(parsedCall, resultJson).join();
							sendToolResponse(callId, resultJson);
						} catch (Exception e) {
							triggerOnToolError(parsedCall, e).thenAccept(recovery -> {
								if (recovery != null) {
									sendToolResponse(callId, recovery.toString());
								} else {
									e.printStackTrace();
									sendToolResponse(callId, "{\"error\": \"Tool error: " + e.getMessage() + "\"}");
								}
							});
						}
					});
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

		if (mcpBridge != null) {
			mcpBridge.close();
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
