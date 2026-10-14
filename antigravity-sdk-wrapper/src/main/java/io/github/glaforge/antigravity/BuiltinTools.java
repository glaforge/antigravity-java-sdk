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

import java.util.Arrays;
import java.util.List;

/**
 * Identifiers for common connection-provided builtin tools and helpers to
 * categorize them.
 */
public enum BuiltinTools {
	/** List directory contents. */
	LIST_DIR("list_directory"),
	/** Search within directories (grep). */
	SEARCH_DIR("search_directory"),
	/** Find files by name within a directory. */
	FIND_FILE("find_file"),
	/** View file contents. */
	VIEW_FILE("view_file"),
	/** Create a new file. */
	CREATE_FILE("create_file"),
	/** Edit an existing file. */
	EDIT_FILE("edit_file"),
	/** Execute a shell command. */
	RUN_COMMAND("run_command"),
	/** Ask the user a clarifying question. */
	ASK_QUESTION("ask_question"),
	/** Invoke a subagent. */
	INVOKE_SUBAGENT("invoke_subagent"),
	/** Generate or edit images. */
	GENERATE_IMAGE("generate_image"),
	/** Search the web. */
	SEARCH_WEB("search_web"),
	/** Read content from a URL. */
	READ_URL_CONTENT("read_url_content"),
	/** Schedule a one-shot timer or recurring cron job. */
	SCHEDULE("schedule"),
	/** Manage background tasks. */
	MANAGE_TASK("manage_task"),
	/** Execute a declarative multi-step workflow. */
	RUN_WORKFLOW("run_workflow"),
	/** Finish the conversation and return structured output. */
	FINISH("finish");

	/**
	 * Legacy alias for {@link #INVOKE_SUBAGENT}.
	 *
	 * @deprecated Use {@link #INVOKE_SUBAGENT} instead.
	 */
	@Deprecated
	public static final BuiltinTools START_SUBAGENT = INVOKE_SUBAGENT;

	private final String toolName;

	BuiltinTools(String toolName) {
		this.toolName = toolName;
	}

	/**
	 * Returns the underlying tool name identifier string.
	 *
	 * @return tool name string
	 */
	public String getValue() {
		return toolName;
	}

	@Override
	public String toString() {
		return toolName;
	}

	/**
	 * Resolves a {@link BuiltinTools} constant from its wire value or legacy alias
	 * (such as {@code "start_subagent"}).
	 *
	 * @param value
	 *            the tool name string
	 * @return matching BuiltinTools constant, or null if not a builtin tool
	 */
	public static BuiltinTools fromValue(String value) {
		if (value == null) {
			return null;
		}
		String normalized = normalizeToolName(value);
		for (BuiltinTools tool : values()) {
			if (tool.toolName.equals(normalized)) {
				return tool;
			}
		}
		return null;
	}

	/**
	 * Normalizes legacy builtin tool names (such as {@code "start_subagent"}) to
	 * their canonical wire name ({@code "invoke_subagent"}).
	 *
	 * @param toolName
	 *            the tool name to normalize
	 * @return normalized tool name
	 */
	public static String normalizeToolName(String toolName) {
		return normalizeToolName(toolName, null);
	}

	/**
	 * Normalizes legacy builtin tool names (such as {@code "start_subagent"}) to
	 * their canonical wire name ({@code "invoke_subagent"}), leaving MCP tools
	 * (where {@code serverName} is non-empty) untouched.
	 *
	 * @param toolName
	 *            the tool name to normalize
	 * @param serverName
	 *            optional MCP server name
	 * @return normalized tool name
	 */
	public static String normalizeToolName(String toolName, String serverName) {
		if (toolName == null) {
			return null;
		}
		if ((serverName == null || serverName.isEmpty()) && "start_subagent".equals(toolName)) {
			return INVOKE_SUBAGENT.getValue();
		}
		return toolName;
	}

	/**
	 * Returns deprecated/legacy builtin tools that are disabled by default.
	 *
	 * Includes LIST_DIR, SEARCH_DIR, and FIND_FILE, which are excluded from default
	 * tool collections to reduce prompt overhead and only enabled when explicitly
	 * requested.
	 *
	 * @return a list of deprecated BuiltinTools
	 */
	public static List<BuiltinTools> deprecated() {
		return List.of(LIST_DIR, SEARCH_DIR, FIND_FILE);
	}

	/**
	 * Returns tools that only read state (no writes, deletes, or commands).
	 *
	 * Excludes LIST_DIR, SEARCH_DIR, and FIND_FILE, which are disabled by default.
	 *
	 * @return a list of read-only BuiltinTools
	 */
	public static List<BuiltinTools> readOnly() {
		return List.of(VIEW_FILE, READ_URL_CONTENT, SCHEDULE, FINISH);
	}

	/**
	 * Returns tools that cannot delete content.
	 *
	 * Excludes LIST_DIR, SEARCH_DIR, and FIND_FILE, which are disabled by default.
	 *
	 * @return a list of non-destructive BuiltinTools
	 */
	public static List<BuiltinTools> nondestructive() {
		return List.of(VIEW_FILE, CREATE_FILE, EDIT_FILE, ASK_QUESTION, INVOKE_SUBAGENT, GENERATE_IMAGE, SEARCH_WEB,
				READ_URL_CONTENT, SCHEDULE, MANAGE_TASK, RUN_WORKFLOW, FINISH);
	}

	/**
	 * Returns all builtin tools.
	 *
	 * @return a list of all BuiltinTools
	 */
	public static List<BuiltinTools> allTools() {
		return List.of(values());
	}

	/**
	 * Returns tools that perform file read/write/create operations.
	 *
	 * @return a list of file-operation BuiltinTools
	 */
	public static List<BuiltinTools> fileTools() {
		return List.of(VIEW_FILE, CREATE_FILE, EDIT_FILE);
	}

	/**
	 * Returns the minimal set of software engineering tools.
	 *
	 * Includes run_command, view_file, create_file, and edit_file.
	 *
	 * @return a list of minimal BuiltinTools
	 */
	public static List<BuiltinTools> minimal() {
		return List.of(RUN_COMMAND, VIEW_FILE, CREATE_FILE, EDIT_FILE);
	}

	/**
	 * Returns the default set of builtin tools for autonomous agents.
	 *
	 * Excludes {@link #ASK_QUESTION} because autonomous agents cannot prompt the
	 * user interactively, as well as deprecated tools ({@link #LIST_DIR},
	 * {@link #SEARCH_DIR}, and {@link #FIND_FILE}) to minimize prompt overhead.
	 *
	 * @return a list of default BuiltinTools
	 */
	public static List<BuiltinTools> defaultTools() {
		return Arrays.stream(values()).filter(t -> t != ASK_QUESTION && !deprecated().contains(t)).toList();
	}

	/**
	 * Alias for {@link #defaultTools()}.
	 *
	 * @return a list of default BuiltinTools
	 */
	public static List<BuiltinTools> defaults() {
		return defaultTools();
	}

	/**
	 * Returns an empty tool list (no builtin tools).
	 *
	 * @return an empty list of BuiltinTools
	 */
	public static List<BuiltinTools> none() {
		return List.of();
	}
}
