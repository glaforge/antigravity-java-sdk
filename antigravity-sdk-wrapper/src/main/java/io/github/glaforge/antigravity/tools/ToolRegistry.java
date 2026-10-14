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
import io.github.glaforge.antigravity.DynamicTool;
import io.github.glaforge.antigravity.ToolContext;
import io.github.glaforge.antigravity.ToolExecutionError;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Manages the registration and execution of tools for the agent.
 */
public class ToolRegistry {
	private final Map<String, ToolMethodHandler> registry = new HashMap<>();
	private final Map<String, DynamicTool> dynamicRegistry = new HashMap<>();
	private final JsonMapper mapper = SchemaGenerator.getMapper();

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
			if (isToolMethod(method)) {
				String toolName = resolveToolName(serviceInstance, method);

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
		if (method != null) {
			if (method.isAnnotationPresent(Tool.class)) {
				Tool anno = method.getAnnotation(Tool.class);
				if (!anno.name().isEmpty()) {
					return anno.name();
				}
				return method.getName();
			}
			for (Annotation ann : method.getAnnotations()) {
				if (isThirdPartyToolAnnotation(ann)) {
					String name = SchemaGenerator.invokeStringAttribute(ann, "name");
					if (name != null && !name.isEmpty()) {
						return name;
					}
					return method.getName();
				}
			}
			return method.getName();
		}
		return instance.getClass().getSimpleName();
	}

