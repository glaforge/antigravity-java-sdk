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

import io.github.glaforge.antigravity.localharness.ModelAPIRetryConfig;
import io.github.glaforge.antigravity.localharness.ModelOutputRetryConfig;

/**
 * Retry configuration for transient model API errors and invalid model output
 * retries.
 *
 * @param apiRetry
 *            configuration for model API call retries, or null
 * @param modelOutputRetry
 *            configuration for invalid model output retries, or null
 */
public record RetryConfig(ModelAPIRetryConfigRecord apiRetry, ModelOutputRetryConfigRecord modelOutputRetry) {

	/**
	 * Configuration parameters for model API retries with backoff.
	 *
	 * @param maxRetries
	 *            maximum number of API retries
	 * @param initialSleepDurationMs
	 *            initial sleep duration in milliseconds
	 * @param exponentialMultiplier
	 *            multiplier for exponential backoff
	 * @param jitterRange
	 *            range for backoff jitter
	 */
	public record ModelAPIRetryConfigRecord(int maxRetries, int initialSleepDurationMs, double exponentialMultiplier,
			double jitterRange) {

		/**
		 * Converts this record to the corresponding Protobuf message.
		 *
		 * @return Protobuf ModelAPIRetryConfig
		 */
		public ModelAPIRetryConfig toProtobuf() {
			return ModelAPIRetryConfig.newBuilder().setMaxRetries(maxRetries)
					.setInitialSleepDurationMs(initialSleepDurationMs).setExponentialMultiplier(exponentialMultiplier)
					.setJitterRange(jitterRange).build();
		}
	}

	/**
	 * Configuration parameters for retrying invalid model outputs.
	 *
	 * @param maxRetries
	 *            maximum number of output retries
	 */
	public record ModelOutputRetryConfigRecord(int maxRetries) {

		/**
		 * Converts this record to the corresponding Protobuf message.
		 *
		 * @return Protobuf ModelOutputRetryConfig
		 */
		public ModelOutputRetryConfig toProtobuf() {
			return ModelOutputRetryConfig.newBuilder().setMaxRetries(maxRetries).build();
		}
	}

	/**
	 * Returns a preset RetryConfig tailored for benchmark and evaluation workflows.
	 *
	 * @return benchmark RetryConfig preset
	 */
	public static RetryConfig benchmark() {
		return new RetryConfig(new ModelAPIRetryConfigRecord(5, 1000, 2.0, 0.2), new ModelOutputRetryConfigRecord(3));
	}

	/**
	 * Converts this RetryConfig into its Protobuf counterpart.
	 *
	 * @return Protobuf RetryConfig
	 */
	public io.github.glaforge.antigravity.localharness.RetryConfig toProtobuf() {
		var builder = io.github.glaforge.antigravity.localharness.RetryConfig.newBuilder();
		if (apiRetry != null) {
			builder.setApiRetry(apiRetry.toProtobuf());
		}
		if (modelOutputRetry != null) {
			builder.setModelOutputRetry(modelOutputRetry.toProtobuf());
		}
		return builder.build();
	}
}
