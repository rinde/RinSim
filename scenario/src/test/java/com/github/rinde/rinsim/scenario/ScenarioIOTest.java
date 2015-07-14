/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.scenario;

import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.testutil.TestUtil;

/**
 *
 * @author Rinde van Lon
 */
public class ScenarioIOTest {

  /**
   * Tests unreachable code.
   */
  @BeforeClass
  public static void setUpClass() {
    TestUtil.testPrivateConstructor(ScenarioIO.class);
  }

  /**
   * Tests {@link ScenarioIO#readerAdapter(com.google.common.base.Function)}.
   * @throws IOException When IO fails.
   */
  @Test
  public void testReaderAdapter() throws IOException {
    final Scenario s = Scenario.builder()
        .addModel(TimeModel.builder().withTickLength(7L))
        .build();

    final Path tmpDir = Files.createTempDirectory("rinsim-scenario-io-test");
    final Path file = Paths.get(tmpDir.toString(), "test.scen");
    ScenarioIO.write(s, file);

    final Scenario out = ScenarioIO.reader().apply(file);
    final Scenario convertedOut =
        verifyNotNull(ScenarioIO.readerAdapter(ScenarioConverters.toRealtime())
            .apply(file));

    assertThat(s).isEqualTo(out);
    assertThat(s).isNotEqualTo(convertedOut);
    assertThat(convertedOut.getModelBuilders())
        .contains(TimeModel.builder()
            .withRealTime()
            .withStartInClockMode(ClockMode.SIMULATED)
            .withTickLength(7L));

    Files.delete(file);
    Files.delete(tmpDir);
  }
}
