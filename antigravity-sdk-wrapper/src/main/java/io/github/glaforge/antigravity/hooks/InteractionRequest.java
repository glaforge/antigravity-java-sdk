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
 */
public class InteractionRequest {
    private final List<Question> questions;

    private InteractionRequest(List<Question> questions) {
        this.questions = questions;
    }

    /**
     * @return the list of questions asked by the agent.
     */
    public List<Question> getQuestions() {
        return questions;
    }

    /**
     * Represents a single question in an interaction request.
     */
    public static class Question {
        private final String questionText;
        private final List<String> choices;
        private final boolean multiSelect;

        private Question(String questionText, List<String> choices, boolean multiSelect) {
            this.questionText = questionText;
            this.choices = choices;
            this.multiSelect = multiSelect;
        }

        public String getQuestionText() { return questionText; }
        public List<String> getChoices() { return choices; }
        public boolean isMultiSelect() { return multiSelect; }
    }

    /**
     * Internal method to convert from the protobuf representation.
     * @param proto the protobuf request
     * @return the parsed request
     */
    public static InteractionRequest fromProtobuf(UserQuestionsRequest proto) {
        List<Question> mappedQuestions = new ArrayList<>();
        for (UserQuestion q : proto.getQuestionsList()) {
            if (q.hasMultipleChoice()) {
                mappedQuestions.add(new Question(
                    q.getMultipleChoice().getQuestion(),
                    q.getMultipleChoice().getChoicesList(),
                    q.getMultipleChoice().getIsMultiSelect()
                ));
            }
        }
        return new InteractionRequest(mappedQuestions);
    }
}
