/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
		TestUtils.retry(3, () -> {
			AtomicReference<UserQuestionsRequest> capturedRequest = new AtomicReference<>();

			AgentHook mockAskHook = new OnInteractionHook() {
				@Override
				public CompletableFuture<List<UserQuestionAnswer>> onInteraction(UserQuestionsRequest request) {
					capturedRequest.set(request);

					MultipleChoiceAnswer choiceAns = MultipleChoiceAnswer.newBuilder().addSelectedChoiceIndices(1)
							.setFreeformResponse("I pick blue").build();

					UserQuestionAnswer answer = UserQuestionAnswer.newBuilder().setMultipleChoiceAnswer(choiceAns)
							.build();

					return CompletableFuture.completedFuture(List.of(answer));
				}
			};

			AgentConfig config = AgentConfig.builder()
					.capabilities(CapabilitiesConfig.builder().allowUserQuestions(true).build()).addHook(mockAskHook)
					.build();

			try (Agent agent = new Agent(config)) {
				AgentInput prompt = new AgentInput.Text(
						"Ask me a multiple choice question with 3 options: red, blue, green. Then tell me what I chose.");
				CompletableFuture<AgentResponse> responseFuture = agent.chat(List.of(prompt));

				await().atMost(120, TimeUnit.SECONDS).until(responseFuture::isDone);
				AgentResponse response = responseFuture.get();

				assertNotNull(capturedRequest.get(), "Agent should have requested interaction");

				assertTrue(capturedRequest.get().getQuestionsCount() > 0, "Should have at least one question");
				UserQuestion q = capturedRequest.get().getQuestions(0);

				assertTrue(q.hasMultipleChoice());
				assertEquals(3, q.getMultipleChoice().getChoicesCount());

				assertTrue(response.getText().toLowerCase().contains("blue"),
						"Agent should acknowledge the choice 'blue' we sent back");
			}
		});
	}
}
