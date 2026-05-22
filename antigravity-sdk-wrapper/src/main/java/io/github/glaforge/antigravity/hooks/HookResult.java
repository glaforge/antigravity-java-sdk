package io.github.glaforge.antigravity.hooks;

public record HookResult(boolean allow) {
	public static HookResult allowed() {
		return new HookResult(true);
	}
	public static HookResult denied() {
		return new HookResult(false);
	}
}
