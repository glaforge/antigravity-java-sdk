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

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

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
	 * @param genericType the Java type to generate a schema for
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

	/**
	 * Returns the shared JsonMapper instance used by this generator.
	 * 
	 * @return the JsonMapper
	 */
	public static JsonMapper getMapper() {
		return mapper;
	}
}
