package io.github.glaforge.antigravity.hooks;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface PreTurnHook extends AgentHook {
	CompletableFuture<HookResult> onPreTurn(String prompt);
}
