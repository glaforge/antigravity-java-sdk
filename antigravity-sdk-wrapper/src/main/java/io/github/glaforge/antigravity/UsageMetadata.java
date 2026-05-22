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
