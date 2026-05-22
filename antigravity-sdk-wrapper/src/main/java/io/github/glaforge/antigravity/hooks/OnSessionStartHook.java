package io.github.glaforge.antigravity.hooks;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface OnSessionStartHook extends AgentHook {
    CompletableFuture<Void> onSessionStart();
}
