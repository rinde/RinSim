package rinde.sim.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static rinde.sim.util.StochasticSuppliers.constant;
import static rinde.sim.util.StochasticSuppliers.normal;
import static rinde.sim.util.StochasticSuppliers.uniformDouble;
import static rinde.sim.util.StochasticSuppliers.uniformInt;
import static rinde.sim.util.StochasticSuppliers.uniformLong;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StochasticSuppliersDeterminismTest {
  private final StochasticSupplier<?> supplier;

  public StochasticSuppliersDeterminismTest(StochasticSupplier<?> supp) {
    supplier = supp;
  }

  @Parameters
  public static List<Object[]> configs() {
    return asList(new Object[][] {
        { uniformDouble(0.5, 5.5) },
        { uniformInt(0, 10) },
        { uniformLong(-100, 100) },
        { constant(new Object()) },
        { constant(100L) },
        { normal().buildDouble() },
        { normal().bounds(-1, 1).buildInteger() },
        { normal().mean(100).std(50).buildLong() }
    });
  }

  /**
   * Tests whether repeated invocations with the same seed yields the same
   * result.
   */
  @Test
  public void determinism() {
    final List<Long> seeds = asList(123L, 456L, 789L);
    for (final long seed : seeds) {
      final Object value = supplier.get(seed);
      for (int i = 0; i < 10; i++) {
        assertEquals(value, supplier.get(seed));
      }
    }
  }
}
