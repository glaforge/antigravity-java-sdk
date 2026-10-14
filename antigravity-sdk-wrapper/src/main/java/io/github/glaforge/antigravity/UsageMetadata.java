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
 * Represents the usage metadata and token counts for a generation.
 *
 * @param promptTokenCount
 *            the number of prompt tokens
 * @param cachedContentTokenCount
 *            the number of cached content tokens
 * @param candidatesTokenCount
 *            the number of candidates tokens
 * @param thoughtsTokenCount
 *            the number of thoughts tokens
 * @param totalTokenCount
 *            the total token count
 * @param serviceTier
 *            the service tier used for inference (e.g. "priority", "standard")
 * @param promptTokensDetails
 *            the breakdown of prompt tokens by modality
 * @param cacheTokensDetails
 *            the breakdown of cached tokens by modality
 * @param candidatesTokensDetails
 *            the breakdown of candidate tokens by modality
 * @param toolUsePromptTokensDetails
 *            the breakdown of tool-use prompt tokens by modality
 */
public record UsageMetadata(int promptTokenCount, int cachedContentTokenCount, int candidatesTokenCount,
		int thoughtsTokenCount, int totalTokenCount, String serviceTier, List<ModalityTokenCount> promptTokensDetails,
		List<ModalityTokenCount> cacheTokensDetails, List<ModalityTokenCount> candidatesTokensDetails,
		List<ModalityTokenCount> toolUsePromptTokensDetails) {

	/**
	 * Convenience constructor without detailed modality breakdown.
	 *
	 * @param promptTokenCount
	 *            the number of prompt tokens
	 * @param cachedContentTokenCount
	 *            the number of cached content tokens
	 * @param candidatesTokenCount
	 *            the number of candidates tokens
	 * @param thoughtsTokenCount
	 *            the number of thoughts tokens
	 * @param totalTokenCount
	 *            the total token count
	 */
	public UsageMetadata(int promptTokenCount, int cachedContentTokenCount, int candidatesTokenCount,
			int thoughtsTokenCount, int totalTokenCount) {
		this(promptTokenCount, cachedContentTokenCount, candidatesTokenCount, thoughtsTokenCount, totalTokenCount, null,
				List.of(), List.of(), List.of(), List.of());
	}

	private boolean isZero() {
		return promptTokenCount == 0 && cachedContentTokenCount == 0 && candidatesTokenCount == 0
				&& thoughtsTokenCount == 0 && totalTokenCount == 0 && serviceTier == null
				&& (promptTokensDetails == null || promptTokensDetails.isEmpty())
				&& (cacheTokensDetails == null || cacheTokensDetails.isEmpty())
				&& (candidatesTokensDetails == null || candidatesTokensDetails.isEmpty())
				&& (toolUsePromptTokensDetails == null || toolUsePromptTokensDetails.isEmpty());
	}

	/**
	 * Adds token counts from another UsageMetadata.
	 *
	 * @param other
	 *            the other UsageMetadata to add
	 * @return a new UsageMetadata with summed token counts and merged service tier
	 */
	public UsageMetadata add(UsageMetadata other) {
		if (other == null || other.isZero()) {
			return this;
		}
		if (this.isZero()) {
			return other;
		}
		String mergedTier;
		if (this.serviceTier == null) {
			mergedTier = other.serviceTier();
		} else if (other.serviceTier() == null || this.serviceTier.equalsIgnoreCase(other.serviceTier())) {
			mergedTier = this.serviceTier;
		} else {
			mergedTier = "standard";
		}
		return new UsageMetadata(this.promptTokenCount + other.promptTokenCount(),
				this.cachedContentTokenCount + other.cachedContentTokenCount(),
				this.candidatesTokenCount + other.candidatesTokenCount(),
				this.thoughtsTokenCount + other.thoughtsTokenCount(), this.totalTokenCount + other.totalTokenCount(),
				mergedTier, List.of(), List.of(), List.of(), List.of());
	}

	/**
	 * Alias for {@link #add(UsageMetadata)}.
	 *
	 * @param other
	 *            the other UsageMetadata to add
	 * @return the sum
	 */
	public UsageMetadata plus(UsageMetadata other) {
		return add(other);
	}

	/**
	 * Subtracts token counts of another UsageMetadata from this one.
	 *
	 * @param other
	 *            the other UsageMetadata to subtract
	 * @return a new UsageMetadata with subtracted token counts
	 */
	public UsageMetadata subtract(UsageMetadata other) {
		if (other == null || other.isZero()) {
			return this;
		}
		String mergedTier = this.serviceTier != null ? this.serviceTier : other.serviceTier();
		return new UsageMetadata(this.promptTokenCount - other.promptTokenCount(),
				this.cachedContentTokenCount - other.cachedContentTokenCount(),
				this.candidatesTokenCount - other.candidatesTokenCount(),
				this.thoughtsTokenCount - other.thoughtsTokenCount(), this.totalTokenCount - other.totalTokenCount(),
				mergedTier, List.of(), List.of(), List.of(), List.of());
	}

	/**
	 * Alias for {@link #subtract(UsageMetadata)}.
	 *
	 * @param other
	 *            the other UsageMetadata to subtract
	 * @return the difference
	 */
	public UsageMetadata minus(UsageMetadata other) {
		return subtract(other);
	}

	/**
	 * Returns a new {@link UsageMetadata} with all token counts negated.
	 *
	 * @return negated UsageMetadata
	 */
	public UsageMetadata negate() {
		return new UsageMetadata(-this.promptTokenCount, -this.cachedContentTokenCount, -this.candidatesTokenCount,
				-this.thoughtsTokenCount, -this.totalTokenCount, this.serviceTier, List.of(), List.of(), List.of(),
				List.of());
	}

	/**
	 * Subtracts this {@link UsageMetadata} from a numeric left-hand side
	 * (equivalent to Python's {@code __rsub__}). Only {@code 0} is supported
	 * (yielding {@link #negate()}).
	 *
	 * @param lhs
	 *            left-hand numeric operand (must be 0)
	 * @return negated UsageMetadata when lhs == 0
	 * @throws IllegalArgumentException
	 *             if lhs is non-zero
	 */
	public UsageMetadata subtractFrom(double lhs) {
		if (lhs == 0.0) {
			return negate();
		}
		throw new IllegalArgumentException(
				"Numeric subtraction from UsageMetadata is only supported for 0 (e.g. 0 - usage), got " + lhs);
	}

	/**
	 * Subtracts this {@link UsageMetadata} from another {@link UsageMetadata} (or
	 * null, which is treated as 0).
	 *
	 * @param lhs
	 *            left-hand UsageMetadata operand, or null
	 * @return {@code lhs - this} (or {@code -this} if lhs is null)
	 */
	public UsageMetadata subtractFrom(UsageMetadata lhs) {
		if (lhs == null) {
			return negate();
		}
		return lhs.subtract(this);
	}

	/**
	 * Scales token counts by a non-negative, finite numeric factor.
	 *
	 * @param factor
	 *            finite, non-negative scale factor
	 * @return scaled UsageMetadata
	 * @throws IllegalArgumentException
	 *             if factor is negative, infinite, or NaN
	 */
	public UsageMetadata multiply(double factor) {
		if (!Double.isFinite(factor) || factor < 0) {
			throw new IllegalArgumentException(
					"Multiplication factor must be a finite, non-negative number, got " + factor);
		}
		return new UsageMetadata((int) Math.round(this.promptTokenCount * factor),
				(int) Math.round(this.cachedContentTokenCount * factor),
				(int) Math.round(this.candidatesTokenCount * factor),
				(int) Math.round(this.thoughtsTokenCount * factor), (int) Math.round(this.totalTokenCount * factor),
				this.serviceTier, List.of(), List.of(), List.of(), List.of());
	}

	/**
	 * Alias for {@link #multiply(double)}.
	 *
	 * @param factor
	 *            scale factor
	 * @return scaled UsageMetadata
	 */
	public UsageMetadata times(double factor) {
		return multiply(factor);
	}
}
