package io.github.glaforge.antigravity;

import com.fasterxml.jackson.databind.JsonNode;

public interface Policy {
	enum Decision {
		ALLOW, DENY, PASS
	}

	Decision evaluate(String toolName, JsonNode arguments);
}
