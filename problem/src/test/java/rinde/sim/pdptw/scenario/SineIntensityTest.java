package rinde.sim.pdptw.scenario;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import rinde.sim.pdptw.scenario.IntensityFunctions.IntensityFunction;
import rinde.sim.pdptw.scenario.IntensityFunctions.SineIntensity;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;

/**
 * Tests for {@link IntensityFunctions#sineIntensity()}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class SineIntensityTest {

  /**
   * Tests correctness of area calculation.
   */
  @Test
  public void areaCorrectnessTest() {
    // integral (0)..(1) max(((1 sin( (2pi x 1) - (.5pi))) + .1),0)
    // = 0.369903
    SineIntensity sine = (SineIntensity) IntensityFunctions.sineIntensity()
        .height(.1)
        .build();
    assertEquals(.369903, sine.area(), 0.000001);
    assertEquals(
        .369903,
        IntensityFunctions.areaByIntegration(sine, 0, 1d / sine.getFrequency()),
        0.000001);

    // integral (0)..(1) max(((1 sin( (2pi x 1) - (.5pi))) + 0),0)
    // = 0.31831
    sine = (SineIntensity) IntensityFunctions.sineIntensity().build();
    assertEquals(.31831, sine.area(), 0.00001);
    assertEquals(
        .31831,
        IntensityFunctions.areaByIntegration(sine, 0, 1d / sine.getFrequency()),
        0.00001);

    // integral (0)..(1) max(((1 sin( (2pi x 1) - (.5pi))) + -.1),0)
    // = 0.269903
    sine = (SineIntensity) IntensityFunctions.sineIntensity().height(-.1)
        .build();
    assertEquals(.269903, sine.area(), 0.000001);
    assertEquals(
        .269903,
        IntensityFunctions.areaByIntegration(sine, 0, 1d / sine.getFrequency()),
        0.000001);

    // integral (0)..(3600) max(((5 sin( (2pi x 1/3600) - (.5pi))) + 10),0)
    // = 36000
    sine = (SineIntensity) IntensityFunctions.sineIntensity()
        .amplitude(5d)
        .period(3600d)
        .height(10)
        .build();
    assertEquals(36000, sine.area(), 0.00001);
    assertEquals(
        36000,
        IntensityFunctions.areaByIntegration(sine, 0, 1d / sine.getFrequency()),
        0.00001);

    // according to Wolfram Alpha:
    // integral (1.84)..(8.15) ((5 sin( (2pi x .1) - (.5pi))) + 2)
    // = 27.2065
    sine = (SineIntensity) IntensityFunctions.sineIntensity()
        .amplitude(5d)
        .frequency(1d / 10d)
        .height(2)
        .build();
    assertEquals(27.2065, sine.area(), 0.0001);
    assertEquals(
        27.2065,
        IntensityFunctions.areaByIntegration(sine, 0, 1d / sine.getFrequency()),
        0.0001);
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
        final SineIntensity si20 = (SineIntensity) IntensityFunctions
            .sineIntensity()
            .period(p)
            .height(h)
            .area(20)
            .build();
        assertEquals(si20.area(), 20d, 0.000000001);

        final SineIntensity si200 = (SineIntensity) IntensityFunctions
            .sineIntensity()
            .period(p)
            .height(h)
            .area(200)
            .build();
        assertEquals(si200.area(), 200d, 0.000000001);

        final SineIntensity si2000 = (SineIntensity) IntensityFunctions
            .sineIntensity()
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

  /**
   * Tests whether phase shifts are correctly implemented.
   */
  // varargs
  @SuppressWarnings("unchecked")
  @Test
  public void testPhaseShift() {
    SineIntensity si = (SineIntensity) IntensityFunctions.sineIntensity()
        .phaseShift(0).build();
    nonZeroCheck(Range.closed(0d, 1d), si, Range.open(0d, .5));

    si = (SineIntensity) IntensityFunctions.sineIntensity().phaseShift(.5)
        .build();
    nonZeroCheck(Range.closed(0d, 1d), si, Range.open(.25, .75));
    // check default
    assertEquals(IntensityFunctions.sineIntensity().build(), si);

    si = (SineIntensity) IntensityFunctions.sineIntensity().phaseShift(1)
        .build();
    nonZeroCheck(Range.closed(0d, 1d), si, Range.open(.5, 1d));

    si = (SineIntensity) IntensityFunctions.sineIntensity().phaseShift(1.5)
        .build();
    nonZeroCheck(Range.closed(0d, 1d), si, Range.closedOpen(0d, .25),
        Range.openClosed(.75, 1d));

    si = (SineIntensity) IntensityFunctions.sineIntensity().phaseShift(2)
        .build();
    nonZeroCheck(Range.closed(0d, 1d), si, Range.open(0d, .5));
  }

  private static <C extends Comparable<?>> ImmutableRangeSet<C> asSet(
      Range<C>... ranges) {
    final ImmutableRangeSet.Builder<C> b = ImmutableRangeSet.builder();
    for (final Range<C> r : ranges) {
      b.add(r);
    }
    return b.build();
  }

  private static void nonZeroCheck(Range<Double> globalRange,
      IntensityFunction intFunc, Range<Double>... nonZeroRanges) {
    final RangeSet<Double> nonZeroRangeSet = asSet(nonZeroRanges);
    for (double d = globalRange.lowerEndpoint(); d <= globalRange
        .upperEndpoint(); d = Math.round((d + .01) * 100d) / 100d) {
      if (nonZeroRangeSet.contains(d)) {
        assertTrue(intFunc.apply(d) > 0);
      } else {
        assertEquals(0d, intFunc.apply(d), 0.000001);
      }
    }
  }
}