	private static boolean isToolMethod(Method method) {
		if (method.isAnnotationPresent(Tool.class)) {
			return true;
		}
		for (Annotation ann : method.getAnnotations()) {
			if (isThirdPartyToolAnnotation(ann)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isThirdPartyToolAnnotation(Annotation ann) {
		return SchemaGenerator.isAnnotationNamed(ann, "dev.langchain4j.agent.tool.Tool", "Tool")
				|| SchemaGenerator.isAnnotationNamed(ann, "org.springframework.ai.tool.annotation.Tool", "Tool")
				|| SchemaGenerator.isAnnotationNamed(ann, "org.springaicommunity.mcp.annotation.McpTool", "McpTool")
				|| SchemaGenerator.isAnnotationNamed(ann, "org.springframework.ai.mcp.annotation.McpTool", "McpTool")
				|| SchemaGenerator.isAnnotationNamed(ann, "org.mcpjava.annotations.tools.Tool", "Tool");
	}

	private static String resolveToolDescription(Method method) {
		if (method.isAnnotationPresent(Tool.class)) {
			return method.getAnnotation(Tool.class).description();
		}
		for (Annotation ann : method.getAnnotations()) {
			if (SchemaGenerator.isAnnotationNamed(ann, "dev.langchain4j.agent.tool.Tool", "Tool")) {
				String desc = SchemaGenerator.invokeStringOrArrayAttribute(ann, "value");
				if (desc != null && !desc.isEmpty()) {
					return desc;
				}
			}
			if (isThirdPartyToolAnnotation(ann)) {
				String desc = SchemaGenerator.invokeStringAttribute(ann, "description");
				if (desc != null && !desc.isEmpty()) {
					return desc;
				}
			}
		}
		return SchemaGenerator.extractDescription(method);
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
			Method method = entry.getValue().method();
			ObjectNode parametersNode = mapper.createObjectNode();
			parametersNode.put("type", "object");
			ObjectNode properties = parametersNode.putObject("properties");
			ArrayNode required = mapper.createArrayNode();

			Parameter[] params = method.getParameters();
			for (Parameter p : params) {
				if (isToolContextParam(p)) {
					continue;
				}

				String paramName = SchemaGenerator.extractPropertyName(p, p.getName());
				String description = SchemaGenerator.extractDescription(p);
				String defaultValue = SchemaGenerator.extractDefaultValue(p);

				ObjectNode paramSchema = SchemaGenerator.generateSchema(p.getParameterizedType());
				if (!description.isEmpty()) {
					paramSchema.put("description", description);
				}
				if (!defaultValue.isEmpty()) {
					paramSchema.put("default", defaultValue);
				}

				properties.set(paramName, paramSchema);
				if (SchemaGenerator.isRequired(p)) {
					required.add(paramName);
				}
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
			builder.name(entry.getKey()).description(resolveToolDescription(method))
					.parametersSchema(parametersJsonSchema);
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
			if (e instanceof InvocationTargetException ite && ite.getCause() != null) {
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
			if (isOptionalToolContextParam(param)) {
				parsedValues[i] = Optional.ofNullable(toolContext);
				continue;
			}

			String name = SchemaGenerator.extractPropertyName(param, param.getName());

			if (arguments == null || !arguments.has(name) || arguments.get(name).isNull()) {
				String defaultVal = SchemaGenerator.extractDefaultValue(param);
				if (!defaultVal.isEmpty()) {
					parsedValues[i] = convertDefaultValue(defaultVal, param.getParameterizedType(), type);
				} else if (type == Optional.class) {
					parsedValues[i] = Optional.empty();
				} else {
					parsedValues[i] = getDefaultValue(type);
				}
				continue;
			}

			JsonNode valueNode = arguments.get(name);
			if (type == Optional.class) {
				Type innerType = Object.class;
				if (param.getParameterizedType() instanceof ParameterizedType pt
						&& pt.getActualTypeArguments().length == 1) {
					innerType = pt.getActualTypeArguments()[0];
				}
				Object innerVal = convertNodeValue(valueNode, innerType);
				parsedValues[i] = Optional.ofNullable(innerVal);
			} else {
				parsedValues[i] = convertNodeValue(valueNode, param.getParameterizedType());
			}
		}
		return parsedValues;
	}

	private Object convertNodeValue(JsonNode valueNode, Type targetType) throws Exception {
		Class<?> rawClass = targetType instanceof Class<?> c
				? c
				: (targetType instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> rc
						? rc
						: Object.class);
		if (rawClass == String.class)
			return valueNode.asText();
		if (rawClass == int.class || rawClass == Integer.class)
			return valueNode.asInt();
		if (rawClass == long.class || rawClass == Long.class)
			return valueNode.asLong();
		if (rawClass == double.class || rawClass == Double.class)
			return valueNode.asDouble();
		if (rawClass == float.class || rawClass == Float.class)
			return (float) valueNode.asDouble();
		if (rawClass == boolean.class || rawClass == Boolean.class)
			return valueNode.asBoolean();
		return mapper.convertValue(valueNode, mapper.constructType(targetType));
	}

	private Object convertDefaultValue(String defaultVal, Type genericType, Class<?> rawType) throws Exception {
		if (rawType == Optional.class) {
			Type innerType = Object.class;
			if (genericType instanceof ParameterizedType pt && pt.getActualTypeArguments().length == 1) {
				innerType = pt.getActualTypeArguments()[0];
			}
			Class<?> innerRaw = innerType instanceof Class<?> c ? c : Object.class;
			return Optional.ofNullable(convertDefaultValue(defaultVal, innerType, innerRaw));
		}
		if (rawType == String.class)
			return defaultVal;
		if (rawType == int.class || rawType == Integer.class)
			return Integer.parseInt(defaultVal);
		if (rawType == long.class || rawType == Long.class)
			return Long.parseLong(defaultVal);
		if (rawType == double.class || rawType == Double.class)
			return Double.parseDouble(defaultVal);
		if (rawType == float.class || rawType == Float.class)
			return Float.parseFloat(defaultVal);
		if (rawType == boolean.class || rawType == Boolean.class)
			return Boolean.parseBoolean(defaultVal);
		return mapper.readValue(defaultVal, mapper.constructType(genericType));
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

	private boolean isToolContextParam(Parameter param) {
		return param.getType() == ToolContext.class || isOptionalToolContextParam(param);
	}

	private boolean isOptionalToolContextParam(Parameter param) {
		if (param.getType() != Optional.class) {
			return false;
		}
		if (param.getParameterizedType() instanceof ParameterizedType pt) {
			var typeArgs = pt.getActualTypeArguments();
			return typeArgs.length == 1 && typeArgs[0] == ToolContext.class;
		}
		return false;
	}

	private record ToolMethodHandler(Object instance, Method method) {
	}
}
