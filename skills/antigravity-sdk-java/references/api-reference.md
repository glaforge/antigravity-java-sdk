# API Reference — Antigravity SDK for Java

This document details the configuration options, tool definitions, MCP integration, local model configs, triggers, multimodal capabilities, and structured output patterns in the Antigravity SDK for Java.

---

## 1. Agent Configuration (`AgentConfig`)

`AgentConfig` is the primary configuration object passed to `new Agent(config)`.

```java
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.CapabilitiesConfig;
import io.github.glaforge.antigravity.GenerationConfig;
import io.github.glaforge.antigravity.ThinkingLevel;
import java.util.Map;

AgentConfig config = AgentConfig.builder()
    .instructions("System instructions for the agent model.")
    .modelName("gemini-3.6-flash") // Default model selection
    .conversationId("session-123") // Resume existing session context
    .environmentVariables(Map.of("CUSTOM_ENV_VAR", "value")) // Custom process environment
    .generation(GenerationConfig.builder()
        .temperature(0.2)
        .maxOutputTokens(2048)
        .thinkingLevel(ThinkingLevel.EXTRA_HIGH) // Configure "extra_high" reasoning severity
        .build())
    .capabilities(CapabilitiesConfig.builder()
        .enableWebSearch(true)
        .enableShell(true)
        .enableWriteFile(true)
        .enableFileEdit(true)
        .enableListDir(true)
        .enableGrepSearch(true)
        .enableGenerateImage(true) // Enable image generation capability
        .enableSubagents(true)
        .build())
    .build();
```

---

## 2. Local Models & Custom Backends

### LiteRT (Local Gemma Models)

Use `LiteRTAgentConfig` to run local Gemma models via the LiteRT backend with hardware acceleration (`CPU`, `GPU`, `NPU`).

```java
import io.github.glaforge.antigravity.LiteRTAgentConfig;

LiteRTAgentConfig litertConfig = LiteRTAgentConfig.builder()
    .modelPath("/path/to/gemma.litertlm")
    .backend(LiteRTAgentConfig.Backend.GPU)
    .instructions("You are a local Gemma agent.")
    .build();

try (Agent agent = new Agent(litertConfig.getAgentConfig())) {
    // Execute local agent
}
```

### Local OpenAI Endpoints (Ollama, LM Studio)

Use `LocalOpenAIAgentConfig` to connect to local OpenAI-compatible API servers.

```java
import io.github.glaforge.antigravity.LocalOpenAIAgentConfig;

LocalOpenAIAgentConfig ollamaConfig = LocalOpenAIAgentConfig.builder()
    .baseUrl("http://localhost:11434/v1")
    .modelName("llama3")
    .instructions("Local Ollama assistant.")
    .build();

try (Agent agent = new Agent(ollamaConfig.getAgentConfig())) {
    // Execute local agent
}
```

---

## 3. Tool Definitions

### Annotated Tools (Recommended)

Use `@Tool` on public methods and `@Param` on parameters. Synchronous methods and asynchronous futures (`CompletableFuture<T>`) are supported seamlessly.

```java
import io.github.glaforge.antigravity.tools.Tool;
import io.github.glaforge.antigravity.tools.Param;
import java.util.concurrent.CompletableFuture;

public class CustomerServiceTools {

    public record AccountQuery(String accountId, boolean includeTransactions) {}

    @Tool(name = "get_account_details", description = "Retrieve customer account summary.")
    public String getAccountDetails(
        @Param(name = "query", description = "Account lookup criteria") AccountQuery query
    ) {
        return "Account " + query.accountId() + " balance: $1,450.00";
    }

    @Tool(name = "async_fetch_data", description = "Fetch data asynchronously.")
    public CompletableFuture<String> fetchAsyncData(
        @Param(name = "key", description = "Lookup key") String key
    ) {
        return CompletableFuture.completedFuture("Result for " + key);
    }
}
```

### Dynamic Tools

Implement `DynamicTool` when tools are defined at runtime without predefined Java classes.

