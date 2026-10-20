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

import java.util.Collections;
import java.util.List;
import io.github.glaforge.antigravity.beta.WorkflowProgress;
import io.github.glaforge.antigravity.hooks.ToolCall;

/**
 * Represents a single step in a conversation trajectory, either loaded from
 * persistent session history on initialization/resumption or recorded during an
 * active turn.
 *
 * @param id
 *            unique step identifier ({@code trajectoryId:stepIndex} or
 *            {@code stepIndex})
 * @param stepIndex
 *            zero-based index of the step within its trajectory
 * @param trajectoryId
 *            identifier of the trajectory this step belongs to
 * @param parentTrajectoryId
 *            identifier of the parent trajectory (non-empty for subagent steps)
 * @param depth
 *            nesting depth of the trajectory (0 for main agent)
 * @param source
 *            source of the step (e.g. {@code SOURCE_USER},
 *            {@code SOURCE_MODEL}, {@code SOURCE_SYSTEM})
 * @param state
 *            lifecycle state of the step (e.g. {@code STATE_ACTIVE},
 *            {@code STATE_DONE}, {@code STATE_WAITING_FOR_USER},
 *            {@code STATE_ERROR})
 * @param target
 *            target of the step (e.g. {@code TARGET_USER},
 *            {@code TARGET_ENVIRONMENT}, {@code TARGET_MODEL})
 * @param content
 *            text content of the step (or user request text)
 * @param thinking
 *            reasoning/thinking trace associated with the step
 * @param toolCalls
 *            tool calls invoked in this step
 * @param error
 *            error message if the step failed, or empty string
 * @param completeResponse
 *            true if this step is a completed model response targeted at the
 *            user
 * @param structuredOutput
 *            raw JSON string from a {@code finish} step, or null if none
 * @param workflowProgress
 *            workflow execution progress for {@code run_workflow} steps, or
 *            null if none
 */
public record ConversationStep(String id, int stepIndex, String trajectoryId, String parentTrajectoryId, int depth,
		String source, String state, String target, String content, String thinking, List<ToolCall> toolCalls,
		String error, boolean completeResponse, String structuredOutput, WorkflowProgress workflowProgress) {

	/**
	 * Compact constructor ensuring non-null defaults and unmodifiable toolCalls.
	 */
	public ConversationStep {
		id = id != null ? id : "0";
		trajectoryId = trajectoryId != null ? trajectoryId : "";
		parentTrajectoryId = parentTrajectoryId != null ? parentTrajectoryId : "";
		source = source != null ? source : "SOURCE_UNSPECIFIED";
		state = state != null ? state : "STATE_UNSPECIFIED";
		target = target != null ? target : "TARGET_UNSPECIFIED";
		content = content != null ? content : "";
		thinking = thinking != null ? thinking : "";
		toolCalls = toolCalls != null ? Collections.unmodifiableList(toolCalls) : List.of();
		error = error != null ? error : "";
	}
}
