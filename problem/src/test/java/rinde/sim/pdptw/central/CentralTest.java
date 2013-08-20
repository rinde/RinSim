/**
 * 
 */
package rinde.sim.pdptw.central;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rinde.sim.pdptw.central.arrays.ArraysSolverValidator;
import rinde.sim.pdptw.central.arrays.MultiVehicleHeuristicSolver;
import rinde.sim.pdptw.central.arrays.MultiVehicleSolverAdapter;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;
import rinde.sim.pdptw.gendreau06.Gendreau06Scenario;
import rinde.sim.ui.View;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class CentralTest {
    @Test
    public void test() throws IOException {
        final List<String> files = getFilesFromDir("data/test/gendreau06/", "");

        final RandomGenerator rng = new MersenneTwister(123);
        // "files/scenarios/gendreau06/req_rapide_1_240_24"
        for (final String file : files) {
            // final String file = "data/test/gendreau06/req_rapide_2_450_24";

            System.out.println(file);
            for (int i = 0; i < 1; i++) {
                final Gendreau06Scenario scenario =
                        Gendreau06Parser.parse(file, 10);

                final Solver s =
                        SolverValidator.wrap(new MultiVehicleSolverAdapter(
                                ArraysSolverValidator
                                        .wrap(new MultiVehicleHeuristicSolver(
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

    public static List<String> getFilesFromDir(String dir, final String suffix) {
        final File directory = new File(dir);
        checkArgument(directory.isDirectory());
        final String[] names = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File d, String name) {
                return name.endsWith(suffix)
                        && new File(d + "/" + name).isFile();
            }
        });
        // sort on file name such that order of returned list does not depend on
        // filesystem ordering.
        Arrays.sort(names);
        final List<String> paths = newArrayList();
        for (final String scen : names) {
            paths.add(dir + scen);
        }
        return paths;
    }
}