```java
import io.github.glaforge.antigravity.DynamicTool;
import io.github.glaforge.antigravity.tools.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;

AgentConfig config = AgentConfig.builder()
    .addTool(new DynamicTool() {
        @Override
        public String getName() { return "calculate_discount"; }

        record DiscountParams(double amount, double rate) {}

        @Override
        public ToolDefinition getDefinition() {
            return ToolDefinition.builder()
                .name("calculate_discount")
                .description("Calculate discounted total.")
                .parametersSchema(DiscountParams.class)
                .build();
        }

        @Override
        public Object execute(JsonNode arguments) {
            double amount = arguments.get("amount").asDouble();
            double rate = arguments.get("rate").asDouble();
            return amount * (1.0 - rate);
        }
    })
    .build();
```

---

## 4. Multi-Threaded State Management (`SessionContext`)

`SessionContext` provides a thread-safe state store backed by `ConcurrentHashMap` with atomic update helper methods across concurrent tools and hooks.

```java
SessionContext context = new SessionContext();

// Atomic update
context.update("tool_executions", (key, oldVal) -> oldVal == null ? 1 : ((Integer) oldVal) + 1);

// Compute if absent
context.computeIfAbsent("cache_key", key -> loadDataFromDatabase(key));

// Atomic merge
context.merge("total_tokens", 150, (oldVal, newVal) -> ((Integer) oldVal) + ((Integer) newVal));
```

---

## 5. Structured Outputs

Force the agent to respond in a strict JSON schema format, deserializing directly into Java 21 `record`s.

```java
import io.github.glaforge.antigravity.Agent;
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.AgentResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record CodeAnalysis(
    String language,
    int complexityScore,
    List<String> suggestions
) {}

AgentConfig config = AgentConfig.builder()
    .instructions("Analyze the provided snippet and return structured metrics.")
    .finishToolSchema(CodeAnalysis.class)
    .build();

try (Agent agent = new Agent(config)) {
    AgentResponse response = agent.chat("Analyze public static void main...").get(120, TimeUnit.SECONDS);
    CodeAnalysis analysis = response.getStructuredOutput(CodeAnalysis.class);
    System.out.println("Language: " + analysis.language());
    System.out.println("Suggestions: " + analysis.suggestions());
}
```

---

## 6. Model Context Protocol (MCP)

Connect to local or remote Model Context Protocol (MCP) servers to expose external tools to the agent dynamically.

```java
import io.github.glaforge.antigravity.McpServerConfig;
import java.util.List;

// Standard I/O MCP Server
McpServerConfig sqliteMcp = McpServerConfig.stdio(
    "npx",
    List.of("-y", "@modelcontextprotocol/server-sqlite", "app_data.db")
);

// Server-Sent Events (SSE) MCP Server
McpServerConfig sseMcp = McpServerConfig.sse("http://localhost:8080/sse");

// Streamable HTTP / HTTP MCP Server
McpServerConfig httpMcp = McpServerConfig.streamableHttp("http://localhost:8080/mcp");

AgentConfig config = AgentConfig.builder()
    .instructions("You have access to tools via MCP.")
    .addMcpServer(sqliteMcp)
    .addMcpServer(sseMcp)
    .addMcpServer(httpMcp)
    .build();
```

---

## 7. Background Triggers

Inject recurring context updates into active agent sessions without interrupting user turns.

```java
import io.github.glaforge.antigravity.triggers.Triggers;
import java.util.concurrent.TimeUnit;

AgentConfig config = AgentConfig.builder()
    .instructions("Monitor background deployment status and notify user on completion.")
    .addTrigger(Triggers.every(30, TimeUnit.SECONDS, ctx -> {
        boolean completed = checkDeploymentStatus();
        if (completed) {
            ctx.fireTrigger("Deployment #842 completed successfully.");
        }
    }))
    .build();
```

---

## 8. Multimodal Inputs

Pass text, images, audio, or video files to the agent using `AgentInput`.

```java
import io.github.glaforge.antigravity.AgentInput;
import java.nio.file.Path;

AgentResponse response = agent.chat(
    AgentInput.Text.of("Identify architectural flaws in this diagram and audio note."),
    AgentInput.Image.fromFile(Path.of("architecture_diagram.png")),
    AgentInput.Audio.fromFile(Path.of("voice_memo.mp3"))
).get(120, TimeUnit.SECONDS);
```
