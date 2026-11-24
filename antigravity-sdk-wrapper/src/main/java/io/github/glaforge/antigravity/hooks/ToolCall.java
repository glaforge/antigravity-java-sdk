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
package io.github.glaforge.antigravity.hooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.glaforge.antigravity.BuiltinTools;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a tool call with its arguments and correlation identifiers.
 *
 * @param name
 *            the name of the tool
 * @param args
 *            the JSON arguments provided to the tool
 * @param id
 *            optional unique identifier for the tool call
 * @param stepId
 *            optional trajectory step ID correlating with the step
 * @param serverName
 *            optional MCP server name for MCP tools
 * @param canonicalPath
 *            optional normalized filesystem path extracted from the first path
 *            argument
 */
public record ToolCall(String name, JsonNode args, String id, String stepId, String serverName, String canonicalPath) {

	/**
	 * Ordered list of tool call argument keys that carry wire-format URIs
	 * ({@code file:///...}, {@code cns://...}) and are normalized to clean
	 * filesystem paths.
	 */
	public static final List<String> WIRE_PATH_ARGUMENT_KEYS = List.of("path", "file_path", "directory_path",
			"dir_path", "output_path", "script_path", "ScriptPath");

	/**
	 * Canonical constructor normalizing the tool name, wire path arguments, and
	 * canonical path in a single deterministic pass.
	 */
	public ToolCall {
		name = BuiltinTools.normalizeToolName(name, serverName);
		String resolvedCanonical = normalizePathArguments(args);
		if (canonicalPath == null) {
			canonicalPath = resolvedCanonical;
		}
	}

	/**
	 * Constructor without explicit canonicalPath (derived automatically from args).
	 *
	 * @param name
	 *            the name of the tool
	 * @param args
	 *            the JSON arguments provided to the tool
	 * @param id
	 *            optional unique identifier for the tool call
	 * @param stepId
	 *            optional trajectory step ID correlating with the step
	 * @param serverName
	 *            optional MCP server name for MCP tools
	 */
	public ToolCall(String name, JsonNode args, String id, String stepId, String serverName) {
		this(name, args, id, stepId, serverName, null);
	}

	/**
	 * Convenience constructor for backward compatibility.
	 *
	 * @param name
	 *            the name of the tool
	 * @param args
	 *            the JSON arguments provided to the tool
	 */
	public ToolCall(String name, JsonNode args) {
		this(name, args, null, null, null, null);
	}

