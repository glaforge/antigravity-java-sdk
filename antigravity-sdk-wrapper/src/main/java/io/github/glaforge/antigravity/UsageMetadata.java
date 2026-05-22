package io.github.glaforge.antigravity;

public class UsageMetadata {
	private final int promptTokenCount;
	private final int cachedContentTokenCount;
	private final int candidatesTokenCount;
	private final int thoughtsTokenCount;
	private final int totalTokenCount;

	public UsageMetadata(int promptTokenCount, int cachedContentTokenCount, int candidatesTokenCount,
			int thoughtsTokenCount, int totalTokenCount) {
		this.promptTokenCount = promptTokenCount;
		this.cachedContentTokenCount = cachedContentTokenCount;
		this.candidatesTokenCount = candidatesTokenCount;
		this.thoughtsTokenCount = thoughtsTokenCount;
		this.totalTokenCount = totalTokenCount;
	}

	public int getPromptTokenCount() {
		return promptTokenCount;
	}
	public int getCachedContentTokenCount() {
		return cachedContentTokenCount;
	}
	public int getCandidatesTokenCount() {
		return candidatesTokenCount;
	}
	public int getThoughtsTokenCount() {
		return thoughtsTokenCount;
	}
	public int getTotalTokenCount() {
		return totalTokenCount;
	}

	@Override
	public String toString() {
		return "UsageMetadata{" + "prompt=" + promptTokenCount + ", cached=" + cachedContentTokenCount + ", candidates="
				+ candidatesTokenCount + ", thoughts=" + thoughtsTokenCount + ", total=" + totalTokenCount + '}';
	}
}
