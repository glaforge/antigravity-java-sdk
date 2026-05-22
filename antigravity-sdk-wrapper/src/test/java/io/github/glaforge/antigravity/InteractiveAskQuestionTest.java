package io.github.glaforge.antigravity;

import io.github.glaforge.antigravity.localharness.*;
import io.github.glaforge.antigravity.hooks.*;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class InteractiveAskQuestionTest {

	@Test
	public void testAskQuestionInteraction() throws Exception {
		AtomicReference<UserQuestionsRequest> capturedRequest = new AtomicReference<>();

		AgentHook mockAskHook = new OnInteractionHook() {
			@Override
			public CompletableFuture<java.util.List<UserQuestionAnswer>> onInteraction(UserQuestionsRequest request) {
				capturedRequest.set(request);

				MultipleChoiceAnswer choiceAns = MultipleChoiceAnswer.newBuilder().addSelectedChoiceIndices(1) // Pick
																												// second
																												// option
						.setFreeformResponse("I pick blue").build();

				UserQuestionAnswer answer = UserQuestionAnswer.newBuilder().setMultipleChoiceAnswer(choiceAns).build();

				return CompletableFuture.completedFuture(java.util.List.of(answer));
			}
		};

		AgentConfig config = AgentConfig.builder().allowUserQuestions(true).addHook(mockAskHook).build();

		try (AntigravityAgent agent = new AntigravityAgent(config)) {
			// Send a prompt that forces the agent to ask a question
			AgentInput prompt = new AgentInput.Text(
					"Ask me a multiple choice question with 3 options: red, blue, green. Then tell me what I chose.");
			CompletableFuture<AgentResponse> responseFuture = agent.chat(List.of(prompt));

			await().atMost(30, TimeUnit.SECONDS).until(responseFuture::isDone);
			AgentResponse response = responseFuture.get();

			assertNotNull(capturedRequest.get(), "Agent should have requested interaction");

			assertTrue(capturedRequest.get().getQuestionsCount() > 0, "Should have at least one question");
			UserQuestion q = capturedRequest.get().getQuestions(0);

			assertTrue(q.hasMultipleChoice());
			assertEquals(3, q.getMultipleChoice().getChoicesCount());

			assertTrue(response.getText().toLowerCase().contains("blue"),
					"Agent should acknowledge the choice 'blue' we sent back");
		}
	}
}
