/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model.rand;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.DependencyProvider;

/**
 * Tests {@link RandomModel}.
 * @author Rinde van Lon
 */
public class RandomModelTest {

  @SuppressWarnings("null")
  RandomModel model;

  /**
   * Create model.
   */
  @Before
  public void setUp() {
    model = RandomModel.builder().build(mock(DependencyProvider.class));
  }

  /**
   * Test of master.
   */
  @Test
  public void testMaster() {
    model.register(new RandomUser() {
      @Override
      public void setRandomGenerator(RandomProvider provider) {
        final RandomGenerator rng = provider.masterInstance();
        assertSame(model.masterRandomGenerator, rng);
        testUnmodifiable(rng);
      }
    });
  }

  /**
   * Should always work.
   */
  @Test
  public void testUnregister() {
    assertTrue(model.unregister(new RandomUser() {
      @Override
      public void setRandomGenerator(RandomProvider provider) {}
    }));
  }

  /**
   * Test new instance.
   */
  @Test
  public void testNewInstance() {
    model.register(new RandomUser() {
      @Override
      public void setRandomGenerator(RandomProvider provider) {
        assertNotSame(model.masterRandomGenerator, provider.newInstance());
      }
    });
  }

  /**
   * Not requesting an instance should be fine.
   */
  @Test
  public void testNotUsing() {
    model.register(new RandomUser() {
      @Override
      public void setRandomGenerator(RandomProvider provider) {}
    });
  }

  /**
   * Test of shared instance method.
   */
  @Test
  public void testSharedInstance() {
    final List<RandomGenerator> rngs = new ArrayList<>();
    model.register(new RandomUser() {
      @Override
      public void setRandomGenerator(RandomProvider provider) {
        rngs.add(provider.sharedInstance(List.class));
      }
    });
    model.register(new RandomUser() {
      @Override
      public void setRandomGenerator(RandomProvider provider) {
        rngs.add(provider.sharedInstance(List.class));
      }
    });
    model.register(new RandomUser() {
      @Override
      public void setRandomGenerator(RandomProvider provider) {
        rngs.add(provider.sharedInstance(Collection.class));
      }
    });
    testUnmodifiable(rngs.get(0));
    testUnmodifiable(rngs.get(1));
    testUnmodifiable(rngs.get(2));
    assertSame(rngs.get(0), rngs.get(1));
    assertNotSame(model.masterRandomGenerator, rngs.get(0));
    assertNotSame(model.masterRandomGenerator, rngs.get(2));
  }

  /**
   * Test that calling methods more than once results in an
   * {@link IllegalStateException}.
   */
  @Test
  public void testMaxUsage() {
    model.register(new RandomUser() {
      @Override
      public void setRandomGenerator(RandomProvider provider) {
        provider.getSeed();
        boolean fail = false;
        try {
          provider.getSeed();
        } catch (final IllegalStateException e) {
          fail = true;
        }
        assertTrue(fail);
        fail = false;
        try {
          provider.masterInstance();
        } catch (final IllegalStateException e) {
          fail = true;
        }
        assertTrue(fail);
        fail = false;
        try {
          provider.newInstance();
        } catch (final IllegalStateException e) {
          fail = true;
        }
        assertTrue(fail);
        fail = false;
        try {
          provider.sharedInstance(Object.class);
        } catch (final IllegalStateException e) {
          fail = true;
        }
        assertTrue(fail);
      }
    });
  }

  /**
   * Test that requesting something from the {@link RandomProvider} after the
   * {@link RandomUser#setRandomGenerator(RandomProvider)} is complete results
   * in an {@link IllegalStateException}.
   */
  @Test
  public void testTooLateUsage() {
    final List<RandomProvider> rp = new ArrayList<>();
    model.register(new RandomUser() {
      @Override
      public void setRandomGenerator(RandomProvider provider) {
        rp.add(provider);
      }
    });
    boolean fail = false;
    try {
      rp.get(0).masterInstance();
    } catch (final IllegalStateException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  static void testUnmodifiable(RandomGenerator rng) {
    boolean fail = false;
    try {
      rng.setSeed(0);
    } catch (final UnsupportedOperationException e) {
      fail = true;
    }
    assertTrue(fail);
    fail = false;
    try {
      rng.setSeed(new int[] { 0 });
    } catch (final UnsupportedOperationException e) {
      fail = true;
    }
    assertTrue(fail);
    fail = false;
    try {
      rng.setSeed(123L);
    } catch (final UnsupportedOperationException e) {
      fail = true;
    }
    assertTrue(fail);
    fail = false;

    rng.nextBoolean();
    rng.nextBytes(new byte[] {});
    rng.nextDouble();
    rng.nextFloat();
    rng.nextGaussian();
    rng.nextInt();
    rng.nextInt(1);
    rng.nextLong();
  }
}
