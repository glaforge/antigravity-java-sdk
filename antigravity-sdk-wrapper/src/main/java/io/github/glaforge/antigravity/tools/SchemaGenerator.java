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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for generating JSON schemas from Java types and providing a
 * pre-configured Jackson {@link JsonMapper} for symmetrical serialization and
 * deserialization.
 *
 * <p>
 * Supports three tiers of schema metadata:
 * <ul>
 * <li><b>Tier 1 (Jackson &amp; Standard Types)</b>: {@link JsonProperty},
 * {@link JsonPropertyDescription}, {@link JsonClassDescription},
 * {@link JsonIgnore}, {@link Optional}, and {@code @Nullable}.</li>
 * <li><b>Tier 2 (Antigravity Native)</b>: {@link Param} on parameters, fields,
 * getters, record components, enum constants, and types.</li>
 * <li><b>Tier 3 (Polyglot Ecosystem via reflection)</b>: LangChain4j
 * ({@code @Description}, {@code @P}), Spring AI ({@code @ToolParam},
 * {@code @McpToolParam}, {@code @McpArg}), mcp-java ({@code @ToolArg}), and
 * OpenAPI/Swagger ({@code @Schema}).</li>
 * </ul>
 */
public class SchemaGenerator {

	private static final JsonMapper mapper = createMapper();

	private static JsonMapper createMapper() {
		SimpleModule optionalModule = new SimpleModule("AntigravityOptionalModule");
		optionalModule.addDeserializer(Optional.class, new OptionalDeserializer());
		optionalModule.addSerializer(Optional.class, new OptionalSerializer());

		return JsonMapper.builder().addModule(optionalModule)
				.annotationIntrospector(new JacksonAnnotationIntrospector() {
					@Override
					public PropertyName findNameForSerialization(Annotated a) {
						String custom = findCustomAnnotatedName(a);
						if (custom != null && !custom.isEmpty()) {
							return PropertyName.construct(custom);
						}
						return super.findNameForSerialization(a);
					}

					@Override
					public PropertyName findNameForDeserialization(Annotated a) {
						String custom = findCustomAnnotatedName(a);
						if (custom != null && !custom.isEmpty()) {
							return PropertyName.construct(custom);
						}
						return super.findNameForDeserialization(a);
					}

					@Override
					public boolean hasIgnoreMarker(AnnotatedMember m) {
						if (isHiddenBySchemaAnnotation(m)) {
							return true;
						}
						return super.hasIgnoreMarker(m);
					}

					@Override
					public String[] findEnumValues(MapperConfig<?> config, AnnotatedClass annotatedClass,
							Enum<?>[] enumValues, String[] names) {
						names = super.findEnumValues(config, annotatedClass, enumValues, names);
						Map<String, String> customNames = null;
						for (AnnotatedField f : annotatedClass.fields()) {
							String custom = findCustomAnnotatedName(f);
							if (custom != null && !custom.isEmpty()) {
								if (customNames == null) {
									customNames = new HashMap<>();
								}
								customNames.put(f.getName(), custom);
							}
						}
						if (customNames != null) {
							for (int i = 0; i < enumValues.length; i++) {
								String custom = customNames.get(enumValues[i].name());
								if (custom != null) {
									names[i] = custom;
								}
							}
						}
						return names;
					}
				}).build();
	}

	private static final class OptionalDeserializer extends JsonDeserializer<Optional<?>>
			implements
				ContextualDeserializer {
		private final JavaType valueType;

		OptionalDeserializer() {
			this(null);
		}

		OptionalDeserializer(JavaType valueType) {
			this.valueType = valueType;
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
			JavaType wrapperType = property != null ? property.getType() : ctxt.getContextualType();
			if (wrapperType != null && wrapperType.containedTypeCount() > 0) {
				return new OptionalDeserializer(wrapperType.containedType(0));
			}
			return this;
		}

		@Override
		public Optional<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			if (p.currentToken() == JsonToken.VALUE_NULL) {
				return Optional.empty();
			}
			Object val = valueType != null ? ctxt.readValue(p, valueType) : p.readValueAs(Object.class);
			return Optional.ofNullable(val);
		}

