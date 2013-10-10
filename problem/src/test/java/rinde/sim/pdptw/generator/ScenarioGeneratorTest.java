/**
 * 
 */
package rinde.sim.pdptw.generator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import rinde.sim.scenario.Scenario;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ScenarioGeneratorTest {

  @Test
  public void test() {
    final ScenarioGenerator sg = ScenarioGenerator.builder()
        .setAnnouncementIntensityPerKm2(1.3d).setOrdersPerAnnouncement(1.3) //
        .setScale(.1, 5) //
        .setScenarioLength(240) //
        .build();
    final RandomGenerator rng = new MersenneTwister(123);

    for (int i = 0; i < 10; i++) {
      final Scenario s = sg.generate(rng);

      Metrics.checkTimeWindowStrictness(s);

      final List<Double> loads = Metrics.measureLoad(s);
      writeLoads(loads, new File("files/generator/load/scenario" + i + ".load"));

      // measure dynamism
      // measure load

      // System.out.println(s.size() + " " + Metrics.measureDynamism(s));
    }

  }

  static void writeLoads(List<Double> loads, File f) {
    final StringBuilder sb = new StringBuilder();
    int i = 0;
    for (; i < loads.size(); i++) {
      sb.append(i).append(" ").append(loads.get(i)).append("\n");
    }
    sb.append(i).append(" ").append(0).append("\n");
    try {
      Files.createParentDirs(f);
      Files.write(sb.toString(), f, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