	/**
	 * Translates wire-format URIs ({@code file:///...}, {@code cns://...}) to clean
	 * absolute filesystem paths with {@code .} and {@code ..} segments collapsed.
	 *
	 * @param path
	 *            raw path or URI string
	 * @return normalized filesystem path string
	 */
	public static String normalizeWirePath(String path) {
		if (path == null || path.isEmpty()) {
			return path;
		}
		if (path.startsWith("file://")) {
			String decodedPath;
			try {
				URI uri = URI.create(path);
				if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
					decodedPath = uri.getPath();
				} else {
					decodedPath = URLDecoder.decode(path.substring("file://".length()), StandardCharsets.UTF_8);
				}
			} catch (Exception ignored) {
				decodedPath = URLDecoder.decode(path.substring("file://".length()), StandardCharsets.UTF_8);
			}
			return normalizeNativePath(collapseExtraLeadingSlashes(decodedPath));
		}
		if (path.startsWith("cns://")) {
			String cnsSubpath;
			try {
				URI uri = URI.create(path);
				if ("cns".equalsIgnoreCase(uri.getScheme())) {
					String host = uri.getHost() != null ? uri.getHost() : uri.getAuthority();
					String uriPath = uri.getPath() != null ? uri.getPath() : "";
					cnsSubpath = host != null ? host + uriPath : path.substring("cns://".length());
				} else {
					cnsSubpath = path.substring("cns://".length());
				}
			} catch (Exception ignored) {
				cnsSubpath = URLDecoder.decode(path.substring("cns://".length()), StandardCharsets.UTF_8);
			}
			String normalizedSubpath = normalizePosixPath("/" + stripLeadingSlashes(cnsSubpath));
			return "/".equals(normalizedSubpath) ? "/cns" : "/cns" + normalizedSubpath;
		}
		if (!hasNonDriveScheme(path)) {
			return normalizeNativePath(path);
		}
		return path;
	}

	private static String collapseExtraLeadingSlashes(String path) {
		if (path.startsWith("//") && !isWindowsDriveOrUnc()) {
			return "/" + stripLeadingSlashes(path);
		}
		return path;
	}

	private static String stripLeadingSlashes(String path) {
		int i = 0;
		while (i < path.length() && path.charAt(i) == '/') {
			i++;
		}
		return path.substring(i);
	}

	private static boolean isWindowsDriveOrUnc() {
		return java.io.File.separatorChar == '\\';
	}

	private static boolean hasNonDriveScheme(String path) {
		int colon = path.indexOf(':');
		if (colon <= 1) {
			return false;
		}
		for (int i = 0; i < colon; i++) {
			char c = path.charAt(i);
			boolean valid = (i == 0)
					? Character.isLetter(c)
					: (Character.isLetterOrDigit(c) || c == '+' || c == '-' || c == '.');
			if (!valid) {
				return false;
			}
		}
		return true;
	}

	private static String normalizeNativePath(String rawPath) {
		Path normalized = Paths.get(rawPath).normalize();
		String result = normalized.toString();
		if (!isWindowsDriveOrUnc() && result.startsWith("//")) {
			return "/" + stripLeadingSlashes(result);
		}
		return result;
	}

	private static String normalizePosixPath(String rawPath) {
		boolean absolute = rawPath.startsWith("/");
		List<String> stack = new ArrayList<>();
		for (String segment : rawPath.split("/")) {
			if (segment.isEmpty() || ".".equals(segment)) {
				continue;
			}
			if ("..".equals(segment)) {
				if (!stack.isEmpty() && !"..".equals(stack.get(stack.size() - 1))) {
					stack.remove(stack.size() - 1);
				} else if (!absolute) {
					stack.add("..");
				}
			} else {
				stack.add(segment);
			}
		}
		String joined = String.join("/", stack);
		if (absolute) {
			return "/" + joined;
		}
		return joined.isEmpty() ? "." : joined;
	}

	/**
	 * Normalizes wire-format URIs in-place on a {@link JsonNode} tool arguments
	 * object and returns the first canonical path found in priority order.
	 *
	 * @param args
	 *            tool arguments JSON node
	 * @return the canonical path, or null if no path argument is present
	 */
	public static String normalizePathArguments(JsonNode args) {
		if (!(args instanceof ObjectNode obj)) {
			return null;
		}
		String canonical = null;
		for (String key : WIRE_PATH_ARGUMENT_KEYS) {
			JsonNode valNode = obj.get(key);
			if (valNode != null && valNode.isTextual()) {
				String val = valNode.asText();
				if (!val.isEmpty()) {
					String normalized = normalizeWirePath(val);
					obj.put(key, normalized);
					if (canonical == null) {
						canonical = normalized;
					}
				}
			}
		}
		return canonical;
	}

	/**
	 * Normalizes wire-format URIs in-place on a {@link Map} of tool arguments and
	 * returns the first canonical path found in priority order.
	 *
	 * @param args
	 *            mutable tool arguments map
	 * @return the canonical path, or null if no path argument is present
	 */
	public static String normalizePathArguments(Map<String, Object> args) {
		if (args == null || args.isEmpty()) {
			return null;
		}
		String canonical = null;
		for (String key : WIRE_PATH_ARGUMENT_KEYS) {
			Object valObj = args.get(key);
			if (valObj instanceof String val && !val.isEmpty()) {
				String normalized = normalizeWirePath(val);
				args.put(key, normalized);
				if (canonical == null) {
					canonical = normalized;
				}
			}
		}
		return canonical;
	}
}
