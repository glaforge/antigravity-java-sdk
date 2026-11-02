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

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Compile-time stub representing a single-use or multi-turn workflow agent.
 *
 * <p>
 * Inside a workflow script:
 * <ul>
 * <li>Single-use: {@code res = await workflows.agent("Prompt",
 * schema={...})}</li>
 * <li>Multi-turn:
 *
 * <pre>{@code
 * reviewer = workflows.agent("Standing instructions", role="Reviewer")
 * turn1 = await reviewer("First turn", schema={"type": "string"})
 * turn2 = await reviewer("Second turn", schema={"type": "string"})
 * }</pre>
 *
 * </li>
 * </ul>
 */
@Beta
public final class WorkflowAgent {

	/**
	 * Creates a new WorkflowAgent compile-time stub.
	 */
	public WorkflowAgent() {
	}

	/**
	 * Compile-time stub: invokes a turn on this workflow agent with a prompt and
	 * JSON schema map.
	 *
	 * @param prompt
	 *            turn prompt
	 * @param schema
	 *            JSON schema map constraining the output
	 * @return never returns (throws outside runtime)
	 */
	public Object call(String prompt, Map<String, Object> schema) {
		throw Workflows.outsideRuntimeError("agent");
	}

	/**
	 * Compile-time stub: invokes a turn on this workflow agent with a prompt and
	 * Java type schema.
	 *
	 * @param prompt
	 *            turn prompt
	 * @param schemaType
	 *            Java type or class to generate the JSON schema from
	 * @return never returns (throws outside runtime)
	 */
	public Object call(String prompt, Type schemaType) {
		throw Workflows.outsideRuntimeError("agent");
	}
}
