package rinde.sim.pdptw.generator.times;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.junit.Test;

import rinde.sim.pdptw.generator.times.IntensityFunction.IntensityFunctionWrapper;

public class SineIntensityTest {

  /**
   * Tests correctness of area calculation.
   */
  @Test
  public void areaCorrectnessTest() {
    // integral (0)..(1) max(((1 sin( (2pi x 1) - (.5pi))) + .1),0)
    // = 0.369903
    SineIntensity sine = SineIntensity.builder()
        .height(.1)
        .build();
    assertEquals(.369903, sine.area(), 0.000001);
    assertEquals(.369903, area(sine), 0.000001);

    // integral (0)..(1) max(((1 sin( (2pi x 1) - (.5pi))) + 0),0)
    // = 0.31831
    sine = SineIntensity.builder().build();
    assertEquals(.31831, sine.area(), 0.00001);
    assertEquals(.31831, area(sine), 0.00001);

    // integral (0)..(1) max(((1 sin( (2pi x 1) - (.5pi))) + -.1),0)
    // = 0.269903
    sine = SineIntensity.builder().height(-.1).build();
    assertEquals(.269903, sine.area(), 0.000001);
    assertEquals(.269903, area(sine), 0.000001);

    // integral (0)..(3600) max(((5 sin( (2pi x 1/3600) - (.5pi))) + 10),0)
    // = 36000
    sine = SineIntensity.builder()
        .amplitude(5d)
        .period(3600d)
        .height(10)
        .build();
    assertEquals(36000, sine.area(), 0.00001);
    assertEquals(36000, area(sine), 0.00001);

    // according to Wolfram Alpha:
    // integral (1.84)..(8.15) ((5 sin( (2pi x .1) - (.5pi))) + 2)
    // = 27.2065
    sine = SineIntensity.builder()
        .amplitude(5d)
        .frequency(1d / 10d)
        .height(2)
        .build();
    assertEquals(27.2065, sine.area(), 0.0001);
    assertEquals(27.2065, area(sine), 0.0001);
  }

  /**
   * Tests the shape of sine functions with different periods, heights and
   * scales.
   */
  @Test
  public void areaScaleTest() {

    final List<Double> periods = asList(1d, 5d, 10d, 100d);
    final List<Double> heights = asList(-.99, -.5, 0d, .5, 1d);

    for (final double p : periods) {
      for (final double h : heights) {
        final SineIntensity si20 = SineIntensity.builder()
            .period(p)
            .height(h)
            .area(20)
            .build();
        assertEquals(si20.area(), 20d, 0.000000001);

        final SineIntensity si200 = SineIntensity.builder()
            .period(p)
            .height(h)
            .area(200)
            .build();
        assertEquals(si200.area(), 200d, 0.000000001);

        final SineIntensity si2000 = SineIntensity.builder()
            .period(p)
            .height(h)
            .area(2000)
            .build();
        assertEquals(si2000.area(), 2000d, 0.000000001);

        // test that sine functions with larger areas have same shape (except
        // scaled by a factor).
        for (double d = 0d; d < p; d += .1) {
          assertEquals(si20.apply(d) * 10d, si200.apply(d), 0.00001);
          assertEquals(si20.apply(d) * 100d, si2000.apply(d), 0.00001);
        }
      }
    }
  }

  static double area(SineIntensity s) {
    final UnivariateIntegrator ri = new RombergIntegrator(16, 32);
    final double len = 1d / s.getFrequency();
    final double val = ri.integrate(10000000, new IntensityFunctionWrapper(
        s), 0, len);

    return val;
  }
}
