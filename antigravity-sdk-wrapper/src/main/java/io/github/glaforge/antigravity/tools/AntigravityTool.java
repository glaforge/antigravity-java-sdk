package io.github.glaforge.antigravity.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AntigravityTool {
	String name() default ""; // Overrides the tool name sent to Go
	String description() default ""; // Explains what the tool does
}
