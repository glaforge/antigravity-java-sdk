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

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RetryExtensionTest {

	private final AtomicInteger attempts = new AtomicInteger(0);

	@Test
	@Retry(value = 2, delayMs = 5)
	public void testRetryTwiceAndSucceedOnThirdAttempt() {
		int currentAttempt = attempts.incrementAndGet();
		assertEquals(3, currentAttempt, "Should fail on attempts 1 and 2, and succeed on attempt 3 (retry #2)");
	}
}