		@Override
		public Optional<?> getNullValue(DeserializationContext ctxt) {
			return Optional.empty();
		}

		@Override
		public Object getAbsentValue(DeserializationContext ctxt) {
			return Optional.empty();
		}
	}

	@SuppressWarnings("rawtypes")
	private static final class OptionalSerializer extends JsonSerializer<Optional> {
		@Override
		public void serialize(Optional value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			if (value == null || value.isEmpty()) {
				gen.writeNull();
			} else {
				serializers.defaultSerializeValue(value.get(), gen);
			}
		}
	}

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
		return generateSchema(genericType, Collections.emptySet());
	}

	private static ObjectNode generateSchema(Type genericType, Set<Class<?>> visiting) {
		Class<?> type;
		Type[] typeArgs = null;

		if (genericType instanceof ParameterizedType pType) {
			type = (Class<?>) pType.getRawType();
			typeArgs = pType.getActualTypeArguments();
		} else if (genericType instanceof Class<?> c) {
			type = c;
		} else {
			type = Object.class;
		}

		// Unwrap Optional<T> to the underlying type T
		if (type == Optional.class) {
			Type innerType = (typeArgs != null && typeArgs.length > 0) ? typeArgs[0] : Object.class;
			return generateSchema(innerType, visiting);
		}

		ObjectNode schema = mapper.createObjectNode();

		if (type == String.class || type == CharSequence.class || type == char.class || type == Character.class) {
			schema.put("type", "string");
		} else if (type == int.class || type == Integer.class || type == long.class || type == Long.class
				|| type == short.class || type == Short.class || type == byte.class || type == Byte.class
				|| type == BigInteger.class) {
			schema.put("type", "integer");
		} else if (type == double.class || type == Double.class || type == float.class || type == Float.class
				|| type == BigDecimal.class || Number.class.isAssignableFrom(type)) {
			schema.put("type", "number");
		} else if (type == boolean.class || type == Boolean.class) {
			schema.put("type", "boolean");
		} else if (type.isEnum()) {
			schema.put("type", "string");
			String classDesc = extractDescription(type);
			if (!classDesc.isEmpty()) {
				schema.put("description", classDesc);
			}
			ArrayNode enumNodes = schema.putArray("enum");
			for (Object e : type.getEnumConstants()) {
				String enumVal = e.toString();
				try {
					Field enumField = type.getField(((Enum<?>) e).name());
					enumVal = extractPropertyName(enumVal, Arrays.asList(enumField.getAnnotations()));
				} catch (NoSuchFieldException ignored) {
				}
				enumNodes.add(enumVal);
			}
		} else if (Collection.class.isAssignableFrom(type) || type.isArray()) {
			schema.put("type", "array");
			if (type.isArray()) {
				schema.set("items", generateSchema(type.getComponentType(), visiting));
			} else if (typeArgs != null && typeArgs.length > 0) {
				schema.set("items", generateSchema(typeArgs[0], visiting));
			} else {
				schema.set("items", mapper.createObjectNode());
			}
		} else if (Map.class.isAssignableFrom(type)) {
			schema.put("type", "object");
			if (typeArgs != null && typeArgs.length == 2 && typeArgs[1] != Object.class) {
				schema.set("additionalProperties", generateSchema(typeArgs[1], visiting));
			}
		} else {
			schema.put("type", "object");
			String classDesc = extractDescription(type);
			if (!classDesc.isEmpty()) {
				schema.put("description", classDesc);
			}
			if (visiting.contains(type)) {
				return schema;
			}
			Set<Class<?>> nextVisiting = new HashSet<>(visiting);
			nextVisiting.add(type);

			ObjectNode properties = schema.putObject("properties");
			ArrayNode required = schema.putArray("required");
			for (Field f : type.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers()) || f.isSynthetic()) {
					continue;
				}
				List<Annotation> annotations = collectPropertyAnnotations(type, f);
				if (isIgnored(annotations)) {
					continue;
				}
				String fieldName = extractPropertyName(f.getName(), annotations);
				ObjectNode fieldSchema = generateSchema(f.getGenericType(), nextVisiting);

				String description = extractDescription(annotations);
				if (!description.isEmpty()) {
					fieldSchema.put("description", description);
				}

				String defaultValue = extractDefaultValue(annotations);
				if (!defaultValue.isEmpty()) {
					fieldSchema.put("default", defaultValue);
				}

				properties.set(fieldName, fieldSchema);
				if (isRequired(f.getGenericType(), annotations)) {
					required.add(fieldName);
				}
			}
			if (required.isEmpty()) {
				schema.remove("required");
			}
		}

		return schema;
	}

	/**
	 * Extracts the effective JSON property or parameter name from an
	 * {@link AnnotatedElement}, falling back to its default Java name.
	 *
	 * @param element
	 *            the parameter, field, or record component
	 * @param defaultName
	 *            the fallback name if no annotation specifies one
	 * @return the resolved name
	 */
	public static String extractPropertyName(AnnotatedElement element, String defaultName) {
		return extractPropertyName(defaultName, Arrays.asList(element.getAnnotations()));
	}

	/**
	 * Extracts the effective JSON property name from a list of annotations.
	 *
	 * @param defaultName
	 *            the fallback name
	 * @param annotations
	 *            the annotations to inspect
	 * @return the resolved property name
	 */
	public static String extractPropertyName(String defaultName, List<Annotation> annotations) {
		for (Annotation ann : annotations) {
			if (ann instanceof Param p && !p.name().isEmpty()) {
				return p.name();
			}
			if (ann instanceof JsonProperty jp && !jp.value().isEmpty()
					&& !JsonProperty.USE_DEFAULT_NAME.equals(jp.value())) {
				return jp.value();
			}
			if (isAnnotationNamed(ann, "org.mcpjava.annotations.tools.ToolArg", "ToolArg")
					|| isAnnotationNamed(ann, "org.springaicommunity.mcp.annotation.McpArg", "McpArg")
					|| isAnnotationNamed(ann, "org.springframework.ai.mcp.annotation.McpArg", "McpArg")
					|| isAnnotationNamed(ann, "io.swagger.v3.oas.annotations.media.Schema", "Schema")) {
				String name = invokeStringAttribute(ann, "name");
				if (name != null && !name.isEmpty()) {
					return name;
				}
			}
		}
		return defaultName;
	}

	/**
	 * Extracts a human-readable description from an {@link AnnotatedElement}.
	 *
	 * @param element
	 *            the annotated class, field, record component, or parameter
	 * @return the extracted description, or empty string if none is present
	 */
	public static String extractDescription(AnnotatedElement element) {
		return extractDescription(Arrays.asList(element.getAnnotations()));
	}

	/**
	 * Extracts a human-readable description from a list of annotations. Supports
	 * Antigravity {@code @Param}, Jackson
	 * {@code @JsonPropertyDescription}/{@code @JsonClassDescription}, LangChain4j
	 * {@code @Description}/{@code @P}, Spring AI {@code @ToolParam}, MCP
	 * {@code @McpToolParam}/{@code @McpArg}/{@code @ToolArg}, and OpenAPI
	 * {@code @Schema}.
	 *
	 * @param annotations
	 *            the annotations to inspect
	 * @return the extracted description, or empty string if none is present
	 */
	public static String extractDescription(List<Annotation> annotations) {
		for (Annotation ann : annotations) {
			if (ann instanceof Param p && !p.description().isEmpty()) {
				return p.description();
			}
			if (ann instanceof JsonPropertyDescription jpd && !jpd.value().isEmpty()) {
				return jpd.value();
			}
			if (ann instanceof JsonClassDescription jcd && !jcd.value().isEmpty()) {
				return jcd.value();
			}
			if (isAnnotationNamed(ann, "dev.langchain4j.model.output.structured.Description", "Description")) {
				String desc = invokeStringOrArrayAttribute(ann, "value");
				if (desc != null && !desc.isEmpty()) {
					return desc;
				}
			}
			if (isAnnotationNamed(ann, "dev.langchain4j.agent.tool.P", "P")) {
				String desc = invokeStringAttribute(ann, "value");
				if (desc != null && !desc.isEmpty()) {
					return desc;
				}
			}
			if (isAnnotationNamed(ann, "org.springframework.ai.tool.annotation.ToolParam", "ToolParam")
					|| isAnnotationNamed(ann, "org.springaicommunity.mcp.annotation.McpToolParam", "McpToolParam")
					|| isAnnotationNamed(ann, "org.springframework.ai.mcp.annotation.McpToolParam", "McpToolParam")
					|| isAnnotationNamed(ann, "org.springaicommunity.mcp.annotation.McpArg", "McpArg")
					|| isAnnotationNamed(ann, "org.springframework.ai.mcp.annotation.McpArg", "McpArg")
					|| isAnnotationNamed(ann, "org.mcpjava.annotations.tools.ToolArg", "ToolArg")
					|| isAnnotationNamed(ann, "io.swagger.v3.oas.annotations.media.Schema", "Schema")) {
				String desc = invokeStringAttribute(ann, "description");
				if (desc != null && !desc.isEmpty()) {
					return desc;
				}
			}
		}
		return "";
	}

	/**
	 * Extracts a default value from an {@link AnnotatedElement}, if specified.
	 *
	 * @param element
	 *            the annotated element
	 * @return the default value string, or empty string if none is present
	 */
	public static String extractDefaultValue(AnnotatedElement element) {
		return extractDefaultValue(Arrays.asList(element.getAnnotations()));
	}

	/**
	 * Extracts a default value from a list of annotations, if specified.
	 *
	 * @param annotations
	 *            the annotations to inspect
	 * @return the default value string, or empty string if none is present
	 */
	public static String extractDefaultValue(List<Annotation> annotations) {
		for (Annotation ann : annotations) {
			if (ann instanceof JsonProperty jp && !jp.defaultValue().isEmpty()
					&& !JsonProperty.USE_DEFAULT_NAME.equals(jp.defaultValue())) {
				return jp.defaultValue();
			}
			if (isAnnotationNamed(ann, "io.swagger.v3.oas.annotations.media.Schema", "Schema")) {
				String def = invokeStringAttribute(ann, "defaultValue");
				if (def != null && !def.isEmpty()) {
					return def;
				}
			}
		}
		return "";
	}

	/**
	 * Determines whether a method parameter is required in the JSON schema.
	 *
	 * @param parameter
	 *            the method parameter
	 * @return true if required, false if optional
	 */
	public static boolean isRequired(Parameter parameter) {
		return isRequired(parameter.getParameterizedType(), Arrays.asList(parameter.getAnnotations()));
	}

	/**
	 * Determines whether a property of the given type and annotations is required
	 * in the JSON schema.
	 *
	 * <p>
	 * Note on {@link JsonProperty}: because {@code @JsonProperty.required()}
	 * defaults to {@code false} in Jackson, Java reflection cannot distinguish
	 * {@code @JsonProperty("custom_name")} from
	 * {@code @JsonProperty(value = "custom_name", required = false)}. Consequently,
	 * {@code @JsonProperty} with a non-empty {@code value} remains required by
	 * default unless {@code defaultValue} is also set, or it is paired with
	 * {@code @Param(required = false)}, {@code @Nullable}, or {@link Optional}.
	 *
	 * @param propertyType
	 *            the generic type of the property or parameter
	 * @param annotations
	 *            the annotations associated with the property or parameter
	 * @return true if required, false if optional
	 */
	public static boolean isRequired(Type propertyType, List<Annotation> annotations) {
		if (getRawClass(propertyType) == Optional.class) {
			return false;
		}
		for (Annotation ann : annotations) {
			if ("Nullable".equals(ann.annotationType().getSimpleName())) {
				return false;
			}
			if (ann instanceof Param p && !p.required()) {
				return false;
			}
			if (ann instanceof JsonProperty jp) {
				if (!jp.defaultValue().isEmpty() && !JsonProperty.USE_DEFAULT_NAME.equals(jp.defaultValue())) {
					return false;
				}
				if (jp.value().isEmpty() && !jp.required()) {
					return false;
				}
			}
			if (isAnnotationNamed(ann, "dev.langchain4j.agent.tool.P", "P")
					|| isAnnotationNamed(ann, "org.springframework.ai.tool.annotation.ToolParam", "ToolParam")
					|| isAnnotationNamed(ann, "org.springaicommunity.mcp.annotation.McpToolParam", "McpToolParam")
					|| isAnnotationNamed(ann, "org.springframework.ai.mcp.annotation.McpToolParam", "McpToolParam")
					|| isAnnotationNamed(ann, "org.springaicommunity.mcp.annotation.McpArg", "McpArg")
					|| isAnnotationNamed(ann, "org.springframework.ai.mcp.annotation.McpArg", "McpArg")
					|| isAnnotationNamed(ann, "org.mcpjava.annotations.tools.ToolArg", "ToolArg")) {
				Boolean req = invokeBooleanAttribute(ann, "required");
				if (Boolean.FALSE.equals(req)) {
					return false;
				}
			}
			if (isAnnotationNamed(ann, "io.swagger.v3.oas.annotations.media.Schema", "Schema")) {
				Object reqMode = invokeObjectAttribute(ann, "requiredMode");
				if (reqMode != null && "NOT_REQUIRED".equals(reqMode.toString())) {
					return false;
				}
				String def = invokeStringAttribute(ann, "defaultValue");
				if (def != null && !def.isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isIgnored(List<Annotation> annotations) {
		for (Annotation ann : annotations) {
			if (ann instanceof JsonIgnore ji && ji.value()) {
				return true;
			}
			if (isAnnotationNamed(ann, "io.swagger.v3.oas.annotations.media.Schema", "Schema")) {
				Boolean hidden = invokeBooleanAttribute(ann, "hidden");
				if (Boolean.TRUE.equals(hidden)) {
					return true;
				}
			}
		}
		return false;
	}

	private static List<Annotation> collectPropertyAnnotations(Class<?> type, Field f) {
		List<Annotation> result = new ArrayList<>(Arrays.asList(f.getAnnotations()));
		String fieldName = f.getName();

		if (type.isRecord()) {
			RecordComponent[] components = type.getRecordComponents();
			if (components != null) {
				Class<?>[] paramTypes = new Class<?>[components.length];
				int matchIndex = -1;
				for (int i = 0; i < components.length; i++) {
					paramTypes[i] = components[i].getType();
					if (components[i].getName().equals(fieldName)) {
						matchIndex = i;
						addUniqueAnnotations(result, components[i].getAnnotations());
						Method accessor = components[i].getAccessor();
						if (accessor != null) {
							addUniqueAnnotations(result, accessor.getAnnotations());
						}
					}
				}
				if (matchIndex >= 0) {
					try {
						Constructor<?> canonical = type.getDeclaredConstructor(paramTypes);
						Parameter[] params = canonical.getParameters();
						if (matchIndex < params.length) {
							addUniqueAnnotations(result, params[matchIndex].getAnnotations());
						}
					} catch (NoSuchMethodException ignored) {
					}
				}
			}
		} else if (!fieldName.isEmpty()) {
			String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
			for (String prefix : new String[]{"get", "is"}) {
				try {
					Method getter = type.getDeclaredMethod(prefix + capitalized);
					if (getter.getParameterCount() == 0) {
						addUniqueAnnotations(result, getter.getAnnotations());
					}
				} catch (NoSuchMethodException ignored) {
				}
			}
		}
		return result;
	}

	private static void addUniqueAnnotations(List<Annotation> target, Annotation[] additions) {
		for (Annotation ann : additions) {
			if (!target.contains(ann)) {
				target.add(ann);
			}
		}
	}

	private static String findCustomAnnotatedName(Annotated a) {
		return findCustomAnnotatedName(a.annotations());
	}

	private static String findCustomAnnotatedName(Iterable<Annotation> annotations) {
		for (Annotation ann : annotations) {
			if (ann instanceof Param p && !p.name().isEmpty()) {
				return p.name();
			}
			if (isAnnotationNamed(ann, "org.mcpjava.annotations.tools.ToolArg", "ToolArg")
					|| isAnnotationNamed(ann, "org.springaicommunity.mcp.annotation.McpArg", "McpArg")
					|| isAnnotationNamed(ann, "org.springframework.ai.mcp.annotation.McpArg", "McpArg")
					|| isAnnotationNamed(ann, "io.swagger.v3.oas.annotations.media.Schema", "Schema")) {
				String name = invokeStringAttribute(ann, "name");
				if (name != null && !name.isEmpty()) {
					return name;
				}
			}
		}
		return null;
	}

	private static boolean isHiddenBySchemaAnnotation(Annotated a) {
		for (Annotation ann : a.annotations()) {
			if (isAnnotationNamed(ann, "io.swagger.v3.oas.annotations.media.Schema", "Schema")) {
				Boolean hidden = invokeBooleanAttribute(ann, "hidden");
				if (Boolean.TRUE.equals(hidden)) {
					return true;
				}
			}
		}
		return false;
	}

	static boolean isAnnotationNamed(Annotation ann, String fullName, String simpleName) {
		Class<? extends Annotation> type = ann.annotationType();
		return fullName.equals(type.getName()) || simpleName.equals(type.getSimpleName());
	}

	static String invokeStringAttribute(Annotation ann, String attributeName) {
		Object val = invokeObjectAttribute(ann, attributeName);
		return val instanceof String s ? s : null;
	}

	static String invokeStringOrArrayAttribute(Annotation ann, String attributeName) {
		Object val = invokeObjectAttribute(ann, attributeName);
		if (val instanceof String s) {
			return s;
		}
		if (val instanceof String[] arr) {
			return String.join(" ", arr).trim();
		}
		return null;
	}

	static Boolean invokeBooleanAttribute(Annotation ann, String attributeName) {
		Object val = invokeObjectAttribute(ann, attributeName);
		return val instanceof Boolean b ? b : null;
	}

	static Object invokeObjectAttribute(Annotation ann, String attributeName) {
		try {
			Method m = ann.annotationType().getMethod(attributeName);
			return m.invoke(ann);
		} catch (Exception e) {
			return null;
		}
	}

	private static Class<?> getRawClass(Type genericType) {
		if (genericType instanceof ParameterizedType pType && pType.getRawType() instanceof Class<?> c) {
			return c;
		}
		if (genericType instanceof Class<?> c) {
			return c;
		}
		return Object.class;
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
	 * Generates a JSON schema string representation of the given Java type.
	 *
	 * @param genericType
	 *            the Java type to generate a schema for
	 * @return a JSON string representing the JSON schema
	 */
	public static String generateSchemaJson(Type genericType) {
		try {
			ObjectNode schemaNode = generateSchema(genericType);
			return mapper.writeValueAsString(schemaNode);
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate JSON schema for class: " + genericType.getTypeName(), e);
		}
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
