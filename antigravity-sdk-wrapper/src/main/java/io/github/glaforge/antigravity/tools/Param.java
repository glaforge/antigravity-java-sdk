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
package io.github.glaforge.antigravity.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to describe a tool parameter, POJO field, record component,
 * or structured schema type.
 */
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {
	/**
	 * The name of the parameter or property in the JSON schema. If empty, the Java
	 * parameter or field name is used (requires -parameters compiler flag for
	 * method parameters).
	 *
	 * @return the parameter or property name
	 */
	String name() default "";

	/**
	 * Explains what the parameter, property, or type represents. This description
	 * is passed to the LLM in the JSON schema.
	 *
	 * @return the description
	 */
	String description() default "";

	/**
	 * Whether this parameter or property is required in the generated JSON schema.
	 *
	 * @return true if required (default), false if optional
	 */
	boolean required() default true;
}
