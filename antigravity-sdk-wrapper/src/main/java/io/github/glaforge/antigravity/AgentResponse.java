package io.github.glaforge.antigravity;

public class AgentResponse {
	private final String text;
	private final String thoughts;
	private final UsageMetadata usageMetadata;

	public AgentResponse(String text, String thoughts, UsageMetadata usageMetadata) {
		this.text = text != null ? text : "";
		this.thoughts = thoughts != null ? thoughts : "";
		this.usageMetadata = usageMetadata;
	}

	public String getText() {
		return text;
	}
	public String getThoughts() {
		return thoughts;
	}
	public UsageMetadata getUsageMetadata() {
		return usageMetadata;
	}

	public <T> T getStructuredOutput(Class<T> type) throws Exception {
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		return mapper.readValue(text, type);
	}

	@Override
	public String toString() {
		return text;
	}
}
