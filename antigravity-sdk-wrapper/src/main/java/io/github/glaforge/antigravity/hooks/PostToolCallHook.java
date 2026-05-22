package io.github.glaforge.antigravity.hooks;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface PostToolCallHook extends AgentHook {
	CompletableFuture<Void> onPostToolCall(ToolCall toolCall, Object result);
}
