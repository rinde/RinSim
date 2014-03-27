/**
 * 
 */
package rinde.sim.pdptw.generator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.generator.loc.NormalLocationsGenerator;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
@RunWith(Parameterized.class)
public class NormalLocationsGeneratorTest {

  private final NormalLocationsGenerator lg;

  public NormalLocationsGeneratorTest(NormalLocationsGenerator l) {
    lg = l;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { /* */
        { new NormalLocationsGenerator(10, .15, .05) }, /* */
        { new NormalLocationsGenerator(10, .18, .05) }, /* */
        { new NormalLocationsGenerator(5, .15, .05) }, /* */
        { new NormalLocationsGenerator(5, .16, .05) }, /* */
        { new NormalLocationsGenerator(5, .17, .05) }, /* */
        { new NormalLocationsGenerator(5, .18, .05) }, /* */
        { new NormalLocationsGenerator(5, .19, .05) }, /* */
        { new NormalLocationsGenerator(5, .20, .05) }, /* */
        { new NormalLocationsGenerator(5, .15, 1.0) }, /* */
        { new NormalLocationsGenerator(5, .2, .1) }, /* */
        { new NormalLocationsGenerator(5.1, .2, .1) }, /* */
        { new NormalLocationsGenerator(3, .2, 1) } /* */

    });
  }

  static final String WORK_DIR = "files/generator/locations/";

  @Test
  public void test() {
    final List<Point> locations = lg.generate(1000, new MersenneTwister(123));

    final String fileName = new StringBuilder().append(lg.getEnvSize())
        .append("-").append(lg.getRelativeStd()).append("-")
        .append(lg.getBinSize()).toString();

    final String locationListFileName = new StringBuilder(WORK_DIR)
        .append("locs-").append(fileName).append(".points").toString();

    final String histogramFileName = new StringBuilder(WORK_DIR)
        .append("hist-").append(fileName).append(".data").toString();

    Analysis.writeLocationList(locations, new File(locationListFileName));
    writeGeneratorHistogram(lg, new File(histogramFileName));

  }

  static void writeGeneratorHistogram(NormalLocationsGenerator lg, File f) {
    final double[][] histogram = lg.getHistogram();
    final StringBuilder sb = new StringBuilder();
    sb.append("0.0 0.0\n");
    for (int i = 0; i < histogram.length; i++) {
      sb.append(histogram[i][0]).append(" ").append(histogram[i][1])
          .append("\n");
    }
    try {
      Files.createParentDirs(f);
      Files.write(sb.toString(), f, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
