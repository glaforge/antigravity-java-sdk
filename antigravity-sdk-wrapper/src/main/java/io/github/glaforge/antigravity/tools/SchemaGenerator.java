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
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for generating JSON schemas from Java types.
 */
public class SchemaGenerator {

	private static final JsonMapper mapper = JsonMapper.builder().build();

	private SchemaGenerator() {
		// Utility class
	}

	/**
	 * Generates a JSON schema representation of the given Java type.
	 *
	 * @param genericType
	 *            the Java type to generate a schema for
	 * @return an ObjectNode representing the JSON schema
	 */
	public static ObjectNode generateSchema(Type genericType) {
		ObjectNode schema = mapper.createObjectNode();
		Class<?> type;
		Type[] typeArgs = null;

		if (genericType instanceof ParameterizedType pType) {
			type = (Class<?>) pType.getRawType();
			typeArgs = pType.getActualTypeArguments();
		} else if (genericType instanceof Class) {
			type = (Class<?>) genericType;
		} else {
			type = Object.class;
		}

		if (type == String.class || type == CharSequence.class) {
			schema.put("type", "string");
		} else if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
			schema.put("type", "integer");
		} else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
			schema.put("type", "number");
		} else if (type == boolean.class || type == Boolean.class) {
			schema.put("type", "boolean");
		} else if (type.isEnum()) {
			schema.put("type", "string");
			ArrayNode enumNodes = schema.putArray("enum");
			for (Object e : type.getEnumConstants()) {
				enumNodes.add(e.toString());
			}
		} else if (List.class.isAssignableFrom(type) || type.isArray()) {
			schema.put("type", "array");
			if (type.isArray()) {
				schema.set("items", generateSchema(type.getComponentType()));
			} else if (typeArgs != null && typeArgs.length > 0) {
				schema.set("items", generateSchema(typeArgs[0]));
			} else {
				schema.set("items", mapper.createObjectNode());
			}
		} else {
			schema.put("type", "object");
			ObjectNode properties = schema.putObject("properties");
			ArrayNode required = schema.putArray("required");
			for (Field f : type.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
					continue;
				}
				String fieldName = f.getName();
				properties.set(fieldName, generateSchema(f.getGenericType()));
				required.add(fieldName);
			}
			if (required.isEmpty()) {
				schema.remove("required");
			}
		}

		return schema;
	}

	private static final Map<String, String> SCHEMA_KEYWORD_MAP = Map.ofEntries(Map.entry("any_of", "anyOf"),
			Map.entry("one_of", "oneOf"), Map.entry("all_of", "allOf"),
			Map.entry("additional_properties", "additionalProperties"),
			Map.entry("pattern_properties", "patternProperties"), Map.entry("min_items", "minItems"),
			Map.entry("max_items", "maxItems"), Map.entry("min_length", "minLength"),
			Map.entry("max_length", "maxLength"), Map.entry("min_properties", "minProperties"),
			Map.entry("max_properties", "maxProperties"), Map.entry("unique_items", "uniqueItems"),
			Map.entry("multiple_of", "multipleOf"), Map.entry("exclusive_minimum", "exclusiveMinimum"),
			Map.entry("exclusive_maximum", "exclusiveMaximum"), Map.entry("prefix_items", "prefixItems"),
			Map.entry("property_names", "propertyNames"), Map.entry("dependent_required", "dependentRequired"),
			Map.entry("dependent_schemas", "dependentSchemas"),
			Map.entry("unevaluated_properties", "unevaluatedProperties"),
			Map.entry("unevaluated_items", "unevaluatedItems"));

	private static final Set<String> LITERAL_KEYWORDS = Set.of("enum", "const", "default", "example", "examples",
			"dependentRequired");

	/**
	 * Recursively normalizes JSON Schema dictionaries for universal model
	 * compatibility.
	 *
	 * Converts uppercase type names to lowercase strings, converts snake_case JSON
	 * Schema keywords to camelCase (e.g. multiple_of -&gt; multipleOf), and
	 * preserves literal values.
	 *
	 * @param schema
	 *            the JSON schema node
	 * @return a normalized JSON Schema node
	 */
	public static JsonNode normalizeSchema(JsonNode schema) {
		if (schema == null) {
			return null;
		}
		if (schema instanceof ObjectNode obj) {
			ObjectNode normalized = mapper.createObjectNode();
			var fields = obj.fields();
			while (fields.hasNext()) {
				var entry = fields.next();
				String rawKey = entry.getKey();
				String key = SCHEMA_KEYWORD_MAP.getOrDefault(rawKey, rawKey);
				JsonNode val = entry.getValue();

				if ("type".equals(key) && val.isTextual()) {
					normalized.put(key, val.asText().toLowerCase());
				} else if (LITERAL_KEYWORDS.contains(key)) {
					normalized.set(key, val);
				} else {
					normalized.set(key, normalizeSchema(val));
				}
			}
			return normalized;
		} else if (schema instanceof ArrayNode arr) {
			ArrayNode normalized = mapper.createArrayNode();
			for (JsonNode item : arr) {
				normalized.add(normalizeSchema(item));
			}
			return normalized;
		}
		return schema;
	}

	/**
	 * Returns the shared JsonMapper instance used by this generator.
	 * 
	 * @return the JsonMapper
	 */
	public static JsonMapper getMapper() {
		return mapper;
	}
}
