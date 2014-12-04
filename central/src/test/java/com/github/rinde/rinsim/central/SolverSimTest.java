/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import com.github.rinde.rinsim.central.Solvers.ExtendedStats;
import com.github.rinde.rinsim.central.arrays.ArraysSolverDebugger.MVASDebugger;
import com.github.rinde.rinsim.central.arrays.ArraysSolvers;
import com.github.rinde.rinsim.central.arrays.ArraysSolvers.MVArraysObject;
import com.github.rinde.rinsim.central.arrays.SolutionObject;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.experiment.Experiment;
import com.github.rinde.rinsim.pdptw.experiment.ExperimentResults;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Scenario;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;

/**
 * @author Rinde van Lon
 */
public class SolverSimTest {

  private static final double MS_TO_MIN = 60000d;

  /**
   * Tests whether the simulator produces the same objective value as the
   * solver.
   * @throws IOException In case file loading fails.
   */
  @Test
  public void testOffline() throws IOException {
    final Gendreau06Scenario scenario = Gendreau06Parser.parser()
        .addFile(ScenarioPaths.GENDREAU)
        .offline()
        .parse()
        .get(0);

    final RandomGenerator rng = new MersenneTwister(123);
    for (int i = 0; i < 5; i++) {
      final long seed = rng.nextLong();
      final DebugSolverCreator dsc = new DebugSolverCreator(seed,
          scenario.getTimeUnit());
      final Gendreau06ObjectiveFunction obj = Gendreau06ObjectiveFunction
          .instance();
      final ExperimentResults results = Experiment.build(obj)
          .addConfiguration(Central.solverConfiguration(dsc))
          .addScenario(scenario).perform();
      assertEquals(1, results.results.size());
      assertEquals(1, dsc.arraysSolver.getInputs().size());
      assertEquals(1, dsc.arraysSolver.getOutputs().size());

      final SolutionObject[] sols = dsc.arraysSolver.getOutputs().get(0);
      int objVal = 0;
      for (final SolutionObject sol : sols) {
        objVal += sol.objectiveValue;
      }

      // convert the objective values computed by the solver to the unit of the
      // gendreau benchmark (minutes).
      final UnitConverter converter = scenario.getTimeUnit().getConverterTo(
          NonSI.MINUTE);
      final double objValInMinutes = converter.convert(objVal);

      final GlobalStateObject solverInput = dsc.solver.getInputs().get(0);
      final ImmutableList<ImmutableList<ParcelDTO>> solverOutput = dsc.solver
          .getOutputs().get(0);

      assertEquals(obj.computeCost(results.results.asList().get(0).stats),
          objValInMinutes, 0.2);

      final StatisticsDTO stats = Solvers.computeStats(solverInput,
          solverOutput);
      assertTrue(stats.toString(), obj.isValidResult(stats));
      assertEquals(objValInMinutes, obj.computeCost(stats), 0.1);
      assertEquals(objValInMinutes,
          decomposedCost(solverInput, solverOutput, obj), 0.01);
    }
  }

