# API Reference — Antigravity SDK for Java

This document details configuration options, agent skills, tool definitions, MCP integration, local model configs, triggers, multimodal capabilities, and structured output patterns in the Antigravity SDK for Java.

---

## 1. Agent Configuration (`AgentConfig` & `Agent.builder()`)

`AgentConfig` is the primary configuration object passed to `new Agent(config)`. Alternatively, you can use the fluent `Agent.builder()` directly.

```java
import io.github.glaforge.antigravity.Agent;
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.CapabilitiesConfig;
import io.github.glaforge.antigravity.GenerationConfig;
import io.github.glaforge.antigravity.ThinkingLevel;
import java.util.Map;

// Option A: AgentConfig.builder()
AgentConfig config = AgentConfig.builder()
    .instructions("System instructions for the agent model.")
    .modelName("gemini-3.8-flash") // Default model selection
    .conversationId("session-123") // Resume existing session context
    .environmentVariables(Map.of("CUSTOM_ENV_VAR", "value")) // Custom process environment
    .addSkillPath("/path/to/my-agent-skill") // Register file-based agent skill
    .generation(GenerationConfig.builder()
        .temperature(0.2)
        .maxOutputTokens(2048)
        .thinkingLevel(ThinkingLevel.EXTRA_HIGH) // Configure "extra_high" reasoning severity
        .build())
    .capabilities(CapabilitiesConfig.builder()
        .enableWebSearch(true)
        .enableUrlReading(true)
        .enableShell(true)
        .enableWriteFile(true)
        .enableFileEdit(true)
        .enableListDir(true)
        .enableGrepSearch(true)
        .enableGenerateImage(true) // Enable image generation capability
        .allowUserQuestions(true)  // Enable asking clarifying questions
        .enableSubagents(true)
        .build())
    .build();

// Option B: Fluent Agent.builder() directly
Agent agent = Agent.builder()
    .instructions("System instructions for the agent model.")
    .modelName("gemini-3.8-flash")
    .addSkillPath("/path/to/my-agent-skill")
    .build();
```

### Platform Resolution & Harness Binary Configuration
 
The SDK uses a **hybrid architecture** to manage the native Go `localharness` binary across Linux (x86_64 and ARM64), macOS (Apple Silicon and Intel), and Windows (x86_64 and ARM64):
 
* **1. Custom Binary Override**: Checks `ANTIGRAVITY_HARNESS_PATH` environment variable or `-Dantigravity.harness.path=/path/to/localharness`.
* **2. Local Cache**: Checks `~/.antigravity/bin/<slice>/localharness` and its `.version` stamp.
* **3. Embedded Classpath (Offline / Air-Gapped)**: If `antigravity-sdk-harness` (matching the platform classifier) is on the classpath, extracts the embedded binary directly without network access.
* **4. Automatic On-Demand Download**: If not cached or bundled, `HarnessDownloader` streams the platform-specific native binary (~35-43 MB compressed) directly from upstream PyPI wheels into `~/.antigravity/bin/<slice>/` in ~1-2 seconds.
* **Control Flags**:
  - `antigravity.harness.download=false`: Disables remote downloads entirely (throws `FileNotFoundException` if binary is missing from cache and classpath).
  - `antigravity.harness.path`: Direct path to native binary override.

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

### ToolContext Parameter Injection

Any `@Tool` method can declare a `ToolContext` parameter. The SDK runtime automatically injects the context and excludes it from the tool's JSON schema sent to the model:

```java
import io.github.glaforge.antigravity.ToolContext;
import io.github.glaforge.antigravity.tools.Tool;
import io.github.glaforge.antigravity.tools.Param;

public class UserSessionTools {
    @Tool(name = "set_user_preference", description = "Store user theme and locale preferences.")
    public String setPreference(
        @Param(name = "theme", description = "Color theme (light or dark)") String theme,
        ToolContext context // Injected automatically by SDK
    ) {
        // 1. Read or update session-scoped state
        context.setState("theme", theme);

        // 2. Access the active conversation ID
        String conversationId = context.getConversationId();

        // 3. Proactively push a message back into the conversation
        context.send("Preference updated to " + theme);

        return "Preference saved for conversation: " + conversationId;
    }
}
```

---

## 4. Built-in Capabilities Matrix (`CapabilitiesConfig`)

Instead of defining custom tools, you can enable native built-in capabilities implemented directly by the underlying Go harness:

