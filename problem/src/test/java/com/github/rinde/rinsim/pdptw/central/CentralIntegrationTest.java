package com.github.rinde.rinsim.pdptw.central;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.pdptw.central.Central;
import com.github.rinde.rinsim.pdptw.central.RandomSolver;
import com.github.rinde.rinsim.pdptw.central.Solver;
import com.github.rinde.rinsim.pdptw.central.SolverValidator;
import com.github.rinde.rinsim.pdptw.central.arrays.RandomMVArraysSolver;
import com.github.rinde.rinsim.pdptw.experiment.Experiment;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06Scenario;

/**
 * Integration tests for the centralized facade.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
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
        "files/test/gendreau06/req_rapide_1_240_24");
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
   * {@link com.github.rinde.rinsim.pdptw.central.arrays.MultiVehicleArraysSolver} interface.
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
