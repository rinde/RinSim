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
package com.github.rinde.rinsim.central;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.central.arrays.RandomMVArraysSolver;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Scenario;

/**
 * Integration tests for the centralized facade.
 * @author Rinde van Lon
 */
@RunWith(Parameterized.class)
public class CentralIntegrationTest {

  private Gendreau06Scenario scenario;

  private final boolean offline;
  private final boolean allowDiversion;

  public CentralIntegrationTest(boolean offl, boolean allowDiv) {
    offline = offl;
    allowDiversion = allowDiv;
  }

  @Parameters
  public static Collection<Object[]> configs() {
    return Arrays.asList(new Object[][] {//
        { false, true }, { true, true }, { true, false }, { false, false } });
  }

  @Before
  public void setUp() {
    final Gendreau06Parser parser = Gendreau06Parser.parser().addFile(
        ScenarioPaths.GENDREAU);
    if (allowDiversion) {
      parser.allowDiversion();
    }
    if (offline) {
      parser.offline();
    }
    scenario = parser.parse().get(0);
  }

  /**
   * Test of {@link RandomMVArraysSolver} using the
   * {@link com.github.rinde.rinsim.central.arrays.MultiVehicleArraysSolver}
   * interface.
   */
  @Test
  public void test() {
    Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .addScenario(
            scenario)
        .addConfiguration(
            Central.solverConfiguration(RandomMVArraysSolver.solverSupplier()))
        .repeat(3)
        .perform();
  }

  /**
   * Test of {@link RandomSolver} on a scenario using the {@link Solver}
   * interface.
   */
  @Test
  public void testRandomSolver() {
    Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .addScenario(scenario)
        .addConfiguration(
            Central.solverConfiguration(SolverValidator.wrap(RandomSolver
                .supplier())))
        .repeat(6)
        .perform();
  }
}
