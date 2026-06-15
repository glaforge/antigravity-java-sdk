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
package io.github.glaforge.antigravity.triggers;

import io.github.glaforge.antigravity.TriggerContext;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Utility class for creating common AgentTriggers.
 */
public final class Triggers {

	private Triggers() {
	}

	/**
	 * Creates a trigger that executes periodically.
	 *
	 * @param delay
	 *            the time to delay between successive executions
	 * @param unit
	 *            the time unit of the delay parameter
	 * @param action
	 *            the action to execute, which receives the TriggerContext
	 * @return a new AgentTrigger
	 */
	public static AgentTrigger every(long delay, TimeUnit unit, Consumer<TriggerContext> action) {
		return new AgentTrigger() {
			private ScheduledExecutorService executor;

			@Override
			public void start(TriggerContext context) {
				executor = Executors.newSingleThreadScheduledExecutor();
				executor.scheduleWithFixedDelay(() -> {
					try {
						action.accept(context);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}, delay, delay, unit);
			}

			@Override
			public void stop() {
				if (executor != null) {
					executor.shutdownNow();
				}
			}
		};
	}
}
