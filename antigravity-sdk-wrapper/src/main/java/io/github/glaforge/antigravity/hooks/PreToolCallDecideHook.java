package io.github.glaforge.antigravity.hooks;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface PreToolCallDecideHook extends AgentHook {
	CompletableFuture<HookResult> onPreToolCallDecide(ToolCall toolCall);
}
