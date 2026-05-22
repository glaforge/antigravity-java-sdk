package io.github.glaforge.antigravity;

import com.fasterxml.jackson.databind.JsonNode;

public class DenyAllPolicy implements Policy {
	@Override
	public Decision evaluate(String toolName, JsonNode arguments) {
		return Decision.DENY;
	}
}
