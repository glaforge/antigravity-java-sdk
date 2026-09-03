---
name: antigravity-sdk-java
description: Guidelines, API reference, patterns, and best practices for building, configuring, hosting, and executing AI agents in Java using the Antigravity SDK for Java (Java 21). Use when creating Java AI agents, setting up custom tools with @Tool, configuring security policies, using reactive streams, handling MCP servers, setting up lifecycle hooks, or handling multimodal inputs.
license: Apache-2.0
---

# Antigravity SDK for Java

The **Antigravity SDK for Java** enables enterprise Java developers to build, configure, host, and execute AI agents natively in **Java 21**. It wraps the native `localharness` binary over WebSockets, supporting streaming, tool calling, Model Context Protocol (MCP), lifecycle hooks, security policies, and multimodal inputs.

## Prerequisites & Authentication Setup

Before executing tasks with the Antigravity Java SDK, verify the environment:

- **Check Dependencies**: Ensure `io.github.glaforge:antigravity-sdk-wrapper` (and `antigravity-sdk-protocol`) are listed in `pom.xml`.
- **API Key Setup**: A valid `GEMINI_API_KEY` environment variable is required to access Gemini models.
  - If credentials are missing, actively help the user get set up by providing the Google AI Studio link: `https://aistudio.google.com/app/api-keys`.
- **Vertex AI (Gemini Enterprise Agent Platform)**: Uses Application Default Credentials (ADC). Instruct the user to run `gcloud auth application-default login` and set environment variables `GOOGLE_CLOUD_PROJECT` and `GOOGLE_CLOUD_LOCATION`.

## Routing Table

Use the following reference guide based on the user prompt:

- **Core API & Multimodal**: For `AgentConfig`, MCP servers, multimodal inputs (`AgentInput.Audio`, `AgentInput.Image`), `RetryConfig`, `DebugConfig`, or `BuiltinTools`, read [API Reference](references/api-reference.md).
- **Security & Hooks**: For policy rules (`allowTools`, `denyIf`, `askUser`), `PreTurnHook`, `PreToolCallDecideHook`, or `OnToolErrorHook` with `ToolExecutionError`, read [Security Policies & Lifecycle Hooks](references/security-and-hooks.md).
- **Streaming & Reactive**: For `Flow.Publisher`, Spring WebFlux / RxJava 3 integration, or streaming internal thoughts via `AgentStream`, read [Streaming & Reactive Integration](references/streaming-and-reactive.md).

---

## Quick Start

### Basic Agent Execution

Always use Java 21 `try-with-resources` to ensure the underlying `localharness` process is closed cleanly.

```java
import io.github.glaforge.antigravity.Agent;
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.AgentResponse;
import java.util.concurrent.TimeUnit;

AgentConfig config = AgentConfig.builder()
    .instructions("You are a helpful software architecture assistant.")
    .build();

try (Agent agent = new Agent(config)) {
    AgentResponse response = agent.chat("Explain the repository pattern in Java.")
        .get(120, TimeUnit.SECONDS);
    System.out.println(response.text());
}
```

## Core Workflows

### 1. Tool Declaration

Prefer annotated tools (`@Tool` and `@Param`). The SDK auto-generates JSON Schemas from Java reflection.

```java
import io.github.glaforge.antigravity.tools.Tool;
import io.github.glaforge.antigravity.tools.Param;

public class DatabaseTools {
    @Tool(name = "query_user", description = "Fetch user record by email.")
    public String queryUser(
        @Param(name = "email", description = "User's primary email address") String email
    ) {
        return "User record for " + email + ": [Role: Admin, Active: true]";
    }
}

// Register tool with AgentConfig
AgentConfig config = AgentConfig.builder()
    .instructions("Use database tools to fetch account details when asked.")
    .addTool(new DatabaseTools())
    .build();
```

See [API Reference](references/api-reference.md) for dynamic tools and structured output records.

### 2. Security Policies

Policies restrict agent tool execution. Enforce a **Deny-by-Default** posture for production environments.

```java
import io.github.glaforge.antigravity.Policies;

AgentConfig config = AgentConfig.builder()
    .instructions("Safe operational agent.")
    // 1. Block destructive operations explicitly
    .addPolicy(Policies.denyIf((toolName, args) -> 
        "run_command".equals(toolName) && args.path("command_line").asText().contains("rm -rf")))
    // 2. Allow known safe tools
    .addPolicy(Policies.allowTools("query_user", "get_status"))
    // 3. Fallback: Deny all unhandled tools
    .addPolicy(Policies.denyAll())
    .build();
```

