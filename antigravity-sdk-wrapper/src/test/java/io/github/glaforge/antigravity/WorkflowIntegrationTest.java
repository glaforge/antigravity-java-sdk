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
import io.github.glaforge.antigravity.beta.WorkflowProgress;
import io.github.glaforge.antigravity.beta.WorkflowResult;
import io.github.glaforge.antigravity.beta.Workflows;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class WorkflowIntegrationTest {

	public record CalculationResult(int sum, String formula) {
	}

	@Test
	@Retry(2)
	@Timeout(value = 240, unit = TimeUnit.SECONDS)
	public void testRunWorkflowWithTypedSubagentAndProgress(@TempDir Path tempDir) throws Exception {
		WorkflowDefinition workflow = Workflows.builder("math_workflow")
				.description("Compute a sum using a typed subagent and log the result").phase("Calculate sum")
				.agent("calc", "Compute 19 + 23 and return the sum and formula.", CalculationResult.class,
						"Math Calculator")
				.log("Calculation complete").logExpr("calc").build();

		AgentConfig config = AgentConfig.builder().modelName("gemini-3.8-flash").workspaceDir(tempDir).instructions("""
				You are a workflow runner assistant.
				When asked to run a workflow script, immediately invoke the `run_workflow` tool
				with the exact script path provided and summarize the output once complete.
				""").capabilities(CapabilitiesConfig.builder().enableSubagents(true).enableRunWorkflow(true).build())
				.build();

		List<WorkflowProgress> progressUpdates = new CopyOnWriteArrayList<>();

		try (Agent agent = new Agent(config)) {
			CompletableFuture<WorkflowResult> future = agent.beta().runWorkflow(workflow, progressUpdates::add);

			await().atMost(120, TimeUnit.SECONDS).until(future::isDone);
			WorkflowResult result = future.get(5, TimeUnit.SECONDS);

			assertNotNull(result);
			assertNotNull(result.response());
			assertFalse(progressUpdates.isEmpty(), "Expected at least one WorkflowProgress callback");
			assertTrue(result.output().contains("Calculation complete") || result.output().contains("42"),
					"Expected workflow output to contain logged result, got: " + result.output());
		}
	}

	@Test
	@Retry(2)
	@Timeout(value = 240, unit = TimeUnit.SECONDS)
	public void testRunHandwrittenPythonWorkflowScript(@TempDir Path tempDir) throws Exception {
		String pythonWorkflow = """
				phase("1. Parallel capital lookup")
				countries = ["France", "Japan"]

				async def lookup_capital(country):
				    return await agent(
				        f"What is the capital of {country}? Reply with just the city name.",
				        schema={
				            "type": "object",
				            "properties": {
				                "country": {"type": "string"},
				                "capital": {"type": "string"}
				            },
				            "required": ["country", "capital"]
				        },
				        role="Geography Lookup"
				    )

				results = await parallel(countries, lookup_capital)

				phase("2. Log results")
				for entry in results:
				    log(f"{entry['country']} -> {entry['capital']}")
				""";

		Path scriptFile = tempDir.resolve("capitals_workflow.py");
		Files.writeString(scriptFile, pythonWorkflow, StandardCharsets.UTF_8);

		AgentConfig config = AgentConfig.builder().modelName("gemini-3.8-flash").workspaceDir(tempDir).instructions("""
				You are a workflow runner assistant.
				When asked to run a workflow script, immediately invoke the `run_workflow` tool
				with the exact script path provided and summarize the output once complete.
				""").capabilities(CapabilitiesConfig.builder().enableSubagents(true).enableRunWorkflow(true).build())
				.build();

		List<WorkflowProgress> progressUpdates = new CopyOnWriteArrayList<>();

		try (Agent agent = new Agent(config)) {
			CompletableFuture<WorkflowResult> future = agent.beta().runWorkflow(scriptFile,
					"Look up capitals in parallel", progressUpdates::add);

			await().atMost(120, TimeUnit.SECONDS).until(future::isDone);
			WorkflowResult result = future.get(5, TimeUnit.SECONDS);

			assertNotNull(result);
			assertFalse(progressUpdates.isEmpty(), "Expected at least one WorkflowProgress callback");
			String outputLower = result.output().toLowerCase();
			assertTrue(outputLower.contains("paris") && outputLower.contains("tokyo"),
					"Expected workflow output to contain Paris and Tokyo, got: " + result.output());
		}
	}
}
