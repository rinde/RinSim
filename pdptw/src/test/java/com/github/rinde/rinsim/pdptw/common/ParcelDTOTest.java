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
package com.github.rinde.rinsim.pdptw.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * Test for the {@link ParcelDTO#builder(Point, Point)}.
 * @author Rinde van Lon 
 */
public class ParcelDTOTest {

  /**
   * Tests whether the default values are correctly applied.
   */
  @Test
  public void defaultsTest() {
    final ParcelDTO defaults = ParcelDTO.builder(new Point(0, 0),
        new Point(2, 2)).build();

    assertEquals(new Point(0, 0), defaults.pickupLocation);
    assertEquals(new Point(2, 2), defaults.deliveryLocation);
    assertEquals(0, defaults.orderAnnounceTime);
    assertEquals(0, defaults.neededCapacity);
    assertEquals(TimeWindow.ALWAYS, defaults.pickupTimeWindow);
    assertEquals(TimeWindow.ALWAYS, defaults.deliveryTimeWindow);
    assertEquals(0, defaults.pickupDuration);
    assertEquals(0, defaults.deliveryDuration);
  }

  /**
   * Test for changing values in builder.
   */
  @Test
  public void overridingTest() {
    final ParcelDTO.Builder b = ParcelDTO.builder(new Point(0, 0),
        new Point(2, 2));

    final ParcelDTO dto1 = b.timeWindows(new TimeWindow(7, 10)).build();
    assertEquals(new TimeWindow(7, 10), dto1.pickupTimeWindow);
    assertEquals(new TimeWindow(7, 10), dto1.deliveryTimeWindow);

    final ParcelDTO dto2 = b.pickupTimeWindow(new TimeWindow(8, 11)).build();
    assertEquals(new TimeWindow(8, 11), dto2.pickupTimeWindow);
    assertEquals(new TimeWindow(7, 10), dto2.deliveryTimeWindow);

    final ParcelDTO dto3 = b.serviceDuration(560L).build();
    assertEquals(new TimeWindow(8, 11), dto3.pickupTimeWindow);
    assertEquals(new TimeWindow(7, 10), dto3.deliveryTimeWindow);
    assertEquals(560, dto3.pickupDuration);
    assertEquals(560, dto3.deliveryDuration);

    final ParcelDTO dto4 = b.pickupDuration(230L).build();
    assertEquals(230, dto4.pickupDuration);
    assertEquals(560, dto4.deliveryDuration);
  }

}