```java
import io.github.glaforge.antigravity.CapabilitiesConfig;
import io.github.glaforge.antigravity.RunCommandConfig;

CapabilitiesConfig capabilities = CapabilitiesConfig.builder()
    .enableWebSearch(true)
    .enableUrlReading(true)
    .enableShell(true)
    .runCommandConfig(RunCommandConfig.builder().enableDaemons(true).build())
    .enableViewFile(true)
    .enableWriteFile(true)
    .enableFileEdit(true)
    .enableListDir(true)
    .enableGrepSearch(true)
    .enableGenerateImage(true)
    .imageModelName("gemini-3.1-flash-lite-image") // Default image model
    .allowUserQuestions(true)
    .enableSubagents(true)
    .build();
```

| Builder Method | Built-in Tool | Description |
| :--- | :--- | :--- |
| `.enableWebSearch(true)` | `search_web` | Searches Google for real-time web results and citations. |
| `.enableUrlReading(true)` | `read_url_content` | Fetches web page content via HTTP converted to markdown. |
| `.enableShell(true)` | `run_command` | Executes shell commands in the terminal (configurable with `RunCommandConfig`). |
| `.enableViewFile(true)` | `view_file` | Views local text, PDF, image, audio, or video files with slice offsets. |
| `.enableWriteFile(true)` | `write_to_file` | Creates or replaces files in the workspace. |
| `.enableFileEdit(true)` | `replace_file_content` | Applies surgical text replacements to existing files. |
| `.enableListDir(true)` | `list_dir` | Recursively lists directory contents, file sizes, and children counts. |
| `.enableGrepSearch(true)` | `grep_search` | Ripgrep pattern matching over files and directories. |
| `.enableGenerateImage(true)` | `generate_image` | Generates images using Imagen/Gemini models (customizable with `.imageModelName()`). |
| `.allowUserQuestions(true)` | `ask_question` | Allows the agent to ask the user clarifying multi-choice questions. |
| `.enableSubagents(true)` | `invoke_subagent`, `define_subagent`, etc. | Allows the agent to define, spawn, and delegate tasks to subagents. |

---

## 5. Multi-Threaded State Management (`SessionContext`)

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

## 6. Structured Outputs

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

## 7. Model Context Protocol (MCP)

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

## 8. Agent Skills

