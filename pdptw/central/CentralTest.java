/**
 * 
 */
package rinde.sim.pdptw.central;

import java.io.IOException;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rinde.evo4mas.common.ExperimentUtil;
import rinde.sim.pdptw.central.arrays.ArraysSolverValidator;
import rinde.sim.pdptw.central.arrays.MultiVehicleSolverAdapter;
import rinde.sim.pdptw.central.arrays.RandomMVArraysSolver;
import rinde.sim.problem.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.problem.gendreau06.Gendreau06Parser;
import rinde.sim.problem.gendreau06.Gendreau06Scenario;
import rinde.sim.ui.View;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class CentralTest {
    @Test
    public void test() throws IOException {

        final List<String> files =
                ExperimentUtil.getFilesFromDir("files/scenarios/gendreau06/",
                    "");

        final RandomGenerator rng = new MersenneTwister(123);
        // "files/scenarios/gendreau06/req_rapide_1_240_24"
        for (final String file : files) {
            System.out.println(file);
            for (int i = 0; i < 10; i++) {
                final Gendreau06Scenario scenario =
                        Gendreau06Parser.parse(file, 10);

                final Solver s =
                        SolverValidator.wrap(new MultiVehicleSolverAdapter(
                                ArraysSolverValidator
                                        .wrap(new RandomMVArraysSolver(
                                                new MersenneTwister(rng
                                                        .nextLong()))),
                                scenario.getTimeUnit()));

                Central.solve(scenario, s, new Gendreau06ObjectiveFunction(),
                    false);
            }
        }
    }

    @BeforeClass
    public static void setUpClass() {
        View.setAutoClose(true);
        View.setAutoPlay(true);
    }

    @AfterClass
    public static void tearDownClass() {
        View.setAutoClose(false);
        View.setAutoPlay(false);
    }

}
