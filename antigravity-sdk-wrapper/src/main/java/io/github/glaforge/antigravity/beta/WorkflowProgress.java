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

/**
 * Progress and output metadata for a {@code run_workflow} step.
 *
 * @param scriptPath
 *            path to the workflow script being executed (populated when
 *            {@code run_workflow} is invoked with a {@code .py} script file
 *            path)
 * @param script
 *            inline workflow script source (populated when the model invokes
 *            {@code run_workflow} with inline script source)
 * @param description
 *            brief description of the workflow run
 * @param output
 *            formatted workflow log and result text produced by the workflow
 *            script
 */
@Beta
public record WorkflowProgress(String scriptPath, String script, String description, String output) {

	/**
	 * Compact constructor normalizing null strings to empty strings.
	 */
	public WorkflowProgress {
		scriptPath = scriptPath != null ? scriptPath : "";
		script = script != null ? script : "";
		description = description != null ? description : "";
		output = output != null ? output : "";
	}
}
