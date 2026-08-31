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

import io.github.glaforge.antigravity.localharness.ActionViewFile;
import io.github.glaforge.antigravity.localharness.HarnessConfig;
import io.github.glaforge.antigravity.localharness.HarnessSideTools;
import io.github.glaforge.antigravity.localharness.ManageTaskToolConfig;
import io.github.glaforge.antigravity.localharness.ScheduleToolConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class FeatureParity0115Test {

	@Test
	public void testManageTaskAndScheduleToolConfigs() {
		ManageTaskToolConfig manageTask = ManageTaskToolConfig.newBuilder().setEnabled(true).build();
		ScheduleToolConfig schedule = ScheduleToolConfig.newBuilder().setEnabled(true).build();

		HarnessSideTools sideTools = HarnessSideTools.newBuilder().setManageTask(manageTask).setSchedule(schedule)
				.build();

		HarnessConfig config = HarnessConfig.newBuilder().setHarnessSideTools(sideTools).build();

		assertTrue(config.hasHarnessSideTools());
		assertTrue(config.getHarnessSideTools().hasManageTask());
		assertTrue(config.getHarnessSideTools().getManageTask().getEnabled());
		assertTrue(config.getHarnessSideTools().hasSchedule());
		assertTrue(config.getHarnessSideTools().getSchedule().getEnabled());
	}

	@Test
	public void testActionViewFileContentOffset() {
		ActionViewFile viewFile = ActionViewFile.newBuilder().setFilePath("src/Main.java").setStartLine(10)
				.setEndLine(50).setContentOffset(1024).build();

		assertEquals("src/Main.java", viewFile.getFilePath());
		assertEquals(10, viewFile.getStartLine());
		assertEquals(50, viewFile.getEndLine());
		assertEquals(1024, viewFile.getContentOffset());
	}
}
