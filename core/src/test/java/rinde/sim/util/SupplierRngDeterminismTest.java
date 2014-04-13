package rinde.sim.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static rinde.sim.util.SupplierRngs.constant;
import static rinde.sim.util.SupplierRngs.normal;
import static rinde.sim.util.SupplierRngs.uniformDouble;
import static rinde.sim.util.SupplierRngs.uniformInt;
import static rinde.sim.util.SupplierRngs.uniformLong;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SupplierRngDeterminismTest {
  private final SupplierRng<?> supplier;

  public SupplierRngDeterminismTest(SupplierRng<?> supp) {
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
        { normal().doubleSupplier() },
        { normal().bounds(-1, 1).integerSupplier() },
        { normal().mean(100).std(50).longSupplier() }
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
