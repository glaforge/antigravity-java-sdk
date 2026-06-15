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
package io.github.glaforge.antigravity.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.github.glaforge.antigravity.DynamicTool;
import io.github.glaforge.antigravity.ToolContext;

/**
 * Manages the registration and execution of tools for the agent.
 */
public class ToolRegistry {
	private final Map<String, ToolMethodHandler> registry = new HashMap<>();
	private final Map<String, DynamicTool> dynamicRegistry = new HashMap<>();
	private final JsonMapper mapper = JsonMapper.builder().build();

	/**
	 * Default constructor.
	 */
	public ToolRegistry() {
	}

	/**
	 * Scans the provided object for methods annotated with {@literal @}Tool and
	 * registers them.
	 *
	 * @param serviceInstance
	 *            the object containing the tool methods
	 */
	public void registerToolsFromObject(Object serviceInstance) {
		Class<?> clazz = serviceInstance.getClass();

		for (Method method : clazz.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Tool.class)) {
				Tool annotation = method.getAnnotation(Tool.class);

				String toolName = annotation.name().isEmpty() ? method.getName() : annotation.name();

				method.setAccessible(true);
				registry.put(toolName, new ToolMethodHandler(serviceInstance, method));
			}
		}
	}

	/**
	 * Registers a dynamic tool implementation directly.
	 *
	 * @param tool
	 *            the DynamicTool instance to register
	 */
	public void registerDynamicTool(DynamicTool tool) {
		dynamicRegistry.put(tool.getName(), tool);
	}

	/**
	 * Generates Protobuf Tool definitions for all registered tools.
	 *
	 * @return a list of Tool definitions
	 */
	public List<Object> getToolDefinitions() {
		List<Object> definitions = new ArrayList<>();

		for (Map.Entry<String, ToolMethodHandler> entry : registry.entrySet()) {
			Tool annotation = entry.getValue().method().getAnnotation(Tool.class);
			ObjectNode parametersNode = mapper.createObjectNode();
			parametersNode.put("type", "object");
			ObjectNode properties = parametersNode.putObject("properties");
			ArrayNode required = mapper.createArrayNode();

			Parameter[] params = entry.getValue().method().getParameters();
			for (Parameter p : params) {
				if (p.getType() == ToolContext.class) {
					continue;
				}

				String paramName = p.getName();
				String description = "";

				if (p.isAnnotationPresent(Param.class)) {
					Param paramAnno = p.getAnnotation(Param.class);
					if (!paramAnno.name().isEmpty()) {
						paramName = paramAnno.name();
					}
					description = paramAnno.description();
				}

				ObjectNode paramSchema = SchemaGenerator.generateSchema(p.getParameterizedType());
				if (!description.isEmpty()) {
					paramSchema.put("description", description);
				}

				properties.set(paramName, paramSchema);
				required.add(paramName);
			}

			if (required.size() > 0) {
				parametersNode.set("required", required);
			}

			String parametersJsonSchema = "";
			try {
				parametersJsonSchema = mapper.writeValueAsString(parametersNode);
			} catch (Exception e) {
				throw new RuntimeException("Failed to generate schema", e);
			}

			var builder = ToolDefinition.builder();
			builder.name(entry.getKey()).description(annotation.description()).parametersSchema(parametersJsonSchema);
			definitions.add(builder.build());
		}
		for (DynamicTool dt : dynamicRegistry.values()) {
			definitions.add(dt.getDefinition());
		}

		return definitions;
	}

	/**
	 * Executes a registered tool by name with the given JSON arguments.
	 *
	 * @param toolName
	 *            the name of the tool to execute
	 * @param arguments
	 *            the JSON node containing the arguments
	 * @param toolContext
	 *            the context for the tool execution
	 * @return a JSON string representation of the tool's execution result
	 * @throws Exception
	 *             if tool execution fails
	 */
	public String execute(String toolName, JsonNode arguments, ToolContext toolContext) throws Exception {
		if (dynamicRegistry.containsKey(toolName)) {
			Object result = dynamicRegistry.get(toolName).execute(arguments);
			if (result instanceof String) {
				return (String) result;
			}
			return mapper.writeValueAsString(result);
		}

		ToolMethodHandler handler = registry.get(toolName);
		if (handler == null) {
			throw new IllegalArgumentException("Unknown tool requested: " + toolName);
		}

		Object[] parsedArgs = resolveArguments(handler.method(), arguments, toolContext);
		Object result = handler.method().invoke(handler.instance(), parsedArgs);

		if (result instanceof String) {
			return (String) result;
		}
		return mapper.writeValueAsString(result);
	}

	private Object[] resolveArguments(Method method, JsonNode arguments, ToolContext toolContext) throws Exception {
		Parameter[] parameters = method.getParameters();
		Object[] parsedValues = new Object[parameters.length];

		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			Class<?> type = param.getType();

			if (type == ToolContext.class) {
				parsedValues[i] = toolContext;
				continue;
			}

			String name = param.getName();
			if (param.isAnnotationPresent(Param.class)) {
				Param paramAnno = param.getAnnotation(Param.class);
				if (!paramAnno.name().isEmpty()) {
					name = paramAnno.name();
				}
			}

			if (arguments == null || !arguments.has(name)) {
				parsedValues[i] = null;
				continue;
			}

			JsonNode valueNode = arguments.get(name);
			if (type == String.class)
				parsedValues[i] = valueNode.asText();
			else if (type == int.class || type == Integer.class)
				parsedValues[i] = valueNode.asInt();
			else if (type == double.class || type == Double.class)
				parsedValues[i] = valueNode.asDouble();
			else if (type == boolean.class || type == Boolean.class)
				parsedValues[i] = valueNode.asBoolean();
			else {
				parsedValues[i] = mapper.treeToValue(valueNode, type);
			}
		}
		return parsedValues;
	}

	private record ToolMethodHandler(Object instance, Method method) {
	}
}
