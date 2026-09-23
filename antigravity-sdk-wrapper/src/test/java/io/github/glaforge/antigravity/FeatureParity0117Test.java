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

import io.github.glaforge.antigravity.localharness.HarnessConfig;
import io.github.glaforge.antigravity.localharness.InitializeConversationResponse;
import static io.github.glaforge.antigravity.localharness.BudgetConfig.BudgetScope.BUDGET_SCOPE_FORWARD_LOOKING;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity0117Test {

	@Test
	public void testBudgetScopeAndConfig() {
		// Default scope is LIFETIME
		BudgetConfig defaultBudget = BudgetConfig.builder().maxModelCalls(10).maxToolCalls(25).maxInputTokens(5000L)
				.maxOutputTokens(2000L).maxTotalTokens(7000L).build();

		assertEquals(BudgetScope.LIFETIME, defaultBudget.scope());
		assertEquals(10, defaultBudget.maxModelCalls());
		assertEquals(25, defaultBudget.maxToolCalls());
		assertEquals(5000L, defaultBudget.maxInputTokens());
		assertEquals(2000L, defaultBudget.maxOutputTokens());
		assertEquals(7000L, defaultBudget.maxTotalTokens());

		// Forward-looking budget
		BudgetConfig forwardBudget = BudgetConfig.builder().maxModelCalls(5).scope(BudgetScope.FORWARD_LOOKING).build();

		assertEquals(BudgetScope.FORWARD_LOOKING, forwardBudget.scope());

		// Wire conversion to protobuf
		HarnessConfig.Builder harnessBuilder = HarnessConfig.newBuilder();
		harnessBuilder.getBudgetConfigBuilder().setMaxModelCalls(forwardBudget.maxModelCalls())
				.setScope(BUDGET_SCOPE_FORWARD_LOOKING);

		HarnessConfig protoHarness = harnessBuilder.build();
		assertEquals(BUDGET_SCOPE_FORWARD_LOOKING, protoHarness.getBudgetConfig().getScope());
		assertEquals(5, protoHarness.getBudgetConfig().getMaxModelCalls());
	}

	@Test
	public void testCompactionConfig() {
		CompactionConfig simple = CompactionConfig.of(16000);
		assertEquals(16000, simple.tokenThreshold());
		assertNull(simple.checkpointIntervalTokens());
		assertNull(simple.maxContextTokens());

		CompactionConfig full = CompactionConfig.builder().tokenThreshold(20000).checkpointIntervalTokens(4000)
				.maxContextTokens(32000).build();

		assertEquals(20000, full.tokenThreshold());
		assertEquals(4000, full.checkpointIntervalTokens());
		assertEquals(32000, full.maxContextTokens());

		// Protobuf conversion
		HarnessConfig.Builder harnessCompactionBuilder = HarnessConfig.newBuilder();
		harnessCompactionBuilder.getCompactionConfigBuilder().setTokenThreshold(full.tokenThreshold())
				.setCheckpointIntervalTokens(full.checkpointIntervalTokens())
				.setMaxContextTokens(full.maxContextTokens());
		harnessCompactionBuilder.setCompactionThreshold(full.tokenThreshold());
		HarnessConfig harnessConfig = harnessCompactionBuilder.build();

		assertTrue(harnessConfig.hasCompactionConfig());
		assertEquals(20000, harnessConfig.getCompactionConfig().getTokenThreshold());
		assertEquals(4000, harnessConfig.getCompactionConfig().getCheckpointIntervalTokens());
		assertEquals(32000, harnessConfig.getCompactionConfig().getMaxContextTokens());
		assertEquals(20000, harnessConfig.getCompactionThreshold());
	}

	@Test
	public void testUsageMetadataArithmeticAdd() {
		UsageMetadata usage1 = new UsageMetadata(100, 20, 50, 10, 160, "priority", List.of(), List.of(), List.of(),
				List.of());
		UsageMetadata usage2 = new UsageMetadata(50, 10, 25, 5, 80, "priority", List.of(), List.of(), List.of(),
				List.of());

		UsageMetadata sum = usage1.add(usage2);
		assertEquals(150, sum.promptTokenCount());
		assertEquals(30, sum.cachedContentTokenCount());
		assertEquals(75, sum.candidatesTokenCount());
		assertEquals(15, sum.thoughtsTokenCount());
		assertEquals(240, sum.totalTokenCount());
		assertEquals("priority", sum.serviceTier());

		// plus alias
		assertEquals(sum, usage1.plus(usage2));

		// Tier merging: differing tiers default to "standard"
		UsageMetadata standardUsage = new UsageMetadata(10, 0, 5, 0, 15, "standard", List.of(), List.of(), List.of(),
				List.of());
		UsageMetadata mixed = usage1.add(standardUsage);
		assertEquals("standard", mixed.serviceTier());

		// Tier merging: null tier inherits other's tier
		UsageMetadata untiered = new UsageMetadata(10, 0, 5, 0, 15);
		UsageMetadata mixedNull = untiered.add(usage1);
		assertEquals("priority", mixedNull.serviceTier());

		// Null other returns this
		assertSame(usage1, usage1.add(null));
	}

	@Test
	public void testUsageMetadataArithmeticSubtract() {
		UsageMetadata usage1 = new UsageMetadata(100, 20, 50, 10, 160, "priority", List.of(), List.of(), List.of(),
				List.of());
		UsageMetadata usage2 = new UsageMetadata(30, 5, 15, 2, 47, "priority", List.of(), List.of(), List.of(),
				List.of());

		UsageMetadata diff = usage1.subtract(usage2);
		assertEquals(70, diff.promptTokenCount());
		assertEquals(15, diff.cachedContentTokenCount());
		assertEquals(35, diff.candidatesTokenCount());
		assertEquals(8, diff.thoughtsTokenCount());
		assertEquals(113, diff.totalTokenCount());
		assertEquals("priority", diff.serviceTier());

		// minus alias
		assertEquals(diff, usage1.minus(usage2));

		// Null other returns this
		assertSame(usage1, usage1.subtract(null));
	}

	@Test
	public void testUsageMetadataArithmeticMultiply() {
		UsageMetadata usage = new UsageMetadata(100, 20, 50, 10, 160, "priority", List.of(), List.of(), List.of(),
				List.of());

		UsageMetadata scaled = usage.multiply(1.5);
		assertEquals(150, scaled.promptTokenCount());
		assertEquals(30, scaled.cachedContentTokenCount());
		assertEquals(75, scaled.candidatesTokenCount());
		assertEquals(15, scaled.thoughtsTokenCount());
		assertEquals(240, scaled.totalTokenCount());
		assertEquals("priority", scaled.serviceTier());

		// times alias
		assertEquals(scaled, usage.times(1.5));

		// Scale by zero
		UsageMetadata zeroed = usage.multiply(0.0);
		assertEquals(0, zeroed.totalTokenCount());

		// Invalid factor assertions
		assertThrows(IllegalArgumentException.class, () -> usage.multiply(-1.0));
		assertThrows(IllegalArgumentException.class, () -> usage.multiply(Double.NaN));
		assertThrows(IllegalArgumentException.class, () -> usage.multiply(Double.POSITIVE_INFINITY));
	}

	@Test
	public void testBuiltinToolsDefaultsAndMinimal() {
		List<BuiltinTools> defaultTools = BuiltinTools.defaultTools();
		assertFalse(defaultTools.contains(BuiltinTools.ASK_QUESTION),
				"BuiltinTools.defaultTools() must exclude ASK_QUESTION for headless pipelines");
		assertTrue(defaultTools.contains(BuiltinTools.RUN_COMMAND));
		assertTrue(defaultTools.contains(BuiltinTools.VIEW_FILE));
		assertTrue(defaultTools.contains(BuiltinTools.CREATE_FILE));
		assertTrue(defaultTools.contains(BuiltinTools.EDIT_FILE));
		assertTrue(defaultTools.contains(BuiltinTools.FINISH));

		assertEquals(defaultTools, BuiltinTools.defaults());

		List<BuiltinTools> minimal = BuiltinTools.minimal();
		assertEquals(6, minimal.size());
		assertTrue(minimal.contains(BuiltinTools.RUN_COMMAND));
		assertTrue(minimal.contains(BuiltinTools.VIEW_FILE));
		assertTrue(minimal.contains(BuiltinTools.CREATE_FILE));
		assertTrue(minimal.contains(BuiltinTools.EDIT_FILE));
		assertTrue(minimal.contains(BuiltinTools.LIST_DIR));
		assertTrue(minimal.contains(BuiltinTools.SEARCH_DIR));
		assertFalse(minimal.contains(BuiltinTools.ASK_QUESTION));
		assertFalse(minimal.contains(BuiltinTools.GENERATE_IMAGE));
	}

	@Test
	public void testSandboxStatus() {
		SandboxStatus available = new SandboxStatus(true);
		assertTrue(available.available());
		assertNull(available.unavailableReason());

		SandboxStatus unavailable = new SandboxStatus(false, "exebox sandbox driver unavailable");
		assertFalse(unavailable.available());
		assertEquals("exebox sandbox driver unavailable", unavailable.unavailableReason());

		// Protobuf wire compatibility
		InitializeConversationResponse resp = InitializeConversationResponse.newBuilder()
				.setSandboxStatus(InitializeConversationResponse.newBuilder().getSandboxStatusBuilder()
						.setAvailable(false).setUnavailableReason("unsupported platform").build())
				.build();

		assertFalse(resp.getSandboxStatus().getAvailable());
		assertEquals("unsupported platform", resp.getSandboxStatus().getUnavailableReason());
	}

	@Test
	public void testAllPlatformsHarnessBinariesExist() throws Exception {
		String[] slices = {"linux-x86_64", "linux-aarch64", "osx-aarch64", "osx-x86_64", "windows-x86_64",
				"windows-aarch64"};
		HarnessDownloader downloader = new HarnessDownloader();
		for (String slice : slices) {
			String url = downloader.resolveWheelUrl(slice);
			assertNotNull(url, "Upstream wheel URL should be resolvable for platform slice: " + slice);
			assertTrue(url.contains(slice.contains("win") ? "win" : (slice.contains("osx") ? "macosx" : "manylinux")));
		}
	}
}
