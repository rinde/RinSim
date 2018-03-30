/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.util;

import static com.github.rinde.rinsim.util.StochasticSuppliers.constant;
import static com.github.rinde.rinsim.util.StochasticSuppliers.normal;
import static com.github.rinde.rinsim.util.StochasticSuppliers.uniformDouble;
import static com.github.rinde.rinsim.util.StochasticSuppliers.uniformInt;
import static com.github.rinde.rinsim.util.StochasticSuppliers.uniformLong;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.util.StochasticSupplier;

@RunWith(Parameterized.class)
public class StochasticSuppliersDeterminismTest {
  private final StochasticSupplier<?> supplier;

  public StochasticSuppliersDeterminismTest(StochasticSupplier<?> supp) {
    supplier = supp;
  }

  @Parameters
  public static List<Object[]> configs() {
    return asList(new Object[][] {
      {uniformDouble(0.5, 5.5)},
      {uniformInt(0, 10)},
      {uniformLong(-100, 100)},
      {constant(new Object())},
      {constant(100L)},
      {normal().buildDouble()},
      {normal().bounds(-1, 1).buildInteger()},
      {normal().mean(100).std(50).buildLong()}
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
