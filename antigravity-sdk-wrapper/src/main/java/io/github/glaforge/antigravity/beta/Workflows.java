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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.glaforge.antigravity.tools.SchemaGenerator;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authoring utilities, validation rules, and fluent builders for
 * {@code run_workflow} scripts.
 */
@Beta
public final class Workflows {

	private static final Pattern QUALIFIER_PATTERN = Pattern
			.compile("\\b(?:beta\\.)?workflows?\\.(phase|log|agent|parallel|pipeline)\\b");
	private static final Pattern IMPORT_PATTERN = Pattern
			.compile("(?m)^\\s*(?:import\\s+\\S+|from\\s+\\S+\\s+import\\b)");
	private static final Pattern WHILE_PATTERN = Pattern.compile("(?m)^\\s*while\\b");
	private static final Pattern YIELD_PATTERN = Pattern.compile("(?m)(?:^|\\s)yield(?:\\s+from)?\\b");
	private static final Pattern TOP_LEVEL_RETURN_PATTERN = Pattern.compile("(?m)^return\\b");
	private static final Pattern PRIVATE_ATTR_PATTERN = Pattern.compile("\\.(_[a-zA-Z0-9_]*)\\b");
	private static final Pattern DUNDER_NAME_PATTERN = Pattern.compile("(?<![.\\w])(__[a-zA-Z0-9_]+)\\b");

	private Workflows() {
	}

	private static IllegalStateException outsideRuntimeError(String name) {
		return new IllegalStateException("workflows." + name
				+ "() is a compile-time stub for the `run_workflow` tool and cannot be called directly in the host Java process. "
				+ "Use Workflows.define(...) or Workflows.builder(...) and execute the workflow via agent.beta().runWorkflow(...).");
	}

	/**
	 * Compile-time stub: emits a progress log line to the current workflow phase.
	 *
	 * @param args
	 *            values to log
	 */
	public static void log(Object... args) {
		if (args == null || args.length == 0) {
			throw new IllegalArgumentException("log: requires at least 1 argument");
		}
		throw outsideRuntimeError("log");
	}

	/**
	 * Compile-time stub: starts a new named phase in the workflow execution.
	 *
	 * @param name
	 *            phase name
	 * @return phase name
	 */
	public static String phase(String name) {
		throw outsideRuntimeError("phase");
	}

	/**
	 * Compile-time stub: runs a workflow subagent with the given prompt and schema.
	 *
	 * @param prompt
	 *            agent prompt
	 * @param schema
	 *            JSON schema map
	 * @return never returns (throws outside runtime)
	 */
	public static Object agent(String prompt, Map<String, Object> schema) {
		throw outsideRuntimeError("agent");
	}

	/**
	 * Compile-time stub: runs a workflow subagent with role, type name, and
	 * workspace mode.
	 *
	 * @param prompt
	 *            agent prompt
	 * @param schema
	 *            JSON schema map
	 * @param role
	 *            subagent role
	 * @param typeName
	 *            subagent type name
	 * @param workspace
	 *            workspace mode ("inherit", "branch", "share")
	 * @return never returns (throws outside runtime)
	 */
	public static Object agent(String prompt, Map<String, Object> schema, String role, String typeName,
			String workspace) {
		throw outsideRuntimeError("agent");
	}

	/**
	 * Compile-time stub: runs a workflow subagent with the given prompt and Java
	 * type schema.
	 *
	 * @param prompt
	 *            agent prompt
	 * @param schemaType
	 *            Java type or class to generate the JSON schema from
	 * @return never returns (throws outside runtime)
	 */
	public static Object agent(String prompt, Type schemaType) {
		throw outsideRuntimeError("agent");
	}

	/**
	 * Compile-time stub: runs a workflow subagent with a Java type schema, role,
	 * type name, and workspace mode.
	 *
	 * @param prompt
	 *            agent prompt
	 * @param schemaType
	 *            Java type or class to generate the JSON schema from
	 * @param role
	 *            subagent role
	 * @param typeName
	 *            subagent type name
	 * @param workspace
	 *            workspace mode ("inherit", "branch", "share")
	 * @return never returns (throws outside runtime)
	 */
	public static Object agent(String prompt, Type schemaType, String role, String typeName, String workspace) {
		throw outsideRuntimeError("agent");
	}

