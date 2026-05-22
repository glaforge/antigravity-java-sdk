package io.github.glaforge.antigravity.hooks;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface PostTurnHook extends AgentHook {
	CompletableFuture<Void> onPostTurn(String response);
}
