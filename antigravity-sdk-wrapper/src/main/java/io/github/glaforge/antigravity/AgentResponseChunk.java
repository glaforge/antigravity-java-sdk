package io.github.glaforge.antigravity;

public class AgentResponseChunk {
	private final String textDelta;
	private final String thoughtsDelta;

	public AgentResponseChunk(String textDelta, String thoughtsDelta) {
		this.textDelta = textDelta != null ? textDelta : "";
		this.thoughtsDelta = thoughtsDelta != null ? thoughtsDelta : "";
	}

	public String getTextDelta() {
		return textDelta;
	}
	public String getThoughtsDelta() {
		return thoughtsDelta;
	}
}