  /**
   * Tests whether the computation of the objective value in ArraysSolvers and
   * in Solvers produce identical values.
   * @throws IOException When file loading fails.
   */
  @Test
  public void testOnline() throws IOException {

    final Gendreau06Scenario scenario = Gendreau06Parser
        .parse(new File(ScenarioPaths.GENDREAU));

    final DebugSolverCreator dsc = new DebugSolverCreator(123,
        scenario.getTimeUnit());

    final Gendreau06ObjectiveFunction obj = Gendreau06ObjectiveFunction
        .instance();
    Experiment.build(obj).withThreads(1)
        .addConfiguration(Central.solverConfiguration(dsc))
        .addScenario(scenario).repeat(10).perform();

    final MVASDebugger arraysSolver = dsc.arraysSolver;
    final SolverDebugger solver = dsc.solver;

    final int n = solver.getInputs().size();
    assertEquals(n, arraysSolver.getInputs().size());
    assertEquals(n, solver.getOutputs().size());
    assertEquals(n, arraysSolver.getOutputs().size());

    for (int i = 0; i < n; i++) {
      final GlobalStateObject solverInput = solver.getInputs().get(i);

      final ImmutableList<ImmutableList<ParcelDTO>> solverOutput = solver
          .getOutputs().get(i);
      final SolutionObject[] sols = arraysSolver.getOutputs().get(i);
      final MVArraysObject arrInput = arraysSolver.getInputs().get(i);
      assertEquals(solverOutput.size(), sols.length);

      final double arrObjVal = ArraysSolvers.computeTotalObjectiveValue(sols)
          / MS_TO_MIN;
      final double arrOverTime = overTime(sols, arrInput) / MS_TO_MIN;
      final double arrTardiness = computeTardiness(sols, arrInput) / MS_TO_MIN;
      final double arrTravelTime = computeTravelTime(sols, arrInput)
          / MS_TO_MIN;

      final ExtendedStats stats = (ExtendedStats) Solvers.computeStats(
          solverInput, solverOutput);

      // check arrival times
      for (int j = 0; j < sols.length; j++) {
        final SolutionObject sol = sols[j];
        final long[] arraysArrivalTimes = incrArr(sol.arrivalTimes,
            solverInput.time);
        final long[] arrivalTimes = Longs.toArray(stats.arrivalTimes.get(j));
        assertArrayEquals(arraysArrivalTimes, arrivalTimes);
      }

      final double tardiness = obj.tardiness(stats) + obj.overTime(stats);
      final double travelTime = obj.travelTime(stats);

      assertEquals(arrOverTime, obj.overTime(stats), 0.001);
      assertEquals(arrTravelTime, travelTime, 0.01);
      assertEquals(arrTardiness, tardiness, 0.001);
      assertEquals(arrObjVal, obj.computeCost(stats), 0.01);
      assertEquals(arrObjVal,
          decomposedCost(solverInput, solverOutput, obj), 0.01);
    }
  }

  static double decomposedCost(GlobalStateObject gso,
      ImmutableList<ImmutableList<ParcelDTO>> routes, ObjectiveFunction objFunc) {
    double sum = 0d;
    for (int i = 0; i < gso.vehicles.size(); i++) {
      sum += objFunc.computeCost(Solvers.computeStats(gso.withSingleVehicle(i),
          ImmutableList.of(routes.get(i))));
    }
    return sum;
  }

  // increment & convert to long[]
  static long[] incrArr(int[] arr, long incr) {
    final long[] newArr = new long[arr.length];
    for (int i = 0; i < newArr.length; i++) {
      newArr[i] += arr[i] + incr;
    }
    return newArr;
  }

  static int computeTardiness(SolutionObject[] sols, MVArraysObject arr) {
    int total = 0;
    for (int i = 0; i < sols.length; i++) {
      final SolutionObject sol = sols[i];
      total += ArraysSolvers.computeRouteTardiness(sol.route, sol.arrivalTimes,
          arr.serviceTimes, arr.dueDates, arr.remainingServiceTimes[i]);
    }
    return total;
  }

  static int computeTravelTime(SolutionObject[] sols, MVArraysObject arr) {
    int total = 0;
    for (int i = 0; i < sols.length; i++) {
      final SolutionObject sol = sols[i];
      total += ArraysSolvers.computeTotalTravelTime(sol.route, arr.travelTime,
          arr.vehicleTravelTimes[i]);
    }
    return total;
  }

  static int overTime(SolutionObject[] sols, MVArraysObject arr) {
    int overTime = 0;
    for (int i = 0; i < sols.length; i++) {
      final SolutionObject sol = sols[i];
      final int index = sol.route.length - 1;
      assertEquals(0, arr.serviceTimes[sol.route[index]]);
      final int lateness = sol.arrivalTimes[index]
          + arr.serviceTimes[sol.route[index]]
          - arr.dueDates[sol.route[index]];
      if (lateness > 0) {
        overTime += lateness;
      }
    }
    return overTime;
  }
}
