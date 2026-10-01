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
	START_SUBAGENT("start_subagent"),
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
	/** Finish the conversation and return structured output. */
	FINISH("finish");

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
		return List.of(VIEW_FILE, CREATE_FILE, EDIT_FILE, ASK_QUESTION, START_SUBAGENT, GENERATE_IMAGE, SEARCH_WEB,
				READ_URL_CONTENT, SCHEDULE, MANAGE_TASK, FINISH);
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
