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

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a source for Agent Skill definitions — either a filesystem
 * directory path or an in-memory {@link InlineSkill}.
 *
 * @param directoryPath
 *            the absolute or relative path to the directory hosting skill
 *            definitions, or null if backed by an {@link InlineSkill}
 * @param inlineSkill
 *            an in-memory skill definition, or null if backed by a directory
 */
public record SkillSource(String directoryPath, InlineSkill inlineSkill) {

	public SkillSource {
		if (directoryPath == null && inlineSkill == null) {
			throw new NullPointerException("Either directoryPath or inlineSkill must be provided");
		}
		if (directoryPath != null && inlineSkill != null) {
			throw new IllegalArgumentException("SkillSource cannot specify both directoryPath and inlineSkill");
		}
	}

	/**
	 * Creates a directory-backed SkillSource.
	 *
	 * @param directoryPath
	 *            the directory path
	 */
	public SkillSource(String directoryPath) {
		this(Objects.requireNonNull(directoryPath, "directoryPath must not be null"), null);
	}

	/**
	 * Creates an in-memory SkillSource from an {@link InlineSkill}.
	 *
	 * @param inlineSkill
	 *            the inline skill definition
	 */
	public SkillSource(InlineSkill inlineSkill) {
		this(null, Objects.requireNonNull(inlineSkill, "inlineSkill must not be null"));
	}

	/**
	 * Creates a SkillSource from a Path.
	 *
	 * @param path
	 *            the path to the skills directory
	 * @return a new SkillSource
	 */
	public static SkillSource fromPath(Path path) {
		Objects.requireNonNull(path, "path must not be null");
		return new SkillSource(path.toAbsolutePath().normalize().toString());
	}

	/**
	 * Creates a SkillSource from a directory path string.
	 *
	 * @param directoryPath
	 *            the directory path
	 * @return a new SkillSource
	 */
	public static SkillSource fromDirectory(String directoryPath) {
		return new SkillSource(directoryPath);
	}

	/**
	 * Creates a SkillSource from an in-memory {@link InlineSkill}.
	 *
	 * @param inlineSkill
	 *            the inline skill
	 * @return a new SkillSource
	 */
	public static SkillSource fromInlineSkill(InlineSkill inlineSkill) {
		return new SkillSource(inlineSkill);
	}
}
