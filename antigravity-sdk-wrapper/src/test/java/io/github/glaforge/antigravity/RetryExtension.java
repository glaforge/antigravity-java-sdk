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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * JUnit Jupiter {@link InvocationInterceptor} that retries test methods
 * annotated with {@link Retry}.
 */
public class RetryExtension implements InvocationInterceptor {

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		Optional<Retry> retryAnnotation = AnnotationSupport.findAnnotation(extensionContext.getRequiredTestMethod(),
				Retry.class);
		if (retryAnnotation.isEmpty()) {
			retryAnnotation = AnnotationSupport.findAnnotation(extensionContext.getRequiredTestClass(), Retry.class);
		}
		if (retryAnnotation.isEmpty()) {
			invocation.proceed();
			return;
		}

		int maxRetries = Math.max(0, retryAnnotation.get().value());
		long delayMs = Math.max(0, retryAnnotation.get().delayMs());

		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				if (attempt == 0) {
					invocation.proceed();
				} else {
					Method method = invocationContext.getExecutable();
					method.setAccessible(true);
					try {
						method.invoke(invocationContext.getTarget().orElse(null),
								invocationContext.getArguments().toArray());
					} catch (InvocationTargetException e) {
						throw e.getTargetException();
					}
				}
				return;
			} catch (Throwable t) {
				if (attempt == maxRetries) {
					throw t;
				}
				System.err.println("Test " + extensionContext.getDisplayName() + " failed on attempt " + (attempt + 1)
						+ " due to: " + t.getMessage() + ". Retrying (" + (attempt + 1) + "/" + maxRetries + ")...");
				if (delayMs > 0) {
					Thread.sleep(delayMs);
				}
			}
		}
	}
}
