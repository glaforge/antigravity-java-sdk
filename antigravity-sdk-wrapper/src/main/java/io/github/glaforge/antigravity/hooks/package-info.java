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

/**
 * Contains lifecycle hooks for the Antigravity Agent.
 * <p>
 * The hooks in this package follow the core architectural pillars of the
 * Antigravity SDK:
 * <ul>
 * <li><b>{@link io.github.glaforge.antigravity.hooks.InspectHook Inspect
 * Hooks}</b>: Read-Only, Non-Blocking. For logging, audit trails, and metrics.
 * <li><b>{@link io.github.glaforge.antigravity.hooks.DecideHook Decide
 * Hooks}</b>: Read-Only, Blocking. For custom approval/denial logic and
 * policies.
 * <li><b>{@link io.github.glaforge.antigravity.hooks.TransformHook Transform
 * Hooks}</b>: Modifying, Blocking. For sanitizing data in transit or recovering
 * from errors.
 * </ul>
 */
package io.github.glaforge.antigravity.hooks;
