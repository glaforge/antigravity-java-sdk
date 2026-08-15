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

import io.github.glaforge.antigravity.localharness.TrajectoryStateUpdate;

/**
 * Reason why an agent turn or trajectory stopped.
 */
public enum StopReason {
	/**
	 * Default or unspecified stop reason.
	 */
	UNSPECIFIED,

	/**
	 * Turn halted because the session exceeded the configured max_model_calls
	 * limit.
	 */
	MAX_MODEL_CALLS_EXCEEDED,

	/**
	 * Turn halted because the session exceeded the configured max_tool_calls limit.
	 */
	MAX_TOOL_CALLS_EXCEEDED,

	/**
	 * Turn halted because the session exceeded the configured max_input_tokens
	 * limit.
	 */
	MAX_INPUT_TOKENS_EXCEEDED,

	/**
	 * Turn halted because the session exceeded the configured max_output_tokens
	 * limit.
	 */
	MAX_OUTPUT_TOKENS_EXCEEDED,

	/**
	 * Turn halted because the session exceeded the configured max_total_tokens
	 * limit.
	 */
	MAX_TOTAL_TOKENS_EXCEEDED,

	/**
	 * Turn halted because the backend model API quota was exhausted.
	 */
	QUOTA_EXHAUSTED;

	/**
	 * Maps a protobuf {@link TrajectoryStateUpdate.StopReason} to a SDK
	 * {@link StopReason}.
	 *
	 * @param proto
	 *            the protobuf stop reason
	 * @return the mapped SDK StopReason
	 */
	public static StopReason fromProtobuf(TrajectoryStateUpdate.StopReason proto) {
		if (proto == null) {
			return UNSPECIFIED;
		}
		return switch (proto) {
			case STOP_REASON_MAX_MODEL_CALLS_EXCEEDED -> MAX_MODEL_CALLS_EXCEEDED;
			case STOP_REASON_MAX_TOOL_CALLS_EXCEEDED -> MAX_TOOL_CALLS_EXCEEDED;
			case STOP_REASON_MAX_INPUT_TOKENS_EXCEEDED -> MAX_INPUT_TOKENS_EXCEEDED;
			case STOP_REASON_MAX_OUTPUT_TOKENS_EXCEEDED -> MAX_OUTPUT_TOKENS_EXCEEDED;
			case STOP_REASON_MAX_TOTAL_TOKENS_EXCEEDED -> MAX_TOTAL_TOKENS_EXCEEDED;
			case STOP_REASON_QUOTA_EXHAUSTED -> QUOTA_EXHAUSTED;
			default -> UNSPECIFIED;
		};
	}
}
