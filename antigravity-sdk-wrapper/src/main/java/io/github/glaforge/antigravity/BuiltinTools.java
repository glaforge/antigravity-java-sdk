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
	 * Returns tools that only read state (no writes, deletes, or commands).
	 *
	 * @return a list of read-only BuiltinTools
	 */
	public static List<BuiltinTools> readOnly() {
		return List.of(LIST_DIR, SEARCH_DIR, FIND_FILE, VIEW_FILE, READ_URL_CONTENT, FINISH);
	}

	/**
	 * Returns tools that cannot delete content.
	 *
	 * @return a list of non-destructive BuiltinTools
	 */
	public static List<BuiltinTools> nondestructive() {
		return List.of(LIST_DIR, SEARCH_DIR, FIND_FILE, VIEW_FILE, CREATE_FILE, EDIT_FILE, ASK_QUESTION, START_SUBAGENT,
				GENERATE_IMAGE, SEARCH_WEB, READ_URL_CONTENT, FINISH);
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
	 * Returns an empty tool list (no builtin tools).
	 *
	 * @return an empty list of BuiltinTools
	 */
	public static List<BuiltinTools> none() {
		return List.of();
	}
}
