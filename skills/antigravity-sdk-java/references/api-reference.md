# API Reference — Antigravity SDK for Java

This document details the configuration options, tool definitions, MCP integration, triggers, multimodal capabilities, and structured output patterns in the Antigravity SDK for Java.

---

## 1. Agent Configuration (`AgentConfig`)

`AgentConfig` is the primary configuration object passed to `new Agent(config)`.

```java
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.CapabilitiesConfig;
import io.github.glaforge.antigravity.GenerationConfig;

AgentConfig config = AgentConfig.builder()
    .instructions("System instructions for the agent model.")
    .model("gemini-2.5-flash") // Default model selection
    .conversationId("session-123") // Resume existing session context
    .generationConfig(GenerationConfig.builder()
        .temperature(0.2)
        .maxOutputTokens(2048)
        .build())
    .capabilities(CapabilitiesConfig.builder()
        .enableWebSearch(true)
        .enableShell(true)
        .enableWriteFile(true)
        .enableFileEdit(true)
        .enableListDir(true)
        .enableGrepSearch(true)
        .enableSubagents(true)
        .build())
    .build();
```

---

## 2. Tool Definitions

### Annotated Tools (Recommended)

Use `@Tool` on public methods and `@Param` on parameters. Primitive types, strings, records, and POJOs are automatically mapped to JSON Schema.

```java
import io.github.glaforge.antigravity.tools.Tool;
import io.github.glaforge.antigravity.tools.Param;

public class CustomerServiceTools {

    public record AccountQuery(String accountId, boolean includeTransactions) {}

    @Tool(name = "get_account_details", description = "Retrieve customer account summary.")
    public String getAccountDetails(
        @Param(name = "query", description = "Account lookup criteria") AccountQuery query
    ) {
        return "Account " + query.accountId() + " balance: $1,450.00";
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

## 3. Structured Outputs

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

## 4. Model Context Protocol (MCP)

Connect to local or remote Model Context Protocol (MCP) servers to expose external tools to the agent dynamically.

```java
import io.github.glaforge.antigravity.McpServerConfig;
import java.util.List;

McpServerConfig sqliteMcp = McpServerConfig.stdio(
    "npx",
    List.of("-y", "@modelcontextprotocol/server-sqlite", "app_data.db")
);

AgentConfig config = AgentConfig.builder()
    .instructions("You have access to a database via MCP.")
    .addMcpServer(sqliteMcp)
    .build();
```

---

## 5. Background Triggers

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

## 6. Multimodal Inputs

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
