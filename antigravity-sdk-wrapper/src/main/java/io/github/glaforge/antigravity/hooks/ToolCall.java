package io.github.glaforge.antigravity.hooks;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolCall(String name, JsonNode args) {
}
