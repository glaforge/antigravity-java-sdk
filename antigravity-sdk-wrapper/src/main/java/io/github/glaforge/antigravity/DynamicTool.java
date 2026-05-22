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

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glaforge.antigravity.localharness.Tool;

/**
 * Represents a tool that is registered dynamically at runtime, rather than
 * discovered via {@literal @}Tool annotations on Java methods.
 */
public interface DynamicTool {

	/**
	 * @return The unique name of the tool.
	 */
	String getName();

	/**
	 * @return The Protobuf definition of the tool schema used by the harness.
	 */
	Tool getDefinition();

	/**
	 * Executes the tool with the given JSON arguments.
	 * 
	 * @param arguments
	 *            The arguments passed to the tool.
	 * @return The result of the execution.
	 * @throws Exception
	 *             if an error occurs during execution.
	 */
	Object execute(JsonNode arguments) throws Exception;
}
