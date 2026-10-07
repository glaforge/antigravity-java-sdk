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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glaforge.antigravity.beta.Beta;
import io.github.glaforge.antigravity.beta.BetaOperations;
import io.github.glaforge.antigravity.hooks.AgentHook;
import io.github.glaforge.antigravity.tools.Tool;
import io.github.glaforge.antigravity.tools.Param;
import io.github.glaforge.antigravity.tools.ToolRegistry;
import io.github.glaforge.antigravity.tools.ToolDefinition;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity0121Test {

	@Test
	public void testSubagentSkillsConfigInherit() {
		SubagentSkillsConfig cfg = SubagentSkillsConfig.inherit(List.of("skill-1"), List.of("/path/a", "/path/b"));
		assertInstanceOf(SubagentInheritSkillsConfig.class, cfg);

		SubagentInheritSkillsConfig inherit = (SubagentInheritSkillsConfig) cfg;
		assertEquals(2, inherit.extraSkillsPaths().size());
		assertTrue(inherit.extraSkillsPaths().contains("/path/a"));
		assertEquals(List.of("skill-1"), inherit.skillNames());

		// Test factory without extraPaths
		SubagentSkillsConfig cfgNoExtra = SubagentSkillsConfig.inherit(List.of("skill-2"));
		assertInstanceOf(SubagentInheritSkillsConfig.class, cfgNoExtra);
		assertEquals(List.of("skill-2"), ((SubagentInheritSkillsConfig) cfgNoExtra).skillNames());
		assertTrue(((SubagentInheritSkillsConfig) cfgNoExtra).extraSkillsPaths().isEmpty());

		// Test factory with only extra paths
		SubagentSkillsConfig cfgOnlyExtra = SubagentSkillsConfig.inheritWithExtraPaths(List.of("/path/c"));
		assertInstanceOf(SubagentInheritSkillsConfig.class, cfgOnlyExtra);
		assertTrue(((SubagentInheritSkillsConfig) cfgOnlyExtra).skillNames().isEmpty());
		assertEquals(List.of("/path/c"), ((SubagentInheritSkillsConfig) cfgOnlyExtra).extraSkillsPaths());
	}

	@Test
	public void testSubagentSkillsConfigNone() {
		SubagentSkillsConfig cfg = SubagentSkillsConfig.none();
		assertInstanceOf(SubagentNoneSkillsConfig.class, cfg);
	}

	@Test
	public void testSubagentSkillsConfigOverride() {
		SubagentSkillsConfig cfg = SubagentSkillsConfig.override(List.of("/custom/skills"));
		assertInstanceOf(SubagentOverrideSkillsConfig.class, cfg);

		SubagentOverrideSkillsConfig override = (SubagentOverrideSkillsConfig) cfg;
		assertEquals(List.of("/custom/skills"), override.skillsPaths());
	}

	@Test
	public void testSubagentConfigWithSkillsConfig() {
		SubagentConfig subagent = SubagentConfig.builder().name("specialist").description("Specialist agent")
				.instructions("Do specialized work")
				.skillsConfig(SubagentSkillsConfig.override(List.of("/skills/specialist"))).build();

		assertEquals("specialist", subagent.name());
		assertNotNull(subagent.skillsConfig());
		assertInstanceOf(SubagentOverrideSkillsConfig.class, subagent.skillsConfig());
	}

	@Test
	public void testBulkHooksRegistration() {
		AgentHook hook1 = new AgentHook() {
		};
		AgentHook hook2 = new AgentHook() {
		};
		AgentHook hook3 = new AgentHook() {
		};

		// Config builder varargs and collection
		AgentConfig config = AgentConfig.builder().hooks(hook1, hook2).addHooks(List.of(hook3)).build();

		assertEquals(3, config.getHooks().size());
		assertTrue(config.getHooks().contains(hook1));
		assertTrue(config.getHooks().contains(hook2));
		assertTrue(config.getHooks().contains(hook3));

		// Agent builder varargs and collection
		Agent.Builder agentBuilder = Agent.builder().hooks(List.of(hook1)).addHooks(hook2, hook3);

		AgentConfig builtConfig = agentBuilder.buildConfig();
		assertEquals(3, builtConfig.getHooks().size());
	}

	@Test
	public void testCompactionConfigSummaryPromptOverride() {
		CompactionConfig config = CompactionConfig.builder().tokenThreshold(1000)
				.summaryPromptOverride("Summarize in French only").build();

		assertEquals(1000, config.tokenThreshold());
		assertEquals("Summarize in French only", config.summaryPromptOverride());

		// Test record constructor overload
		CompactionConfig config2 = new CompactionConfig(1000, 200, 2000, "Custom summary prompt");
		assertEquals(1000, config2.tokenThreshold());
		assertEquals(200, config2.checkpointIntervalTokens());
		assertEquals(2000, config2.maxContextTokens());
		assertEquals("Custom summary prompt", config2.summaryPromptOverride());
	}

	@Test
	public void testBetaNamespaceAnnotationAndAccess() throws Exception {
		assertTrue(Beta.class.isAnnotation());

		AgentConfig config = AgentConfig.builder().build();
		Agent agent = new Agent(config, false);

		BetaOperations beta = agent.beta();
		assertNotNull(beta);
		assertSame(agent, beta.agent());
		assertSame(config, agent.getConfig());
	}

	static class SampleToolService {
		@Tool(name = "greetWithContext", description = "Tool with ToolContext")
		public String greetWithContext(ToolContext context, @Param(name = "name") String name) {
			return (context != null ? "ContextPresent: " : "NoContext: ") + name;
		}

		@Tool(name = "greetWithOptionalContext", description = "Tool with Optional<ToolContext>")
		public String greetWithOptionalContext(Optional<ToolContext> context, @Param(name = "name") String name) {
			return (context != null && context.isPresent() ? "OptionalPresent: " : "OptionalEmpty: ") + name;
		}
	}

	static class DummyToolContext implements ToolContext {
		private final String id;
		DummyToolContext(String id) {
			this.id = id;
		}
		@Override
		public String getConversationId() {
			return id;
		}
		@Override
		public boolean isIdle() {
			return true;
		}
		@Override
		public void send(String message) {
		}
		@Override
		public Object getState(String key, Object defaultValue) {
			return defaultValue;
		}
		@Override
		public void setState(String key, Object value) {
		}
		@Override
		public ConcurrentMap<String, Object> getStateMap() {
			return null;
		}
	}

	@Test
	public void testToolRegistryOptionalToolContext() throws Exception {
		ToolRegistry registry = new ToolRegistry();
		registry.registerToolsFromObject(new SampleToolService());

		List<ToolDefinition> definitions = registry.getToolDefinitions();
		assertEquals(2, definitions.size());

		// Ensure neither tool exposed context or optional context as a visible
		// parameter schema
		for (ToolDefinition def : definitions) {
			String schema = def.getParametersJsonSchema();
			assertFalse(schema.contains("context"));
			assertTrue(schema.contains("name"));
		}

		ObjectMapper mapper = new ObjectMapper();
		ToolContext mockContext = new DummyToolContext("session-123");

		// Execute greetWithContext
		String res1 = registry.execute("greetWithContext", mapper.readTree("{\"name\": \"Alice\"}"), mockContext);
		assertEquals("{\"result\":\"ContextPresent: Alice\"}", res1);

		// Execute greetWithOptionalContext with context present
		String res2 = registry.execute("greetWithOptionalContext", mapper.readTree("{\"name\": \"Bob\"}"), mockContext);
		assertEquals("{\"result\":\"OptionalPresent: Bob\"}", res2);

		// Execute greetWithOptionalContext with null context
		String res3 = registry.execute("greetWithOptionalContext", mapper.readTree("{\"name\": \"Charlie\"}"), null);
		assertEquals("{\"result\":\"OptionalEmpty: Charlie\"}", res3);
	}
}
