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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import io.github.glaforge.antigravity.localharness.Tool;
import io.github.glaforge.antigravity.DynamicTool;

public class ToolRegistry {
	private final Map<String, ToolMethodHandler> registry = new HashMap<>();
	private final Map<String, DynamicTool> dynamicRegistry = new HashMap<>();
	private final JsonMapper mapper = JsonMapper.builder().build();

	public void registerToolsFromObject(Object serviceInstance) {
		Class<?> clazz = serviceInstance.getClass();

		for (Method method : clazz.getDeclaredMethods()) {
			if (method.isAnnotationPresent(AntigravityTool.class)) {
				AntigravityTool annotation = method.getAnnotation(AntigravityTool.class);

				String toolName = annotation.name().isEmpty() ? method.getName() : annotation.name();

				method.setAccessible(true);
				registry.put(toolName, new ToolMethodHandler(serviceInstance, method));
				System.out.println("Successfully registered native tool: " + toolName);
			}
		}
	}

	public void registerDynamicTool(DynamicTool tool) {
		dynamicRegistry.put(tool.getName(), tool);
		System.out.println("Successfully registered dynamic tool: " + tool.getName());
	}

	public List<Tool> getToolDefinitions() {
		List<Tool> definitions = new ArrayList<>();

		for (Map.Entry<String, ToolMethodHandler> entry : registry.entrySet()) {
			AntigravityTool annotation = entry.getValue().method().getAnnotation(AntigravityTool.class);
			StringBuilder properties = new StringBuilder();
			Parameter[] params = entry.getValue().method().getParameters();
			for (int i = 0; i < params.length; i++) {
				Parameter p = params[i];
				String typeStr = "string";
				if (p.getType() == int.class || p.getType() == Integer.class)
					typeStr = "integer";
				else if (p.getType() == boolean.class || p.getType() == Boolean.class)
					typeStr = "boolean";
				else if (p.getType() == double.class || p.getType() == Double.class)
					typeStr = "number";
				properties.append("\"").append(p.getName()).append("\": {\"type\": \"").append(typeStr).append("\"}");
				if (i < params.length - 1)
					properties.append(", ");
			}

			String parametersJsonSchema = "{\"type\": \"object\", \"properties\": {" + properties.toString() + "}}";
			definitions.add(Tool.newBuilder().setName(entry.getKey()).setDescription(annotation.description())
					.setParametersJsonSchema(parametersJsonSchema).build());
		}
		for (DynamicTool dt : dynamicRegistry.values()) {
			definitions.add(dt.getDefinition());
		}

		return definitions;
	}

	public String execute(String toolName, JsonNode arguments) throws Exception {
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

		Object[] parsedArgs = resolveArguments(handler.method(), arguments);
		Object result = handler.method().invoke(handler.instance(), parsedArgs);

		if (result instanceof String) {
			return (String) result;
		}
		return mapper.writeValueAsString(result);
	}

	private Object[] resolveArguments(Method method, JsonNode arguments) throws Exception {
		Parameter[] parameters = method.getParameters();
		Object[] parsedValues = new Object[parameters.length];

		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			String name = param.getName();
			Class<?> type = param.getType();

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
