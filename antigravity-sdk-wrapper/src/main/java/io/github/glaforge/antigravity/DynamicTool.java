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