	/**
	 * Compile-time stub: runs items in parallel.
	 *
	 * @param items
	 *            items to process
	 * @param fn
	 *            stage function
	 * @return never returns (throws outside runtime)
	 */
	public static List<Object> parallel(Iterable<?> items, Object fn) {
		throw outsideRuntimeError("parallel");
	}

	/**
	 * Compile-time stub: runs items sequentially in a pipeline.
	 *
	 * @param items
	 *            items to process
	 * @param fn
	 *            stage function
	 * @return never returns (throws outside runtime)
	 */
	public static List<Object> pipeline(Iterable<?> items, Object fn) {
		throw outsideRuntimeError("pipeline");
	}

	/**
	 * Strips {@code workflows.<primitive>} or {@code beta.workflows.<primitive>}
	 * module qualifiers from a workflow script.
	 *
	 * @param source
	 *            raw workflow script source
	 * @return normalized workflow script source
	 */
	public static String stripWorkflowQualifiers(String source) {
		Objects.requireNonNull(source, "source must not be null");
		return QUALIFIER_PATTERN.matcher(source).replaceAll("$1");
	}

	/**
	 * Validates workflow script source against {@code run_workflow} AST rules:
	 * <ul>
	 * <li>Rejects {@code import} / {@code from ... import} statements</li>
	 * <li>Rejects {@code while} loops (workflows must use bounded {@code for}
	 * loops)</li>
	 * <li>Rejects top-level {@code return} and any {@code yield} /
	 * {@code yield from}</li>
	 * <li>Rejects {@code _}-prefixed attribute access and {@code __}-prefixed
	 * dunder names</li>
	 * </ul>
	 *
	 * @param source
	 *            workflow script source
	 * @throws WorkflowException
	 *             if the source violates any workflow restriction
	 */
	public static void validateWorkflowSource(String source) {
		if (source == null || source.isBlank()) {
			throw new WorkflowException("workflow function body cannot be empty");
		}
		String strippedStrings = stripStringLiteralsAndComments(source);

		if (hasUnbalancedDelimiters(strippedStrings)) {
			throw new WorkflowException("workflow syntax error: unbalanced delimiters");
		}
		if (IMPORT_PATTERN.matcher(strippedStrings).find()) {
			throw new WorkflowException("import statements are not allowed in workflow scripts");
		}
		if (WHILE_PATTERN.matcher(strippedStrings).find()) {
			throw new WorkflowException("while loops are not allowed in workflow scripts; use bounded for loops");
		}
		if (TOP_LEVEL_RETURN_PATTERN.matcher(strippedStrings).find() || YIELD_PATTERN.matcher(strippedStrings).find()) {
			throw new WorkflowException(
					"return/yield statements are not allowed in workflow scripts; use workflows.log(...) to emit output");
		}
		Matcher attrMatcher = PRIVATE_ATTR_PATTERN.matcher(strippedStrings);
		if (attrMatcher.find()) {
			throw new WorkflowException("attribute '" + attrMatcher.group(1) + "' is not allowed in workflow scripts");
		}
		Matcher dunderMatcher = DUNDER_NAME_PATTERN.matcher(strippedStrings);
		if (dunderMatcher.find()) {
			throw new WorkflowException("name '" + dunderMatcher.group(1) + "' is not allowed in workflow scripts");
		}
	}

	private static String stripStringLiteralsAndComments(String source) {
		String s = source.replaceAll("(?s)\"\"\".*?\"\"\"|'''.*?'''", "\"\"");
		s = s.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'", "\"\"");
		s = s.replaceAll("(?m)#.*$", "");
		return s;
	}

