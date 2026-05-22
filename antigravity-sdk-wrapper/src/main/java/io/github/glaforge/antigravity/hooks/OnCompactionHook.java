package io.github.glaforge.antigravity.hooks;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface OnCompactionHook extends AgentHook {
    CompletableFuture<Void> onCompaction(Object stepData);
}
