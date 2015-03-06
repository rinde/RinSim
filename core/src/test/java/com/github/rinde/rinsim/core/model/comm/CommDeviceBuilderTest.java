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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Rinde van Lon
 *
 */
public class CommDeviceBuilderTest {
  @SuppressWarnings("null")
  CommDeviceBuilder builder;

  @Before
  public void setUp() {
    builder = new CommDeviceBuilder(CommModel.builder().build(),
        mock(CommUser.class));
  }

  /**
   * Tests that comm users should not create more than one device.
   */
  @Test
  public void testCanBeUsedOnlyOnce() {
    boolean fail = false;
    builder.build();
    try {
      builder.build();
    } catch (final IllegalStateException e) {
      fail = true;
    }
    assertTrue(fail);
  }
}