	private static boolean hasUnbalancedDelimiters(String code) {
		int parens = 0;
		int brackets = 0;
		int braces = 0;
		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);
			switch (c) {
				case '(' -> parens++;
				case ')' -> {
					if (--parens < 0)
						return true;
				}
				case '[' -> brackets++;
				case ']' -> {
					if (--brackets < 0)
						return true;
				}
				case '{' -> braces++;
				case '}' -> {
					if (--braces < 0)
						return true;
				}
				default -> {
				}
			}
		}
		return parens != 0 || brackets != 0 || braces != 0;
	}

	/**
	 * Defines and validates a workflow from a script string, stripping any
	 * {@code (beta.)workflows.<primitive>} qualifiers.
	 *
	 * @param name
	 *            workflow name
	 * @param source
	 *            workflow script source
	 * @return validated {@link WorkflowDefinition}
	 */
	public static WorkflowDefinition define(String name, String source) {
		return define(name, "", source);
	}

	/**
	 * Defines and validates a workflow with a description and script string,
	 * stripping any {@code (beta.)workflows.<primitive>} qualifiers.
	 *
	 * @param name
	 *            workflow name
	 * @param description
	 *            workflow description
	 * @param source
	 *            workflow script source
	 * @return validated {@link WorkflowDefinition}
	 */
	public static WorkflowDefinition define(String name, String description, String source) {
		Objects.requireNonNull(name, "name must not be null");
		if (source == null || source.isBlank()) {
			throw new WorkflowException("workflow function body cannot be empty");
		}
		String normalized = stripWorkflowQualifiers(source.stripIndent()).trim() + "\n";
		validateWorkflowSource(normalized);
		return new WorkflowDefinition(name, description != null ? description : "", normalized);
	}

	/**
	 * Creates a fluent builder for constructing a {@link WorkflowDefinition} in
	 * Java.
	 *
	 * @param name
	 *            workflow name
	 * @return new {@link Builder}
	 */
	public static Builder builder(String name) {
		return new Builder(name);
	}

	/**
	 * Fluent builder for authoring multi-step {@link WorkflowDefinition} scripts in
	 * Java.
	 */
	public static final class Builder {
		private final String name;
		private String description = "";
		private final List<String> lines = new ArrayList<>();

		/**
		 * Creates a new Builder for the given workflow name.
		 *
		 * @param name
		 *            workflow name
		 */
		public Builder(String name) {
			this.name = Objects.requireNonNull(name, "name must not be null");
		}

		/**
		 * Sets the human-readable description of the workflow.
		 *
		 * @param description
		 *            workflow description
		 * @return this builder
		 */
		public Builder description(String description) {
			this.description = description != null ? description : "";
			return this;
		}

		/**
		 * Appends a {@code phase("<name>")} step to the workflow.
		 *
		 * @param phaseName
		 *            name of the phase
		 * @return this builder
		 */
		public Builder phase(String phaseName) {
			Objects.requireNonNull(phaseName, "phaseName must not be null");
			lines.add("phase(" + quotePythonString(phaseName) + ")");
			return this;
		}

		/**
		 * Appends a {@code log("...")} statement with string literals.
		 *
		 * @param messages
		 *            string messages to log
		 * @return this builder
		 */
		public Builder log(String... messages) {
			if (messages == null || messages.length == 0) {
				throw new IllegalArgumentException("log requires at least 1 message");
			}
			List<String> quoted = new ArrayList<>(messages.length);
			for (String m : messages) {
				quoted.add(quotePythonString(m));
			}
			lines.add("log(" + String.join(", ", quoted) + ")");
			return this;
		}

		/**
		 * Appends a {@code log(<expr>, ...)} statement referencing workflow variable
		 * expressions.
		 *
		 * @param expressions
		 *            variable names or expressions to log
		 * @return this builder
		 */
		public Builder logExpr(String... expressions) {
			if (expressions == null || expressions.length == 0) {
				throw new IllegalArgumentException("logExpr requires at least 1 expression");
			}
			lines.add("log(" + String.join(", ", expressions) + ")");
			return this;
		}

		/**
		 * Appends an {@code <assignTo> = await agent(...)} step to the workflow using a
		 * JSON schema map.
		 *
		 * @param assignTo
		 *            variable name to store the agent result (or null to not assign)
		 * @param prompt
		 *            prompt for the subagent
		 * @param schema
		 *            JSON schema map for the expected output (defaults to
		 *            {@code {"type": "string"}} if null)
		 * @return this builder
		 */
		public Builder agent(String assignTo, String prompt, Map<String, Object> schema) {
			return agent(assignTo, prompt, schema, null, null, null);
		}

		/**
		 * Appends an {@code <assignTo> = await agent(...)} step to the workflow using a
		 * JSON schema map.
		 *
		 * @param assignTo
		 *            variable name to store the agent result (or null to not assign)
		 * @param prompt
		 *            prompt for the subagent
		 * @param schema
		 *            JSON schema map for the expected output (defaults to
		 *            {@code {"type": "string"}} if null)
		 * @param role
		 *            optional subagent role
		 * @return this builder
		 */
		public Builder agent(String assignTo, String prompt, Map<String, Object> schema, String role) {
			return agent(assignTo, prompt, schema, role, null, null);
		}

		/**
		 * Appends an {@code <assignTo> = await agent(...)} step with full options to
		 * the workflow using a JSON schema map.
		 *
		 * @param assignTo
		 *            variable name to store the agent result (or null to not assign)
		 * @param prompt
		 *            prompt for the subagent
		 * @param schema
		 *            JSON schema map for the expected output (defaults to
		 *            {@code {"type": "string"}} if null)
		 * @param role
		 *            optional subagent role
		 * @param typeName
		 *            optional subagent type name (e.g. "self")
		 * @param workspace
		 *            optional workspace mode ("inherit", "branch", "share")
		 * @return this builder
		 */
		public Builder agent(String assignTo, String prompt, Map<String, Object> schema, String role, String typeName,
				String workspace) {
			Map<String, Object> effectiveSchema = schema != null ? schema : Map.of("type", "string");
			return appendAgentStep(assignTo, prompt, toJsonLiteral(effectiveSchema), role, typeName, workspace);
		}

		/**
		 * Appends an {@code <assignTo> = await agent(...)} step to the workflow by
		 * auto-generating the JSON schema from the given Java type or class (e.g.
		 * {@code String.class}, {@code String[].class}, or a {@code record} class).
		 *
		 * @param assignTo
		 *            variable name to store the agent result (or null to not assign)
		 * @param prompt
		 *            prompt for the subagent
		 * @param schemaType
		 *            Java type or class to generate a JSON Schema from (defaults to
		 *            {@code String.class} if null)
		 * @return this builder
		 */
		public Builder agent(String assignTo, String prompt, Type schemaType) {
			return agent(assignTo, prompt, schemaType, null, null, null);
		}

		/**
		 * Appends an {@code <assignTo> = await agent(...)} step to the workflow by
		 * auto-generating the JSON schema from the given Java type or class (e.g.
		 * {@code String.class}, {@code String[].class}, or a {@code record} class).
		 *
		 * @param assignTo
		 *            variable name to store the agent result (or null to not assign)
		 * @param prompt
		 *            prompt for the subagent
		 * @param schemaType
		 *            Java type or class to generate a JSON Schema from (defaults to
		 *            {@code String.class} if null)
		 * @param role
		 *            optional subagent role
		 * @return this builder
		 */
		public Builder agent(String assignTo, String prompt, Type schemaType, String role) {
			return agent(assignTo, prompt, schemaType, role, null, null);
		}

		/**
		 * Appends an {@code <assignTo> = await agent(...)} step with full options to
		 * the workflow by auto-generating the JSON schema from the given Java type or
		 * class.
		 *
		 * @param assignTo
		 *            variable name to store the agent result (or null to not assign)
		 * @param prompt
		 *            prompt for the subagent
		 * @param schemaType
		 *            Java type or class to generate a JSON Schema from (defaults to
		 *            {@code String.class} if null)
		 * @param role
		 *            optional subagent role
		 * @param typeName
		 *            optional subagent type name (e.g. "self")
		 * @param workspace
		 *            optional workspace mode ("inherit", "branch", "share")
		 * @return this builder
		 */
		public Builder agent(String assignTo, String prompt, Type schemaType, String role, String typeName,
				String workspace) {
			Type effectiveType = schemaType != null ? schemaType : String.class;
			return appendAgentStep(assignTo, prompt, SchemaGenerator.generateSchemaJson(effectiveType), role, typeName,
					workspace);
		}

		private Builder appendAgentStep(String assignTo, String prompt, String schemaLiteral, String role,
				String typeName, String workspace) {
			Objects.requireNonNull(prompt, "prompt must not be null");
			StringBuilder call = new StringBuilder();
			if (assignTo != null && !assignTo.isBlank()) {
				call.append(assignTo.trim()).append(" = ");
			}
			call.append("await agent(").append(quotePythonString(prompt)).append(", schema=").append(schemaLiteral);
			if (role != null && !role.isBlank()) {
				call.append(", role=").append(quotePythonString(role));
			}
			if (typeName != null && !typeName.isBlank()) {
				call.append(", type_name=").append(quotePythonString(typeName));
			}
			if (workspace != null && !workspace.isBlank()) {
				call.append(", workspace=").append(quotePythonString(workspace));
			}
			call.append(")");
			lines.add(call.toString());
			return this;
		}

		/**
		 * Appends an {@code <assignTo> = await parallel(<itemsExpr>, <fnExpr>)} step to
		 * the workflow.
		 *
		 * @param assignTo
		 *            variable name to store the parallel results list (or null to not
		 *            assign)
		 * @param itemsExpr
		 *            expression or variable name evaluating to the items collection
		 * @param fnExpr
		 *            step function or lambda expression to execute concurrently for
		 *            each item
		 * @return this builder
		 */
		public Builder parallel(String assignTo, String itemsExpr, String fnExpr) {
			return appendCollectionStep("parallel", assignTo, itemsExpr, fnExpr);
		}

		/**
		 * Appends an {@code <assignTo> = await pipeline(<itemsExpr>, <fnExpr>)} step to
		 * the workflow.
		 *
		 * @param assignTo
		 *            variable name to store the pipeline results list (or null to not
		 *            assign)
		 * @param itemsExpr
		 *            expression or variable name evaluating to the items collection
		 * @param fnExpr
		 *            step function or lambda expression to execute sequentially for
		 *            each item
		 * @return this builder
		 */
		public Builder pipeline(String assignTo, String itemsExpr, String fnExpr) {
			return appendCollectionStep("pipeline", assignTo, itemsExpr, fnExpr);
		}

		private Builder appendCollectionStep(String primitive, String assignTo, String itemsExpr, String fnExpr) {
			Objects.requireNonNull(itemsExpr, "itemsExpr must not be null");
			Objects.requireNonNull(fnExpr, "fnExpr must not be null");
			StringBuilder call = new StringBuilder();
			if (assignTo != null && !assignTo.isBlank()) {
				call.append(assignTo.trim()).append(" = ");
			}
			call.append("await ").append(primitive).append("(").append(itemsExpr.trim()).append(", ")
					.append(fnExpr.trim()).append(")");
			lines.add(call.toString());
			return this;
		}

		/**
		 * Appends a raw statement line to the workflow script.
		 *
		 * @param statement
		 *            workflow script statement
		 * @return this builder
		 */
		public Builder statement(String statement) {
			if (statement != null && !statement.isBlank()) {
				lines.add(stripWorkflowQualifiers(statement));
			}
			return this;
		}

		/**
		 * Builds and validates the {@link WorkflowDefinition}.
		 *
		 * @return validated {@link WorkflowDefinition}
		 */
		public WorkflowDefinition build() {
			if (lines.isEmpty()) {
				throw new WorkflowException("workflow function body cannot be empty");
			}
			String source = String.join("\n", lines) + "\n";
			return define(name, description, source);
		}

		private static String quotePythonString(String s) {
			try {
				return SchemaGenerator.getMapper().writeValueAsString(s);
			} catch (JsonProcessingException e) {
				return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
			}
		}

		private static String toJsonLiteral(Map<String, Object> map) {
			try {
				return SchemaGenerator.getMapper().writeValueAsString(map);
			} catch (JsonProcessingException e) {
				throw new WorkflowException("Failed to serialize schema map", e);
			}
		}
	}
}
