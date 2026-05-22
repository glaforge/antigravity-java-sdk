package io.github.glaforge.antigravity.hooks;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface OnToolErrorHook extends AgentHook {
    CompletableFuture<Object> onToolError(ToolCall toolCall, Throwable error);
}
