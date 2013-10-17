package rinde.sim.pdptw.central;

import java.io.IOException;

import org.junit.Test;

import rinde.sim.pdptw.central.arrays.RandomMVArraysSolver;
import rinde.sim.pdptw.experiment.Experiment;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;

public class CentralIntegrationTest {

  @Test
  public void test() throws IOException {
    // try test in RinLog?
    Experiment
        .build(new Gendreau06ObjectiveFunction())
        .addScenario(
            Gendreau06Parser.parse("files/test/gendreau06/req_rapide_1_240_24",
                3))
        .addConfiguration(
            Central.solverConfiguration(RandomMVArraysSolver.solverSupplier()))
        .repeat(10).perform();

  }
}
