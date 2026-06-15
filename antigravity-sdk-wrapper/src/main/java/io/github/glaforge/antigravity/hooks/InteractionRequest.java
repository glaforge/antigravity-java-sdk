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
package io.github.glaforge.antigravity.hooks;

import java.util.List;
import java.util.ArrayList;
import io.github.glaforge.antigravity.localharness.UserQuestionsRequest;
import io.github.glaforge.antigravity.localharness.UserQuestion;

/**
 * Represents a request from the agent to ask the user a set of questions.
 *
 * @param questions
 *            the list of questions asked by the agent.
 */
public record InteractionRequest(List<Question> questions) {

	/**
	 * Represents a single question in an interaction request.
	 *
	 * @param questionText
	 *            the text of the question
	 * @param choices
	 *            the multiple-choice options
	 * @param multiSelect
	 *            whether multiple options can be selected
	 */
	public record Question(String questionText, List<String> choices, boolean multiSelect) {
	}

	/**
	 * Internal method to convert from the protobuf representation.
	 * 
	 * @param proto
	 *            the protobuf request
	 * @return the parsed request
	 */
	public static InteractionRequest fromProtobuf(UserQuestionsRequest proto) {
		List<Question> mappedQuestions = new ArrayList<>();
		for (UserQuestion q : proto.getQuestionsList()) {
			if (q.hasMultipleChoice()) {
				mappedQuestions.add(new Question(q.getMultipleChoice().getQuestion(),
						q.getMultipleChoice().getChoicesList(), q.getMultipleChoice().getIsMultiSelect()));
			}
		}
		return new InteractionRequest(mappedQuestions);
	}
}
