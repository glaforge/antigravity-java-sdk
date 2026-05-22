package io.github.glaforge.antigravity.hooks;

import java.util.concurrent.CompletableFuture;
import io.github.glaforge.antigravity.localharness.UserQuestionsRequest;
import io.github.glaforge.antigravity.localharness.UserQuestionAnswer;

public interface OnInteractionHook extends AgentHook {
	CompletableFuture<java.util.List<UserQuestionAnswer>> onInteraction(UserQuestionsRequest request);
}
