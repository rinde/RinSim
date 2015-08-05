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

import com.github.rinde.rinsim.core.model.pdp.IParcel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * Test for the {@link Parcel#builder(Point, Point)}.
 * @author Rinde van Lon
 */
public class ParcelDTOTest {

  /**
   * Tests whether the default values are correctly applied.
   */
  @Test
  public void defaultsTest() {
    final IParcel defaults = Parcel.builder(new Point(0, 0),
      new Point(2, 2)).buildDTO();

    assertEquals(new Point(0, 0), defaults.getPickupLocation());
    assertEquals(new Point(2, 2), defaults.getDeliveryLocation());
    assertEquals(0, defaults.getOrderAnnounceTime());
    assertEquals(0, defaults.getNeededCapacity(), 0.00001);
    assertEquals(TimeWindow.always(), defaults.getPickupTimeWindow());
    assertEquals(TimeWindow.always(), defaults.getDeliveryTimeWindow());
    assertEquals(0, defaults.getPickupDuration());
    assertEquals(0, defaults.getDeliveryDuration());
  }

  /**
   * Test for changing values in builder.
   */
  @Test
  public void overridingTest() {
    final Parcel.Builder b = Parcel.builder(new Point(0, 0),
      new Point(2, 2));

    final IParcel dto1 = b.timeWindows(TimeWindow.create(7, 10)).buildDTO();
    assertEquals(TimeWindow.create(7, 10), dto1.getPickupTimeWindow());
    assertEquals(TimeWindow.create(7, 10), dto1.getDeliveryTimeWindow());

    final IParcel dto2 =
      b.pickupTimeWindow(TimeWindow.create(8, 11)).buildDTO();
    assertEquals(TimeWindow.create(8, 11), dto2.getPickupTimeWindow());
    assertEquals(TimeWindow.create(7, 10), dto2.getDeliveryTimeWindow());

    final IParcel dto3 = b.serviceDuration(560L).buildDTO();
    assertEquals(TimeWindow.create(8, 11), dto3.getPickupTimeWindow());
    assertEquals(TimeWindow.create(7, 10), dto3.getDeliveryTimeWindow());
    assertEquals(560, dto3.getPickupDuration());
    assertEquals(560, dto3.getDeliveryDuration());

    final IParcel dto4 = b.pickupDuration(230L).buildDTO();
    assertEquals(230, dto4.getPickupDuration());
    assertEquals(560, dto4.getDeliveryDuration());
  }

}
