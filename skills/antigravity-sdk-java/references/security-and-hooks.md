# Security Policies & Lifecycle Hooks — Antigravity SDK for Java

This document explains how to restrict tool execution with security policies and intercept agent execution turns using lifecycle hooks.

---

## 1. Security Policies (`Policies`)

Security policies evaluate every tool execution requested by the agent. They are evaluated strictly in the order they are registered in `AgentConfig`.

### Built-in Policy Factories

| Policy Method | Description |
| :--- | :--- |
| `Policies.denyAll()` | Denies all tool executions unconditionally. |
| `Policies.allowTools(names...)` | Explicitly permits named tools. |
| `Policies.denyIf(predicate)` | Evaluates a custom `BiPredicate<String, JsonNode>` returning `true` to block execution. |
| `Policies.askUser(predicate)` | Prompts interactive user confirmation when predicate evaluates to `true`. |

### Recommended Production Posture

```java
import io.github.glaforge.antigravity.AgentConfig;
import io.github.glaforge.antigravity.Policies;
import java.util.Scanner;

AgentConfig config = AgentConfig.builder()
    .instructions("Secure system maintenance agent.")

    // Rule 1: Hard deny dangerous shell commands
    .addPolicy(Policies.denyIf((toolName, args) -> {
        if ("run_command".equals(toolName) && args.has("command_line")) {
            String cmd = args.get("command_line").asText();
            return cmd.contains("drop database") || cmd.contains("rm -rf");
        }
        return false;
    }))

    // Rule 2: Interactive user confirmation for sensitive files
    .addPolicy(Policies.askUser((toolName, args) -> {
        if ("view_file".equals(toolName) && args.has("path")) {
            String path = args.get("path").asText();
            if (path.contains("credentials.env")) {
                System.out.print("⚠️ Agent requested sensitive file: " + path + ". Allow? (y/n): ");
                Scanner scanner = new Scanner(System.in);
                return scanner.nextLine().trim().equalsIgnoreCase("y");
            }
        }
        return true; // Auto-pass non-sensitive files
    }))

    // Rule 3: Explicit allowlist for standard tools
    .addPolicy(Policies.allowTools("query_user", "get_status"))

    // Rule 4: Fallback deny-all for all other tools
    .addPolicy(Policies.denyAll())

    .build();
```

---

### Protobuf Policy Wire Definitions (`PolicyConfig`)

The underlying protocol module (`antigravity-sdk-protocol`) provides Protobuf wire classes matching upstream Antigravity specifications:

* `PolicyConfig`: Contains `repeated PolicyRule rules`.
* `PolicyRule`: Specifies `tool`, `server_name`, `name`, `decision` (`PolicyDecision`), `deny_reason`, `is_dynamic`, and `rule_id`.
* `PolicyDecisionRequest` / `PolicyDecisionResponse`: Handlers for asynchronous policy evaluation events streamed via WebSockets.

---

## 2. Lifecycle Hooks

Lifecycle hooks allow applications to monitor, gate, or sanitize agent turns and tool calls.

The SDK classifies hooks into three distinct architectural categories:

```
                  ┌──────────────────────────────────────────────┐
                  │              Hook Architecture               │
                  └──────────────────────┬───────────────────────┘
                                         │
       ┌─────────────────────────────────┼─────────────────────────────────┐
       ▼                                 ▼                                 ▼
 ┌───────────┐                     ┌───────────┐                     ┌───────────┐
 │  Inspect  │                     │  Decide   │                     │ Transform │
 │ (Async/RO)│                     │(Blocking) │                     │ (Modifying)│
 └─────┬─────┘                     └─────┬─────┘                     └─────┬─────┘
       │                                 │                                 │
       ├─► PostTurnHook                  ├─► PreTurnHook                   ├─► OnToolErrorHook
       ├─► PostToolCallHook              └─► PreToolCallDecideHook         └─► OnInteractionHook
       ├─► OnSessionStartHook
       ├─► OnSessionEndHook
       └─► OnCompactionHook
```

### Hook Classification Summary

1. **Inspect Hooks**: Read-Only, Non-blocking (`CompletableFuture<Void>`). Ideal for auditing, telemetry, and logging.
2. **Decide Hooks**: Read-Only, Blocking (`CompletableFuture<HookResult>`). Evaluates permissions and decision gates before turns or tools execute.
3. **Transform Hooks**: Modifying, Blocking (`CompletableFuture<HookResult>`). Alters inputs/arguments in-flight or recovers from failures.

---

## 3. Hook Implementations

### Pre-Turn Gate (`PreTurnHook`) — Decide

Block or permit an entire turn before LLM execution.

```java
import io.github.glaforge.antigravity.hooks.HookResult;
import java.util.concurrent.CompletableFuture;

AgentConfig config = AgentConfig.builder()
    .addPreTurnHook((prompt, context) -> {
        if (prompt.contains("UNAUTHORIZED_KW")) {
            return CompletableFuture.completedFuture(HookResult.denied("Prompt contained blocked keyword."));
        }
        return CompletableFuture.completedFuture(HookResult.allowed());
    })
    .build();
```

### Pre-Tool Call Transformation (`PreToolCallDecideHook`) — Transform

Sanitize or rewrite tool arguments before dispatch.

```java
import io.github.glaforge.antigravity.hooks.HookResult;
import java.util.concurrent.CompletableFuture;

AgentConfig config = AgentConfig.builder()
    .addPreToolCallDecideHook((toolCall, context) -> {
        if ("query_user".equals(toolCall.name())) {
            // Rewrite arguments to enforce normalized email formats
            HookResult modified = HookResult.builder()
                .allow(true)
                .modifiedArgumentsJson("{\"email\":\"sanitized@example.com\"}")
                .build();
            return CompletableFuture.completedFuture(modified);
        }
        return CompletableFuture.completedFuture(HookResult.allowed());
    })
    .build();
```

### Tool Error Recovery (`OnToolErrorHook`) — Transform

Catch exceptions thrown by tools and supply custom fallback output to the model.

```java
import io.github.glaforge.antigravity.hooks.HookResult;
import java.util.concurrent.CompletableFuture;

AgentConfig config = AgentConfig.builder()
    .addOnToolErrorHook((toolCall, throwable, context) -> {
        HookResult fallback = HookResult.builder()
            .allow(true)
            .modifiedArgumentsJson("{\"error\":\"Service temporarily unavailable. Try backup endpoint.\"}")
            .build();
        return CompletableFuture.completedFuture(fallback);
    })
    .build();
```

### Interactive Question Answering (`OnInteractionHook`) — Transform

Programmatically answer clarification questions asked by the agent.

```java
import io.github.glaforge.antigravity.hooks.InteractionAnswer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

AgentConfig config = AgentConfig.builder()
    .addOnInteractionHook(request -> {
        InteractionAnswer answer = InteractionAnswer.builder()
            .freeformResponse("Automated system test environment")
            .build();
        return CompletableFuture.completedFuture(List.of(answer));
    })
    .build();
```

### Context Compaction Notifications (`OnCompactionHook`) — Inspect (v0.1.14)

Listen to context compaction events and log summarized trajectory history.

```java
AgentConfig config = AgentConfig.builder()
    .addOnCompactionHook((compactionArgs, ctx) -> {
        System.out.println("History compacted for trajectory " + compactionArgs.getTrajectoryId() 
            + " at step " + compactionArgs.getStepIndex() 
            + ": " + compactionArgs.getSummary());
        return CompletableFuture.completedFuture(null);
    })
    .build();
```

