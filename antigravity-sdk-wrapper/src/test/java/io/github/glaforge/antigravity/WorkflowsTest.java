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

import io.github.glaforge.antigravity.beta.WorkflowDefinition;
import io.github.glaforge.antigravity.beta.WorkflowException;
import io.github.glaforge.antigravity.beta.WorkflowProgress;
import io.github.glaforge.antigravity.beta.WorkflowResult;
import io.github.glaforge.antigravity.beta.Workflows;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class WorkflowsTest {

	public enum Severity {
		LOW, MEDIUM, HIGH
	}

	public record Finding(String file, int line, Severity severity, boolean blocking) {
	}

	public record ReviewReport(boolean passed, List<Finding> findings, String summary) {
	}

	@Test
	public void testBuilderWithJavaTypesRecordsEnumsAndArrays() {
		WorkflowDefinition wf = Workflows.builder("code_review_workflow")
				.description("Multi-stage review using Java types").phase("1. Discover files")
				.agent("files", "Find modified Java files", String[].class).phase("2. Deep review in isolated branch")
				.agent("report", "Review modified files for concurrency bugs", ReviewReport.class, "Security Reviewer",
						"self", "branch")
				.log("Review completed:").logExpr("files", "report").build();

		assertEquals("code_review_workflow", wf.name());
		assertEquals("Multi-stage review using Java types", wf.description());

		String src = wf.source();
		assertTrue(src.contains("phase(\"1. Discover files\")"));
		assertTrue(src.contains(
				"files = await agent(\"Find modified Java files\", schema={\"type\":\"array\",\"items\":{\"type\":\"string\"}})"));
		assertTrue(src.contains("report = await agent(\"Review modified files for concurrency bugs\", schema="));
		assertTrue(src.contains("\"severity\":{\"type\":\"string\",\"enum\":[\"LOW\",\"MEDIUM\",\"HIGH\"]}"));
		assertTrue(src.contains("\"blocking\":{\"type\":\"boolean\"}"));
		assertTrue(src.contains("role=\"Security Reviewer\", type_name=\"self\", workspace=\"branch\")"));
		assertTrue(src.contains("log(\"Review completed:\")"));
		assertTrue(src.contains("log(files, report)"));
	}

	@Test
	public void testBuilderWithParameterizedTypeAndMapOverloads() throws Exception {
		// ParameterizedType for List<Finding>
		Type listOfFindingsType = ReviewReport.class.getDeclaredField("findings").getGenericType();
		assertInstanceOf(ParameterizedType.class, listOfFindingsType);

		WorkflowDefinition wf = Workflows.builder("typed_list_wf")
				.agent("findings", "Extract findings", listOfFindingsType)
				.agent("raw_map", "Summarize", Map.of("type", "string"))
				.agent("full_map", "Detailed step", Map.of("type", "integer"), "Counter", "self", "share")
				.parallel("par_res", "findings", "step_fn").pipeline("pipe_res", "par_res", "step_fn")
				.statement("workflows.log(findings, raw_map, full_map)").build();

		String src = wf.source();
		assertTrue(src.contains("\"type\":\"array\",\"items\":{\"type\":\"object\""));
		assertTrue(src.contains("raw_map = await agent(\"Summarize\", schema={\"type\":\"string\"})"));
		assertTrue(src.contains(
				"full_map = await agent(\"Detailed step\", schema={\"type\":\"integer\"}, role=\"Counter\", type_name=\"self\", workspace=\"share\")"));
		assertTrue(src.contains("par_res = await parallel(findings, step_fn)"));
		assertTrue(src.contains("pipe_res = await pipeline(par_res, step_fn)"));
		assertTrue(src.contains("log(findings, raw_map, full_map)"));
		assertFalse(src.contains("workflows.log"));
	}

	@Test
	public void testQualifierStrippingAndAllowedConstructs() {
		String raw = """
				# Allowed: bounded for loops, async def helpers, if/else, comments with import/while
				beta.workflows.phase("Map-Reduce")
				items = await workflows.agent("List items", schema={"type": "array", "items": {"type": "string"}})

				async def process_item(item):
				    # Note: do not import os or use while loops here
				    msg = "import and while inside string literals are fine"
				    return await beta.workflows.agent(f"Process {item}", schema={"type": "string"})

				results = await workflows.parallel(items, process_item)
				chained = await beta.workflows.pipeline(items, process_item)
				for r in results:
				    if r:
				        workflows.log(r)
				""";

		WorkflowDefinition def = Workflows.define("map_reduce", "Parallel and pipeline test", raw);
		assertFalse(def.source().contains("workflows."));
		assertFalse(def.source().contains("beta."));
		assertTrue(def.source().contains("phase(\"Map-Reduce\")"));
		assertTrue(def.source().contains("await parallel(items, process_item)"));
		assertTrue(def.source().contains("await pipeline(items, process_item)"));
	}

	@Test
	public void testValidationRejectsForbiddenConstructs() {
		// Empty or blank scripts
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource(""));
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("   \n  "));
		assertThrows(WorkflowException.class, () -> Workflows.define("empty", "   "));
		assertThrows(WorkflowException.class, () -> Workflows.builder("empty_builder").build());

		// Forbidden imports
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("import sys\nlog('x')"));
		assertThrows(WorkflowException.class,
				() -> Workflows.validateWorkflowSource("from pathlib import Path\nlog('x')"));

		// Forbidden while loops
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("while cond:\n    log('x')"));

		// Forbidden top-level return and yield
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("return 'done'"));
		assertThrows(WorkflowException.class,
				() -> Workflows.validateWorkflowSource("async def gen():\n    yield 1\n"));
		assertThrows(WorkflowException.class,
				() -> Workflows.validateWorkflowSource("async def gen():\n    yield from items\n"));

		// Forbidden private attribute and dunder access
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("x = ctx._internal\nlog(x)"));
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("x = __builtins__\nlog(x)"));

		// Unbalanced delimiters
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("log(\"unclosed\""));
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("x = [1, 2\nlog(x)"));
		assertThrows(WorkflowException.class, () -> Workflows.validateWorkflowSource("x = {\"a\": 1\nlog(x)"));
	}

	@Test
	public void testCompileTimeStubsThrowOutsideRuntime() {
		assertThrows(IllegalArgumentException.class, () -> Workflows.log());
		assertThrows(IllegalStateException.class, () -> Workflows.log("hello"));
		assertThrows(IllegalStateException.class, () -> Workflows.phase("init"));
		assertThrows(IllegalStateException.class, () -> Workflows.agent("prompt", Map.of("type", "string")));
		assertThrows(IllegalStateException.class,
				() -> Workflows.agent("prompt", Map.of("type", "string"), "role", "self", "inherit"));
		assertThrows(IllegalStateException.class, () -> Workflows.agent("prompt", String.class));
		assertThrows(IllegalStateException.class,
				() -> Workflows.agent("prompt", String.class, "role", "self", "inherit"));
		assertThrows(IllegalStateException.class, () -> Workflows.parallel(List.of("a"), "fn"));
		assertThrows(IllegalStateException.class, () -> Workflows.pipeline(List.of("a"), "fn"));
	}

	@Test
	public void testRunWorkflowExecutionAndMultiStepProgress(@TempDir Path tempDir) throws Exception {
		WorkflowDefinition def = Workflows.builder("multi_step_wf").description("Two-phase workflow").phase("Phase 1")
				.agent("res", "Analyze", ReviewReport.class, "Analyst").logExpr("res").build();

		// Guard: requires both enableSubagents(true) and enableRunWorkflow(true)
		Agent noSubagents = new Agent(AgentConfig.builder()
				.capabilities(CapabilitiesConfig.builder().enableSubagents(false).enableRunWorkflow(true).build())
				.build(), false);
		assertThrows(IllegalArgumentException.class, () -> noSubagents.beta().runWorkflow(def));

		Agent noRunWorkflow = new Agent(AgentConfig.builder()
				.capabilities(CapabilitiesConfig.builder().enableSubagents(true).enableRunWorkflow(false).build())
				.build(), false);
		assertThrows(IllegalArgumentException.class, () -> noRunWorkflow.beta().runWorkflow(def));

		// Valid agent configuration
		Agent agent = new Agent(AgentConfig.builder()
				.capabilities(CapabilitiesConfig.builder().enableSubagents(true).enableRunWorkflow(true).build())
				.workspaceDir(tempDir).build(), false);

		// Missing script file check
		assertThrows(IllegalArgumentException.class,
				() -> agent.beta().runWorkflow(tempDir.resolve("non_existent.py")));

		// Materialized WorkflowDefinition execution with multiple progress updates
		List<WorkflowProgress> seenProgress = new ArrayList<>();
		var future = agent.beta().runWorkflow(def, seenProgress::add);

		// Verify materialized temporary script file was written in workspaceDir
		List<Path> materializedFiles;
		try (var stream = Files.list(tempDir)) {
			materializedFiles = stream
					.filter(p -> p.getFileName().toString().startsWith(".agy_workflow_multi_step_wf_")).toList();
		}
		assertEquals(1, materializedFiles.size());
		Path materializedFile = materializedFiles.get(0);
		assertEquals(def.source(), Files.readString(materializedFile, StandardCharsets.UTF_8));

		// Simulate RUNNING progress update followed by DONE progress update
		agent.handleIncomingMessage(null, """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-wf",
				    "stepIndex": 1,
				    "state": "STATE_RUNNING",
				    "runWorkflow": {
				      "scriptPath": "file:///tmp/multi_step_wf.py",
				      "script": "phase(\\"Phase 1\\")",
				      "description": "Two-phase workflow",
				      "output": "[Phase 1] Starting..."
				    }
				  }
				}
				""");
		agent.handleIncomingMessage(null, """
				{
				  "stepUpdate": {
				    "trajectoryId": "traj-wf",
				    "stepIndex": 1,
				    "state": "STATE_DONE",
				    "runWorkflow": {
				      "scriptPath": "file:///tmp/multi_step_wf.py",
				      "script": "phase(\\"Phase 1\\")",
				      "description": "Two-phase workflow",
				      "output": "[Phase 1] Starting...\\n[Phase 1] Completed!"
				    },
				    "textDelta": "Workflow finished."
				  }
				}
				""");
		agent.handleIncomingMessage(null, "{\"trajectoryStateUpdate\": {\"state\": \"STATE_IDLE\"}}");

		WorkflowResult result = future.get(5, TimeUnit.SECONDS);
		assertEquals(2, seenProgress.size());
		assertEquals("[Phase 1] Starting...", seenProgress.get(0).output());
		assertEquals("[Phase 1] Starting...\n[Phase 1] Completed!", seenProgress.get(1).output());
		assertEquals("/tmp/multi_step_wf.py", result.scriptPath());
		assertEquals("Two-phase workflow", result.description());
		assertEquals("[Phase 1] Starting...\n[Phase 1] Completed!", result.output());
		assertEquals("Workflow finished.", result.text());
		assertFalse(Files.exists(materializedFile), "Materialized temporary workflow script should be cleaned up");
	}
}