See [Security Policies & Lifecycle Hooks](references/security-and-hooks.md) for interactive user confirmation policies and Protobuf `PolicyConfig` wire definitions.

### 3. Response Streaming

Stream text deltas as they arrive via functional callbacks or Java 9 Reactive Streams (`Flow.Publisher`).

```java
try (Agent agent = new Agent(config)) {
    agent.chatStream("Write a microservice specification.", chunk -> {
        System.out.print(chunk.textDelta());
    }).get(120, TimeUnit.SECONDS);
}
```

See [Streaming & Reactive Integration](references/streaming-and-reactive.md) for Spring WebFlux / RxJava 3 integration and `AgentStream` thought interception.

### 4. Retry Configuration & Audio Input (v0.1.9)

Configure exponential retries for transient API errors & model outputs using `RetryConfig`. Pass audio input directly to agents for meeting summary workflows.

```java
import io.github.glaforge.antigravity.RetryConfig;
import io.github.glaforge.antigravity.AgentInput;

// Configure agent to automatically retry transient API errors with backoff
AgentConfig config = AgentConfig.builder()
    .instructions("Analyze meeting recordings and provide a concise summary.")
    .retryConfig(RetryConfig.benchmark())
    .build();

// Build an agent snippet that takes an audio recording of a meeting and streams a summary back
byte[] audioData = Files.readAllBytes(Path.of("meeting.mp3"));
AgentInput.Audio audioInput = new AgentInput.Audio("audio/mp3", audioData, "Q3 planning meeting");

try (Agent agent = new Agent(config)) {
    agent.chatStream(audioInput, chunk -> System.out.print(chunk.textDelta()))
         .get(120, TimeUnit.SECONDS);
}
```

### 5. Structured Tool Exception Handling & Recovery (v0.1.9)

Catch tool execution errors programmatically via `ToolExecutionError` in `OnToolErrorHook` to safely recover when a tool fails.

```java
import io.github.glaforge.antigravity.ToolExecutionError;
import io.github.glaforge.antigravity.BuiltinTools;

AgentConfig config = AgentConfig.builder()
    .addOnToolErrorHook((call, err, ctx) -> {
        if (err instanceof ToolExecutionError tee) {
            System.err.println("Tool execution failed on tool: " + tee.getToolName());
        }
        return CompletableFuture.completedFuture("Safely recovered from tool error");
    })
    .build();
```

### 6. Session Budget Limits & Autonomous Behavior (v0.1.12)

Enforce strict model call, tool call, and token caps using `BudgetConfig`, choose `AgentBehavior` mode, set inference `ServiceTier`, and inspect fine-grained token usage breakdown by `Modality`.

```java
import io.github.glaforge.antigravity.BudgetConfig;
import io.github.glaforge.antigravity.AgentBehavior;
import io.github.glaforge.antigravity.ServiceTier;
import io.github.glaforge.antigravity.GenerationConfig;

BudgetConfig budget = BudgetConfig.builder()
    .maxModelCalls(10)
    .maxToolCalls(20)
    .maxInputTokens(40_000L)
    .maxOutputTokens(8_000L)
    .build();

AgentConfig config = AgentConfig.builder()
    .instructions("Autonomous assistant running with strict budget caps.")
    .budgetConfig(budget)
    .agentBehavior(AgentBehavior.AUTONOMOUS)
    .generation(GenerationConfig.builder()
        .serviceTier(ServiceTier.PRIORITY)
        .build())
    .build();
```

### 7. Run Command Options & Workspace Containment (v0.1.13)

Configure daemon commands and timeouts via `RunCommandConfig`, enforce strict filesystem containment with `WorkspaceContainment`, correlate trajectory steps (`stepId`), and rewrite tool arguments in `PreToolCallDecideHook`.

