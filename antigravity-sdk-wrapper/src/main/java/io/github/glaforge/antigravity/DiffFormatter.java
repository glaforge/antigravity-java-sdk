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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility for formatting {@code EditFileResultStep.diff_block} structures into
 * human-readable unified diff strings.
 */
public final class DiffFormatter {

	private DiffFormatter() {
	}

	/**
	 * Formats a list of serialized {@code DiffBlock} maps ({@code start_line},
	 * {@code end_line}, and {@code lines} with {@code action} and {@code text}) as
	 * a unified diff string.
	 *
	 * @param diffBlocks
	 *            list of diff block maps
	 * @return formatted unified diff string
	 */
	public static String formatDiffBlocks(List<? extends Map<String, ?>> diffBlocks) {
		if (diffBlocks == null || diffBlocks.isEmpty()) {
			return "";
		}
		List<String> chunks = new ArrayList<>();
		for (Map<String, ?> block : diffBlocks) {
			if (block == null) {
				continue;
			}
			Object startObj = block.containsKey("start_line") ? block.get("start_line") : block.get("startLine");
			Object endObj = block.containsKey("end_line") ? block.get("end_line") : block.get("endLine");
			int start = startObj instanceof Number n ? n.intValue() : 0;
			int end = endObj instanceof Number n ? n.intValue() : 0;

			List<String> lines = new ArrayList<>();
			lines.add("@@ -" + start + " +" + end + " @@");

			Object rawLines = block.get("lines");
			if (rawLines instanceof Iterable<?> lineIter) {
				for (Object item : lineIter) {
					if (item instanceof Map<?, ?> lineMap) {
						String action = String.valueOf(lineMap.get("action"));
						String prefix = actionPrefix(action);
						Object textObj = lineMap.get("text");
						String text = textObj != null ? String.valueOf(textObj) : "";
						lines.add(prefix + text);
					}
				}
			}
			chunks.add(String.join("\n", lines));
		}
		return String.join("\n", chunks);
	}

	/**
	 * Formats a Jackson {@link JsonNode} array of {@code DiffBlock} objects as a
	 * unified diff string.
	 *
	 * @param diffBlocksNode
	 *            JSON array node containing diff blocks
	 * @return formatted unified diff string
	 */
	public static String formatDiffBlocks(JsonNode diffBlocksNode) {
		if (diffBlocksNode == null || !diffBlocksNode.isArray() || diffBlocksNode.isEmpty()) {
			return "";
		}
		List<String> chunks = new ArrayList<>();
		for (JsonNode block : diffBlocksNode) {
			if (block == null || !block.isObject()) {
				continue;
			}
			int start = block.has("start_line") ? block.path("start_line").asInt(0) : block.path("startLine").asInt(0);
			int end = block.has("end_line") ? block.path("end_line").asInt(0) : block.path("endLine").asInt(0);

			List<String> lines = new ArrayList<>();
			lines.add("@@ -" + start + " +" + end + " @@");

			JsonNode linesNode = block.get("lines");
			if (linesNode != null && linesNode.isArray()) {
				for (JsonNode lineNode : linesNode) {
					if (lineNode != null && lineNode.isObject()) {
						String action = lineNode.path("action").asText("");
						String prefix = actionPrefix(action);
						String text = lineNode.path("text").asText("");
						lines.add(prefix + text);
					}
				}
			}
			chunks.add(String.join("\n", lines));
		}
		return String.join("\n", chunks);
	}

	private static String actionPrefix(String action) {
		if ("LINE_ACTION_INSERT".equals(action)) {
			return "+";
		}
		if ("LINE_ACTION_DELETE".equals(action)) {
			return "-";
		}
		return " ";
	}
}
