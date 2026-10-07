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
package io.github.glaforge.antigravity.beta;

import io.github.glaforge.antigravity.Agent;
import java.util.Objects;

/**
 * Dedicated namespace view providing access to experimental and preview
 * features on an {@link Agent} instance without polluting the stable API
 * surface.
 */
@Beta
public final class BetaOperations {

	private final Agent agent;

	/**
	 * Creates a new BetaOperations namespace view bound to the specified agent.
	 *
	 * @param agent
	 *            bound agent instance
	 */
	public BetaOperations(Agent agent) {
		this.agent = Objects.requireNonNull(agent, "agent must not be null");
	}

	/**
	 * Returns the underlying agent instance.
	 *
	 * @return agent instance
	 */
	public Agent agent() {
		return agent;
	}
}
