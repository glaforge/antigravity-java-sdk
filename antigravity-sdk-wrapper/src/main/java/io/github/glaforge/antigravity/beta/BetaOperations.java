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

import io.github.glaforge.antigravity.Agent;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Dedicated namespace view providing access to experimental and preview
 * features on an {@link Agent} instance without polluting the stable API
 * surface.
 */
@Beta
public final class BetaOperations {

	private final Agent agent;

	/**
	 * Creates a new BetaOperations namespace view bound to the specified agent.
	 *
	 * @param agent
	 *            bound agent instance
	 */
	public BetaOperations(Agent agent) {
		this.agent = Objects.requireNonNull(agent, "agent must not be null");
	}

	/**
	 * Returns the underlying agent instance.
	 *
	 * @return agent instance
	 */
	public Agent agent() {
		return agent;
	}

	/**
	 * Executes a {@link WorkflowDefinition} using the {@code run_workflow} tool.
	 *
	 * @param workflow
	 *            the workflow definition to execute
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(WorkflowDefinition workflow) {
		return runWorkflow(workflow, "", null);
	}

	/**
	 * Executes a {@link WorkflowDefinition} with an optional description using the
	 * {@code run_workflow} tool.
	 *
	 * @param workflow
	 *            the workflow definition to execute
	 * @param description
	 *            optional human-readable description of the workflow run
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(WorkflowDefinition workflow, String description) {
		return runWorkflow(workflow, description, null);
	}

	/**
	 * Executes a {@link WorkflowDefinition} with a live progress callback using the
	 * {@code run_workflow} tool.
	 *
	 * @param workflow
	 *            the workflow definition to execute
	 * @param onProgress
	 *            optional callback invoked whenever a {@link WorkflowProgress}
	 *            update is received
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(WorkflowDefinition workflow,
			Consumer<WorkflowProgress> onProgress) {
		return runWorkflow(workflow, "", onProgress);
	}

	/**
	 * Executes a {@link WorkflowDefinition} with an optional description and live
	 * progress callback using the {@code run_workflow} tool.
	 *
	 * @param workflow
	 *            the workflow definition to execute
	 * @param description
	 *            optional human-readable description of the workflow run
	 * @param onProgress
	 *            optional callback invoked whenever a {@link WorkflowProgress}
	 *            update is received
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(WorkflowDefinition workflow, String description,
			Consumer<WorkflowProgress> onProgress) {
		return agent.runWorkflowInternal(workflow, null, description, onProgress);
	}

	/**
	 * Executes an existing {@code .py} workflow script on disk using the
	 * {@code run_workflow} tool.
	 *
	 * @param scriptPath
	 *            path to an existing {@code .py} workflow script
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(Path scriptPath) {
		return runWorkflow(scriptPath, "", null);
	}

	/**
	 * Executes an existing {@code .py} workflow script on disk with an optional
	 * description using the {@code run_workflow} tool.
	 *
	 * @param scriptPath
	 *            path to an existing {@code .py} workflow script
	 * @param description
	 *            optional human-readable description of the workflow run
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(Path scriptPath, String description) {
		return runWorkflow(scriptPath, description, null);
	}

	/**
	 * Executes an existing {@code .py} workflow script on disk with a live progress
	 * callback using the {@code run_workflow} tool.
	 *
	 * @param scriptPath
	 *            path to an existing {@code .py} workflow script
	 * @param onProgress
	 *            optional callback invoked whenever a {@link WorkflowProgress}
	 *            update is received
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(Path scriptPath, Consumer<WorkflowProgress> onProgress) {
		return runWorkflow(scriptPath, "", onProgress);
	}

	/**
	 * Executes an existing {@code .py} workflow script on disk with an optional
	 * description and live progress callback using the {@code run_workflow} tool.
	 *
	 * @param scriptPath
	 *            path to an existing {@code .py} workflow script
	 * @param description
	 *            optional human-readable description of the workflow run
	 * @param onProgress
	 *            optional callback invoked whenever a {@link WorkflowProgress}
	 *            update is received
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(Path scriptPath, String description,
			Consumer<WorkflowProgress> onProgress) {
		Objects.requireNonNull(scriptPath, "scriptPath must not be null");
		return agent.runWorkflowInternal(null, scriptPath.toString(), description, onProgress);
	}

	/**
	 * Executes an existing {@code .py} workflow script on disk using the
	 * {@code run_workflow} tool.
	 *
	 * @param scriptPath
	 *            path string to an existing {@code .py} workflow script
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(String scriptPath) {
		return runWorkflow(scriptPath, "", null);
	}

	/**
	 * Executes an existing {@code .py} workflow script on disk with an optional
	 * description using the {@code run_workflow} tool.
	 *
	 * @param scriptPath
	 *            path string to an existing {@code .py} workflow script
	 * @param description
	 *            optional human-readable description of the workflow run
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(String scriptPath, String description) {
		return runWorkflow(scriptPath, description, null);
	}

	/**
	 * Executes an existing {@code .py} workflow script on disk with a live progress
	 * callback using the {@code run_workflow} tool.
	 *
	 * @param scriptPath
	 *            path string to an existing {@code .py} workflow script
	 * @param onProgress
	 *            optional callback invoked whenever a {@link WorkflowProgress}
	 *            update is received
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(String scriptPath, Consumer<WorkflowProgress> onProgress) {
		return runWorkflow(scriptPath, "", onProgress);
	}

	/**
	 * Executes an existing {@code .py} workflow script on disk with an optional
	 * description and live progress callback using the {@code run_workflow} tool.
	 *
	 * @param scriptPath
	 *            path string to an existing {@code .py} workflow script
	 * @param description
	 *            optional human-readable description of the workflow run
	 * @param onProgress
	 *            optional callback invoked whenever a {@link WorkflowProgress}
	 *            update is received
	 * @return a CompletableFuture resolving to the {@link WorkflowResult}
	 */
	public CompletableFuture<WorkflowResult> runWorkflow(String scriptPath, String description,
			Consumer<WorkflowProgress> onProgress) {
		return agent.runWorkflowInternal(null, scriptPath, description, onProgress);
	}
}
