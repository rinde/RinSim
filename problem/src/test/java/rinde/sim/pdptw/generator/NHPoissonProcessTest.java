package rinde.sim.pdptw.generator;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import rinde.sim.pdptw.generator.NHPoissonProcess.SineIntensity;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.common.math.DoubleMath;

public class NHPoissonProcessTest {

  @Test
  public void test() {

    final double period = 200;

    final long length = 1000L;
    final SineIntensity si = new SineIntensity(
        2d, 1d / period);

    final ArrivalTimesGenerator atg = new NHPoissonProcess(length, si);

    final List<Long> list = newArrayList();
    for (long l = 0; l < length; l++) {
      final int expected = DoubleMath.roundToInt(10d *
          si.apply(l),
          RoundingMode.HALF_DOWN);
      for (int i = 0; i < expected; i++) {
        list.add(l);
      }
    }

    try {
      final File dest = new File(
          "files/test/times/sine.times");
      Files.createParentDirs(dest);
      Files.write(Joiner.on("\n").join(list), dest, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalArgumentException();
    }

    final RandomGenerator rng = new MersenneTwister(123);

    for (int i = 0; i < 3; i++) {
      final List<Long> times = atg.generate(rng);

      System.out.printf("%d %1.3f\n", i,
          Metrics.measureDynamismDistr(times, 1000L) * 100d);

      try {
        final File dest = new File(
            "files/test/times/orders" + i + ".times");
        Files.createParentDirs(dest);
        Files.write(Joiner.on("\n").join(times), dest, Charsets.UTF_8);
      } catch (final IOException e) {
        throw new IllegalArgumentException();
      }

    }
  }
}
