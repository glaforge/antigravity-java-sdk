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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import io.github.glaforge.antigravity.DynamicTool;
import io.github.glaforge.antigravity.ToolContext;
import io.github.glaforge.antigravity.ToolExecutionError;

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
		boolean foundTool = false;

		for (Method method : clazz.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Tool.class)) {
				Tool annotation = method.getAnnotation(Tool.class);

				String toolName = annotation.name().isEmpty() ? method.getName() : annotation.name();

				method.setAccessible(true);
				registry.put(toolName, new ToolMethodHandler(serviceInstance, method));
				foundTool = true;
			}
		}

		if (!foundTool) {
			// Automatic tool name resolution: if object has a single public declared
			// method, register it using class/method name
			Method[] methods = clazz.getDeclaredMethods();
			Method candidate = null;
			for (Method m : methods) {
				if (!m.isSynthetic() && !m.getName().contains("$") && !m.getName().equals("equals")
						&& !m.getName().equals("hashCode") && !m.getName().equals("toString")) {
					if (candidate == null) {
						candidate = m;
					} else {
						candidate = null;
						break;
					}
				}
			}
			if (candidate != null) {
				String resolvedName = resolveToolName(serviceInstance, candidate);
				candidate.setAccessible(true);
				registry.put(resolvedName, new ToolMethodHandler(serviceInstance, candidate));
			}
		}
	}

	/**
	 * Resolves tool name automatically from instance class or method.
	 *
	 * @param instance
	 *            the service instance
	 * @param method
	 *            the method candidate, or null
	 * @return resolved tool name
	 */
	public static String resolveToolName(Object instance, Method method) {
		if (method != null && method.isAnnotationPresent(Tool.class)) {
			Tool anno = method.getAnnotation(Tool.class);
			if (!anno.name().isEmpty()) {
				return anno.name();
			}
			return method.getName();
		}
		if (method != null) {
			return method.getName();
		}
		return instance.getClass().getSimpleName();
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
	public List<ToolDefinition> getToolDefinitions() {
		List<ToolDefinition> definitions = new ArrayList<>();

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

			if (!required.isEmpty()) {
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
		try {
			if (dynamicRegistry.containsKey(toolName)) {
				Object result = dynamicRegistry.get(toolName).execute(arguments);
				return formatToolResult(result);
			}

			ToolMethodHandler handler = registry.get(toolName);
			if (handler == null) {
				throw new IllegalArgumentException("Unknown tool requested: " + toolName);
			}

			Object[] parsedArgs = resolveArguments(handler.method(), arguments, toolContext);
			Object result = handler.method().invoke(handler.instance(), parsedArgs);

			return formatToolResult(result);
		} catch (ToolExecutionError tee) {
			throw tee;
		} catch (Exception e) {
			Throwable cause = e;
			if (e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null) {
				cause = ite.getCause();
			}
			if (cause instanceof ToolExecutionError tee) {
				throw tee;
			}
			throw new ToolExecutionError(toolName, arguments != null ? arguments.toString() : "{}", cause);
		}
	}

	private String formatToolResult(Object result) throws Exception {
		if (result instanceof CompletionStage<?> cs) {
			result = cs.toCompletableFuture().get();
		}
		if (result instanceof String str) {
			String trimmed = str.trim();
			if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
				return str; // Assume it's already JSON
			} else {
				ObjectNode node = mapper.createObjectNode();
				node.put("result", str);
				return mapper.writeValueAsString(node);
			}
		}
		if (result == null) {
			return "{}";
		}
		// If it's a primitive or box type that isn't a string, wrap it too
		if (result instanceof Number || result instanceof Boolean) {
			ObjectNode node = mapper.createObjectNode();
			node.put("result", result.toString());
			return mapper.writeValueAsString(node);
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

			if (arguments == null || !arguments.has(name) || arguments.get(name).isNull()) {
				parsedValues[i] = getDefaultValue(type);
				continue;
			}

			JsonNode valueNode = arguments.get(name);
			if (type == String.class)
				parsedValues[i] = valueNode.asText();
			else if (type == int.class || type == Integer.class)
				parsedValues[i] = valueNode.asInt();
			else if (type == long.class || type == Long.class)
				parsedValues[i] = valueNode.asLong();
			else if (type == double.class || type == Double.class)
				parsedValues[i] = valueNode.asDouble();
			else if (type == float.class || type == Float.class)
				parsedValues[i] = (float) valueNode.asDouble();
			else if (type == boolean.class || type == Boolean.class)
				parsedValues[i] = valueNode.asBoolean();
			else {
				parsedValues[i] = mapper.treeToValue(valueNode, type);
			}
		}
		return parsedValues;
	}

	private Object getDefaultValue(Class<?> type) {
		if (type == boolean.class)
			return false;
		if (type == int.class)
			return 0;
		if (type == long.class)
			return 0L;
		if (type == double.class)
			return 0.0;
		if (type == float.class)
			return 0.0f;
		if (type == short.class)
			return (short) 0;
		if (type == byte.class)
			return (byte) 0;
		if (type == char.class)
			return '\0';
		return null;
	}

	private record ToolMethodHandler(Object instance, Method method) {
	}
}
