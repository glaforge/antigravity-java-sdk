package io.github.glaforge.antigravity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PoliciesTest {

	@Test
	public void testAllowAll() {
		Policy policy = Policies.allowAll();
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("any_tool", null));
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("another_tool", null));
	}

	@Test
	public void testDenyAll() {
		Policy policy = Policies.denyAll();
		assertEquals(Policy.Decision.DENY, policy.evaluate("any_tool", null));
		assertEquals(Policy.Decision.DENY, policy.evaluate("another_tool", null));
	}

	@Test
	public void testPassAll() {
		Policy policy = Policies.passAll();
		assertEquals(Policy.Decision.PASS, policy.evaluate("any_tool", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("another_tool", null));
	}

	@Test
	public void testAllowTool() {
		Policy policy = Policies.allowTool("run_command");
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("file_edit", null));
	}

	@Test
	public void testDenyTool() {
		Policy policy = Policies.denyTool("run_command");
		assertEquals(Policy.Decision.DENY, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("file_edit", null));
	}

	@Test
	public void testAllowTools() {
		Policy policy = Policies.allowTools("run_command", "file_edit");
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("file_edit", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("view_file", null));
	}

	@Test
	public void testDenyTools() {
		Policy policy = Policies.denyTools("run_command", "file_edit");
		assertEquals(Policy.Decision.DENY, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.DENY, policy.evaluate("file_edit", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("view_file", null));
	}

	@Test
	public void testAllowIf() {
		Policy policy = Policies.allowIf((tool, args) -> tool.startsWith("run_"));
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.ALLOW, policy.evaluate("run_script", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("file_edit", null));
	}

	@Test
	public void testDenyIf() {
		Policy policy = Policies.denyIf((tool, args) -> tool.startsWith("run_"));
		assertEquals(Policy.Decision.DENY, policy.evaluate("run_command", null));
		assertEquals(Policy.Decision.DENY, policy.evaluate("run_script", null));
		assertEquals(Policy.Decision.PASS, policy.evaluate("file_edit", null));
	}
}
