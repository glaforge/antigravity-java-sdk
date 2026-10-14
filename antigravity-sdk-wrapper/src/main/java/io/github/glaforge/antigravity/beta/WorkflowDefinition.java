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
package io.github.glaforge.antigravity.beta;

import java.util.Objects;

/**
 * A validated workflow script definition for the {@code run_workflow} tool.
 *
 * @param name
 *            name of the workflow
 * @param description
 *            optional human-readable description of the workflow
 * @param source
 *            validated standalone workflow script source
 */
@Beta
public record WorkflowDefinition(String name, String description, String source) {

	/**
	 * Compact constructor validating fields and workflow script source.
	 */
	public WorkflowDefinition {
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(source, "source must not be null");
		description = description != null ? description : "";
		if (source.isBlank()) {
			throw new WorkflowException("workflow function body cannot be empty");
		}
		Workflows.validateWorkflowSource(source);
	}

	/**
	 * Convenience constructor without description.
	 *
	 * @param name
	 *            workflow name
	 * @param source
	 *            workflow script source
	 */
	public WorkflowDefinition(String name, String source) {
		this(name, "", source);
	}
}
