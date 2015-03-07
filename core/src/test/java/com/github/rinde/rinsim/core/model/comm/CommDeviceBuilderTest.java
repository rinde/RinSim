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
package com.github.rinde.rinsim.core.model.comm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;

/**
 * @author Rinde van Lon
 *
 */
public class CommDeviceBuilderTest {
  @SuppressWarnings("null")
  CommDeviceBuilder builder;

  /**
   * Creates a new default builder.
   */
  @Before
  public void setUp() {
    builder = new CommDeviceBuilder(CommModel.builder()
        .setRandomGenerator(new MersenneTwister(123L))
        .build(),
        mock(CommUser.class));
  }

  /**
   * Tests that comm users should not create more than one device.
   */
  @Test
  public void testCanBuildOnlyOnce() {
    boolean fail = false;
    builder.build();
    try {
      builder.build();
    } catch (final IllegalStateException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Test for input validation of reliability.
   */
  @Test
  public void testSetReliability() {
    assertEquals(0d, builder.setReliability(0).build().getReliability(), 0d);
    setUp();
    assertEquals(1d, builder.setReliability(1).build().getReliability(), 0d);
    boolean fail = false;
    try {
      builder.setReliability(1.0000001);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
    fail = false;
    try {
      builder.setReliability(-0.0000001);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    final CommDeviceBuilder builderWithout =
        new CommDeviceBuilder(CommModel.builder().build(),
            mock(CommUser.class));
    fail = false;
    try {
      builderWithout.setReliability(.5);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Test for input validation of max range.
   */
  @Test
  public void testSetMaxRange() {
    boolean fail = false;
    try {
      builder.setMaxRange(-.0000001);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);

    assertEquals(10d, builder.setMaxRange(10).build().getMaxRange().get(), 0);
    setUp();
    assertEquals(Optional.absent(), builder.build().getMaxRange());
  }
}
