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
import io.github.glaforge.antigravity.localharness.UserQuestionAnswer;
import io.github.glaforge.antigravity.localharness.MultipleChoiceAnswer;

/**
 * Represents an answer to an interaction request question.
 *
 * @param unanswered
 *            whether the question was left unanswered
 * @param selectedChoiceIndices
 *            the indices of the selected choices
 * @param freeformResponse
 *            the freeform text response
 */
public record InteractionAnswer(boolean unanswered, List<Integer> selectedChoiceIndices, String freeformResponse) {

	public InteractionAnswer {
		selectedChoiceIndices = selectedChoiceIndices != null ? List.copyOf(selectedChoiceIndices) : List.of();
		freeformResponse = freeformResponse != null ? freeformResponse : "";
	}

	/**
	 * @return a new Builder for an InteractionAnswer
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Creates an unanswered response.
	 * 
	 * @return a new unanswered InteractionAnswer
	 */
	public static InteractionAnswer createUnanswered() {
		return builder().unanswered(true).build();
	}

	/**
	 * Internal method to convert to the protobuf representation.
	 * 
	 * @return the protobuf answer
	 */
	public UserQuestionAnswer toProtobuf() {
		UserQuestionAnswer.Builder builder = UserQuestionAnswer.newBuilder();
		if (unanswered) {
			builder.setUnanswered(true);
		} else {
			MultipleChoiceAnswer.Builder mcBuilder = MultipleChoiceAnswer.newBuilder();
			if (selectedChoiceIndices != null) {
				mcBuilder.addAllSelectedChoiceIndices(selectedChoiceIndices);
			}
			if (freeformResponse != null) {
				mcBuilder.setFreeformResponse(freeformResponse);
			}
			builder.setMultipleChoiceAnswer(mcBuilder.build());
		}
		return builder.build();
	}

	/**
	 * Builder for InteractionAnswer.
	 */
	public static class Builder {
		private boolean unanswered = false;
		private List<Integer> selectedChoiceIndices = new ArrayList<>();
		private String freeformResponse = "";

		public Builder unanswered(boolean unanswered) {
			this.unanswered = unanswered;
			return this;
		}

		public Builder addSelectedChoiceIndex(int index) {
			this.selectedChoiceIndices.add(index);
			return this;
		}

		public Builder selectedChoiceIndices(List<Integer> indices) {
			this.selectedChoiceIndices = new ArrayList<>(indices);
			return this;
		}

		public Builder freeformResponse(String response) {
			this.freeformResponse = response;
			return this;
		}

		public InteractionAnswer build() {
			return new InteractionAnswer(unanswered, selectedChoiceIndices, freeformResponse);
		}
	}
}
