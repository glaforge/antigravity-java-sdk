package io.github.glaforge.antigravity.hooks;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface OnSessionEndHook extends AgentHook {
	CompletableFuture<Void> onSessionEnd();
}
