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
 * Thrown when a workflow script fails validation or runtime execution.
 */
@Beta
public class WorkflowException extends RuntimeException {

	/**
	 * Constructs a new WorkflowException with the specified detail message.
	 *
	 * @param message
	 *            the detail message
	 */
	public WorkflowException(String message) {
		super(message);
	}

	/**
	 * Constructs a new WorkflowException with the specified detail message and
	 * cause.
	 *
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the cause
	 */
	public WorkflowException(String message, Throwable cause) {
		super(message, cause);
	}
}