```java
import io.github.glaforge.antigravity.RunCommandConfig;
import io.github.glaforge.antigravity.WorkspaceContainment;
import io.github.glaforge.antigravity.hooks.HookResult;

RunCommandConfig runCmd = RunCommandConfig.builder()
    .enableDaemons(true)
    .timeoutSeconds(120.0)
    .build();

AgentConfig config = AgentConfig.builder()
    .instructions("Secure assistant with workspace containment.")
    .capabilities(CapabilitiesConfig.builder().enableShell(true).runCommandConfig(runCmd).build())
    .workspaceContainment(WorkspaceContainment.ENABLED)
    .addPreToolCallDecideHook((toolCall, ctx) -> {
        // Rewrite tool arguments dynamically
        if ("run_command".equals(toolCall.name())) {
            return CompletableFuture.completedFuture(
                HookResult.allowedWithModifiedArguments("{\"command_line\": \"echo safe\"}")
            );
        }
    .build();
```

### 8. Compaction Hooks & Trajectory Trace Context (v0.1.14)

Intercept context compaction notifications with `OnCompactionHook`, inspect trajectory termination reasons (`StopReason`) and depth hierarchy (`parentTrajectoryId`, `depth`), and correlate call IDs and step indices across hook events.

```java
AgentConfig config = AgentConfig.builder()
    .addOnCompactionHook((compactionArgs, ctx) -> {
        System.out.println("Compaction triggered on trajectory " + compactionArgs.getTrajectoryId() 
            + " step " + compactionArgs.getStepIndex());
        return CompletableFuture.completedFuture(null);
    })
    .build();
```

### 9. Lightweight Mode, Command Sandboxing & Stop Hooks (v0.1.16)

Agents now default to `gemini-3.8-flash`. Configure lightweight agents for local/small models using `.lightweight()`, sandbox shell executions, and handle termination events with `OnStopHook`.

```java
AgentConfig config = AgentConfig.builder()
    .instructions("Lightweight agent with command sandboxing.")
    .lightweight() // Sets MINIMAL behavior mode and lightweight toolset
    .capabilities(CapabilitiesConfig.builder()
        .enableShell(true)
        .runCommandConfig(RunCommandConfig.builder().enableSandbox(true).build())
        .build())
    .addOnStopHook((stopArgs, ctx) -> {
        System.out.println("Agent stopped: " + stopArgs.getStopReason());
        return CompletableFuture.completedFuture(null);
    })
    .build();
```

---

## Detailed References

For specialized configurations and detailed API breakdowns:

- [API Reference](references/api-reference.md) — `AgentConfig` options, MCP servers, background triggers, multimodal inputs (`AgentInput`), structured output `record`s, and `BudgetConfig` / `AgentBehavior`.
- [Security Policies & Lifecycle Hooks](references/security-and-hooks.md) — Three-tier hook framework (`PreTurnHook`, `PreToolCallDecideHook`, `OnToolErrorHook`, `OnInteractionHook`) and security policy evaluation.
- [Streaming & Reactive Integration](references/streaming-and-reactive.md) — Reactive Streams (`Flow.Publisher`), Project Reactor/RxJava interop, and `AgentStream` internal thought channels.

---

## Gotchas & Best Practices

- **Harness Process Lifecycle**: `Agent` implements `AutoCloseable`. Always wrap `Agent` in `try-with-resources` or explicitly invoke `agent.close()`. Leaving agents unclosed orphan background Go processes.
- **Asynchronous Execution**: `agent.chat()` returns `CompletableFuture<AgentResponse>`. Always specify explicit timeouts when calling `.get(timeout, unit)` to avoid deadlocks.
- **Testing Assertions**: In JUnit tests, **never use `Thread.sleep()`** to wait for asynchronous agent responses. Use `Awaitility`:
  ```java
  await().atMost(120, TimeUnit.SECONDS).until(future::isDone);
  ```
- **Policy Order Sensitivity**: Policies evaluate strictly in insertion order. Place restrictive `denyIf` or `askUser` rules *before* `allowTools` or `denyAll`.
- **Data Carrier Records**: Always represent custom tool parameter DTOs or structured outputs as modern **Java 21 `record`** types for immutability and automatic schema reflection.
- **Thinking Token Overhead**: Extended reasoning models generate thinking tokens that count towards usage (`usageMetadata().thoughtsTokenCount()`). Monitor token counts when setting high `ThinkingLevel`.
- **Observability & Wire Logs**: Use `DebugConfig.defaults()` or `.debugConfig(new DebugConfig(true, "DEBUG"))` in `AgentConfig` to enable client logging and server-side distributed tracing.
- **No Fully Qualified Names (FQNs)**: Maintain clean imports at top of Java files (`import java.util.List;`) rather than inline FQNs.