Extend your agent with specialized domain knowledge, complex workflows, and contextual guidelines by loading file-based skills conforming to the open [Agent Skills specification](https://agentskills.io/specification).

### Registering Skills

Skill directories can be registered via `AgentConfig.builder()` or directly on `Agent.builder()`:

```java
import io.github.glaforge.antigravity.Agent;
import io.github.glaforge.antigravity.AgentConfig;
import java.util.concurrent.TimeUnit;

AgentConfig config = AgentConfig.builder()
    .instructions("You are a specialized enterprise assistant.")
    .addSkillPath("/path/to/my-agent-skill")
    .addSkillPath("skills/antigravity-sdk-java") // Relative or absolute directory path
    .build();

try (Agent agent = new Agent(config)) {
    agent.chat("How do I configure security policies in the Antigravity Java SDK?")
        .get(120, TimeUnit.SECONDS);
}
```

Or via `Agent.builder()`:

```java
Agent agent = Agent.builder()
    .instructions("You are a specialized enterprise assistant.")
    .addSkillPath("/path/to/my-agent-skill")
    .build();
```

### Skill Directory Structure

A valid skill directory must follow the open Agent Skills standard:

```
my-agent-skill/
├── SKILL.md                 # Required: YAML frontmatter (name, description) + instructions
├── references/              # Optional: Technical documentation, API specs, deep-dive docs
├── scripts/                 # Optional: Automation scripts or helper utilities
└── resources/               # Optional: Templates, configuration files, or asset files
```

The underlying Go `localharness` binary indexes the `SKILL.md` frontmatter at startup and dynamically routes to and reads skill content as relevant to the user query during conversation turns.

> [!TIP]
> **Bundled SDK Agent Skill**: This repository includes an official, open-specification [Agent Skill](skills/antigravity-sdk-java/SKILL.md) under [`skills/antigravity-sdk-java/`](skills/antigravity-sdk-java/SKILL.md).
> You can load this skill into your agents (`.addSkillPath("skills/antigravity-sdk-java")`) or register it with AI coding tools (such as the Antigravity CLI, Cursor, Windsurf, or Claude Code) to provide your AI assistants with native expertise on configuring, hosting, and executing agents with this SDK!

---

## 9. Background Triggers

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

## 10. Multimodal & Slash Command Inputs (`AgentInput`)

Pass text, PDF documents, video, audio, images, or CLI slash commands to the agent using strongly-typed `AgentInput` records:

### Multimodal Inputs (`AgentInput.Media`)

```java
import io.github.glaforge.antigravity.AgentInput;
import java.nio.file.Path;

AgentResponse response = agent.chat(
    AgentInput.Text.of("Analyze the financial chart in this PDF and compare it to the video recording."),
    AgentInput.Document.fromFile(Path.of("q4_financials.pdf")), // Reads PDF as application/pdf
    AgentInput.Video.fromFile(Path.of("presentation.mp4")),      // Reads MP4 video
    AgentInput.Audio.fromFile(Path.of("voice_memo.mp3")),        // Reads MP3 audio
    AgentInput.Image.fromFile(Path.of("architecture.png"))       // Reads PNG image
).get(120, TimeUnit.SECONDS);
```

### Slash Commands (`AgentInput.SlashCommand`)

Execute CLI slash commands (e.g. `/help`, `/clear`) either directly via string shorthand or with `AgentInput.SlashCommand`:

```java
try (Agent agent = new Agent(config)) {
    // String shorthand
    AgentResponse helpResponse = agent.chat("/help").get(120, TimeUnit.SECONDS);
    System.out.println(helpResponse.text());

    // Strongly-typed SlashCommand record
    agent.chat(AgentInput.SlashCommand.of("/clear")).get(120, TimeUnit.SECONDS);
}
```

---

## 11. Multi-Turn Session Persistence & Turn Cancellation

### Session Persistence (`conversationId`)

To maintain conversation memory across application restarts or HTTP requests:

```java
String conversationId;

// Session 1: Run turn and retrieve conversation ID
try (Agent agent = new Agent(config)) {
    agent.chat("Remember: My favorite database is Spanner.").get(120, TimeUnit.SECONDS);
    conversationId = agent.getConversationId(); // Save ID in persistent database
}

// Session 2: Resume previous conversation context
AgentConfig resumedConfig = AgentConfig.builder()
    .instructions("Helpful assistant.")
    .conversationId(conversationId)
    .build();

try (Agent agent = new Agent(resumedConfig)) {
    AgentResponse response = agent.chat("What is my favorite database?").get(120, TimeUnit.SECONDS);
    System.out.println(response.text()); // "Spanner"
}
```

### Execution Cancellation (`agent.cancel()`)

Cancel long-running turns from another thread or HTTP abort signal. The pending turn future completes exceptionally with `AgentCancelledException`:

```java
import io.github.glaforge.antigravity.AgentCancelledException;
import java.util.concurrent.ExecutionException;

try (Agent agent = new Agent(config)) {
    CompletableFuture<AgentResponse> future = agent.chat("Generate an exhaustive report.");

    // Trigger cancellation
    agent.cancel();

    try {
        future.get(120, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
        if (e.getCause() instanceof AgentCancelledException) {
            System.out.println("Execution was successfully cancelled.");
        }
    }
}
```

---

## 12. Retry, Observability & Tool Error Configurations (v0.1.9)

### Model Retry Configuration (`RetryConfig`)

```java
import io.github.glaforge.antigravity.RetryConfig;

// Use preset benchmark exponential backoff (5 retries for API, 3 retries for invalid output)
AgentConfig config = AgentConfig.builder()
    .retryConfig(RetryConfig.benchmark())
    .build();
```

### Connection Tracing & Logging (`DebugConfig`)

```java
import io.github.glaforge.antigravity.DebugConfig;

AgentConfig config = AgentConfig.builder()
    .debugConfig(new DebugConfig(true, "DEBUG"))
    .build();
```

### Builtin Tools & Structured Exceptions (`BuiltinTools`, `ToolExecutionError`)

```java
import io.github.glaforge.antigravity.BuiltinTools;
import io.github.glaforge.antigravity.ToolExecutionError;

// Inspect builtin tool classifications
List<BuiltinTools> readOnlyTools = BuiltinTools.readOnly();
List<BuiltinTools> safeTools = BuiltinTools.nondestructive();

// Structured exception handling in hooks
AgentConfig config = AgentConfig.builder()
    .addOnToolErrorHook((call, err, ctx) -> {
        if (err instanceof ToolExecutionError tee) {
            String failingTool = tee.getToolName();
            String argsJson = tee.getArgumentsJson();
            System.err.println("Execution failed for tool: " + failingTool + " with args: " + argsJson);
        }
        return CompletableFuture.completedFuture("Recovered safely");
    })
    .build();
```

---

## 13. Session Budget, Behavior & Multimodal Usage Breakdown (v0.1.12)

### Session Budget Configuration (`BudgetConfig`)

```java
import io.github.glaforge.antigravity.BudgetConfig;

BudgetConfig budget = BudgetConfig.builder()
    .maxModelCalls(10)
    .maxToolCalls(30)
    .maxInputTokens(100_000L)
    .maxOutputTokens(20_000L)
    .maxTotalTokens(120_000L)
    .build();

AgentConfig config = AgentConfig.builder()
    .budgetConfig(budget)
    .build();
```

### Agent Behavior (`AgentBehavior`) & Service Tier (`ServiceTier`)

```java
import io.github.glaforge.antigravity.AgentBehavior;
import io.github.glaforge.antigravity.ServiceTier;
import io.github.glaforge.antigravity.GenerationConfig;

AgentConfig config = AgentConfig.builder()
    .agentBehavior(AgentBehavior.AUTONOMOUS)
    .generation(GenerationConfig.builder()
        .serviceTier(ServiceTier.PRIORITY)
        .build())
    .build();
```

### Multimodal Token Usage Breakdown (`UsageMetadata`, `ModalityTokenCount`)

```java
UsageMetadata usage = response.usage();
if (usage != null) {
    System.out.println("Service tier: " + usage.serviceTier());
    System.out.println("Cached tokens: " + usage.cachedContentTokenCount());
    System.out.println("Thoughts tokens: " + usage.thoughtsTokenCount());

    for (ModalityTokenCount detail : usage.promptTokensDetails()) {
        System.out.println("Prompt modality: " + detail.modality() + " -> " + detail.tokenCount() + " tokens");
    }
}
```

---

## 14. Run Command Options, Workspace Containment & Step Correlation (v0.1.13)

### Run Command Tool Configuration (`RunCommandConfig`)

```java
import io.github.glaforge.antigravity.RunCommandConfig;
import io.github.glaforge.antigravity.CapabilitiesConfig;

RunCommandConfig runConfig = RunCommandConfig.builder()
    .enableDaemons(true)
    .timeoutSeconds(180.0)
    .build();

CapabilitiesConfig capabilities = CapabilitiesConfig.builder()
    .enableShell(true)
    .runCommandConfig(runConfig)
    .build();
```

### Workspace Containment Policy (`WorkspaceContainment`)

```java
import io.github.glaforge.antigravity.WorkspaceContainment;

AgentConfig config = AgentConfig.builder()
    .instructions("Contain all operations strictly within the configured workspace directory.")
    .workspaceContainment(WorkspaceContainment.ENABLED)
    .build();
```

### Step Correlation & Argument Modification in Hooks (`ToolCall`, `HookResult`, `ToolExecutionError`)

```java
import io.github.glaforge.antigravity.hooks.ToolCall;
import io.github.glaforge.antigravity.hooks.HookResult;
import io.github.glaforge.antigravity.ToolExecutionError;

AgentConfig config = AgentConfig.builder()
    .addPreToolCallDecideHook((toolCall, ctx) -> {
        String stepId = toolCall.stepId(); // E.g. "traj_main:3"
        String callId = toolCall.id();
        String server = toolCall.serverName();
        
        // Rewrite arguments dynamically
        if ("view_file".equals(toolCall.name())) {
            return CompletableFuture.completedFuture(
                HookResult.allowedWithModifiedArguments("{\"file_path\": \"sanitized_path.txt\"}")
            );
        }
        return CompletableFuture.completedFuture(HookResult.allowed());
    })
    .addOnToolErrorHook((toolCall, error, ctx) -> {
        if (error instanceof ToolExecutionError tee) {
            System.err.println("Failure on step: " + tee.getStepId() + " in server: " + tee.getServerName());
        }
        return CompletableFuture.completedFuture("Handled error");
    })
    .build();
```

### Trajectory Metadata & Termination Reasons (`StopReason`, `OnCompactionArgs`) (v0.1.14)

```java
import io.github.glaforge.antigravity.localharness.TrajectoryStateUpdate.StopReason;
import io.github.glaforge.antigravity.localharness.OnCompactionArgs;

// StopReason enum values returned when turn budget or limits are exceeded:
// StopReason.STOP_REASON_MAX_MODEL_CALLS_EXCEEDED
// StopReason.STOP_REASON_MAX_TOOL_CALLS_EXCEEDED
// StopReason.STOP_REASON_MAX_INPUT_TOKENS_EXCEEDED
// StopReason.STOP_REASON_MAX_OUTPUT_TOKENS_EXCEEDED
// StopReason.STOP_REASON_MAX_TOTAL_TOKENS_EXCEEDED
// StopReason.STOP_REASON_QUOTA_EXHAUSTED
```

### Lightweight Configuration & Shell Sandboxing (`AgentConfig.lightweight()`, `RunCommandConfig.enableSandbox`) (v0.1.16)

```java
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.RunCommandConfig;
import io.github.glaforge.antigravity.hooks.OnStopHook;

// 1. Configure lightweight agent for local LiteRT models with sandboxed shell execution
AgentConfig config = AgentConfig.builder()
    .instructions("Lightweight agent with sandboxed command execution.")
    .lightweight() // Applies minimal behavior mode and safe local tools
    .capabilities(CapabilitiesConfig.builder()
        .enableShell(true)
        .runCommandConfig(RunCommandConfig.builder().enableSandbox(true).build())
        .build())
    .addOnStopHook((stopArgs, ctx) -> {
        System.out.println("Stopped: " + stopArgs.getStopReason());
        return CompletableFuture.completedFuture(null);
    })
    .build();
```

### Forward-Looking Budgets, Compaction Limits & Token Arithmetic (v0.1.17)

```java
import io.github.glaforge.antigravity.BudgetConfig;
import io.github.glaforge.antigravity.BudgetScope;
import io.github.glaforge.antigravity.CompactionConfig;
import io.github.glaforge.antigravity.BuiltinTools;
import io.github.glaforge.antigravity.SandboxStatus;

// 1. Configure forward-looking budget and context compaction
AgentConfig config = AgentConfig.builder()
    .instructions("Agent with forward-looking budget and context compaction.")
    .budgetConfig(BudgetConfig.builder()
        .maxModelCalls(10)
        .maxTotalTokens(50_000L)
        .scope(BudgetScope.FORWARD_LOOKING) // Evaluates caps only against newly executed turns
        .build())
    .compactionConfig(CompactionConfig.builder()
        .tokenThreshold(16_000)
        .checkpointIntervalTokens(4_000)
        .maxContextTokens(32_000)
        .build())
    .build();

// 2. Token usage arithmetic on UsageMetadata
UsageMetadata combined = turn1.usage().add(turn2.usage()); // Or turn1.usage().plus(turn2.usage())
UsageMetadata diff = combined.subtract(baseline.usage());   // Or minus(...)
UsageMetadata projected = combined.multiply(1.2);          // Or times(...)

// 3. Builtin tool convenience collections
List<BuiltinTools> defaults = BuiltinTools.defaultTools(); // Excludes ASK_QUESTION for headless/autonomous agents
List<BuiltinTools> minimal = BuiltinTools.minimal();       // Core software engineering tools

// 4. OS Command Sandbox verification
try (Agent agent = new Agent(config)) {
    SandboxStatus sandbox = agent.getSandboxStatus();
    if (sandbox != null && !sandbox.available()) {
        System.err.println("Warning: Sandbox is not enforcing: " + sandbox.unavailableReason());
    }
}
```

---

## 15. Harness Process Logging & Java 24+ Runtimes

### SLF4J Harness Diagnostics
The embedded `localharness` stdout and stderr are consumed line-by-line and routed through SLF4J:
- Informational output (`glog` INFO, CDP discovery, permission checks) -> `log.debug(...)`
- Warnings -> `log.warn(...)`
- Fatal errors -> `log.error(...)`

To view raw harness diagnostics, configure `io.github.glaforge.antigravity.Agent` logger level to `DEBUG`:
```properties
org.slf4j.simpleLogger.log.io.github.glaforge.antigravity.Agent=debug
```

### Java 24+ / GraalVM Deprecation Flag
Under Java 24+ (JEP 471), protobuf memory-access via `sun.misc.Unsafe` produces a deprecation warning. Silence it by creating `.mvn/jvm.config`:
```text
--sun-misc-unsafe-memory-access=allow
```
