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

import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.measure.Measure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.TestModelProvider;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.core.pdptw.DefaultDepot;
import com.github.rinde.rinsim.core.pdptw.DefaultParcel;
import com.github.rinde.rinsim.core.pdptw.DefaultVehicle;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;

public abstract class PDPRoadModelCommonTest {
  static final TimeLapse TIME = TimeLapseFactory.create(0, 1);
  static final TimeWindow DEFAULT_TW = new TimeWindow(0, 100);

  DefaultParcel dp1;
  DefaultParcel dp2;
  DefaultParcel dp3;
  DefaultDepot depot;

  DefaultVehicle dv1;
  DefaultVehicle dv2;

  PDPRoadModel rm;
  PDPModel pm;

  protected final boolean allowDiversion;

  public PDPRoadModelCommonTest(boolean diversion) {
    allowDiversion = diversion;
  }

  /**
   * Sets up a test scenario.
   */
  @Before
  public void setUp() {
    rm = new PDPRoadModel(new PlaneRoadModel(new Point(0, 0),
        new Point(10, 10), SI.KILOMETER, Measure.valueOf(0.1,
            NonSI.KILOMETERS_PER_HOUR)), allowDiversion);
    pm = new DefaultPDPModel(TimeWindowPolicies.TARDY_ALLOWED);
    final ModelProvider mp = new TestModelProvider(asList(pm, rm));
    rm.registerModelProvider(mp);
    pm.registerModelProvider(mp);

    dp1 = create(new Point(1, 0), new Point(0, 7));
    dp2 = create(new Point(5, 0), new Point(0, 5));
    dp3 = create(new Point(1, 0), new Point(0, 6));
    for (final DefaultParcel dp : asList(dp1, dp2, dp3)) {
      rm.addObjectAt(dp, dp.dto.pickupLocation);
      pm.register(dp);
    }

    final Point depotLocation = new Point(5, 5);
    depot = new DefaultDepot(depotLocation);
    rm.addObjectAt(depot, depotLocation);

    dv1 = new TestVehicle(new Point(0, 0));
    dv2 = new TestVehicle(new Point(0, 0));
    for (final DefaultVehicle tv : asList(dv1, dv2)) {
      rm.addObjectAt(tv, tv.getDTO().startPosition);
      pm.register(tv);
    }

    // to satisfy coverage tool
    PDPRoadModel.DestType.valueOf("DEPOT").toString();
  }

  /**
   * A vehicle should always be allowed to go back the depot, even multiple
   * times.
   */
  @Test
  public void revisitDepot() {
    // move to depot
    rm.moveTo(dv1, depot, time(1));
    // move to pickup location
    rm.moveTo(dv1, dp1, time(100));
    // pickup
    pm.service(dv1, dp1, time(100));
    // move to delivery location
    rm.moveTo(dv1, dp1, time(100));
    // deliver
    pm.service(dv1, dp1, time(100));
    // move to depot again
    rm.moveTo(dv1, depot, time(1));
  }

  @Test
  public void twoVehiclesGoToSame() {
    // this is allowed
    rm.moveTo(dv1, dp1, time(10));
    rm.moveTo(dv2, dp1, time(10));
    assertEquals(rm.getPosition(dv1), rm.getPosition(dp1));
    assertEquals(rm.getPosition(dv2), rm.getPosition(dp1));
    pm.pickup(dv2, dp1, time(1));
    assertFalse(rm.containsObject(dp1));
    assertEquals(ParcelState.IN_CARGO, pm.getParcelState(dp1));
    rm.moveTo(dv1, dp2, time(3));
  }

  @Test
  public void moveToDepot() {
    rm.moveTo(dv1, depot, time(1));
    assertNull(rm.getDestinationToParcel(dv1));
    rm.moveTo(dv1, depot, time(1));
    rm.moveTo(dv1, dp1, time(1));
    assertEquals(dp1, rm.getDestinationToParcel(dv1));

  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidRoadUser() {
    // does not exist
    rm.moveTo(dv1, new PlainTestParcel(new Point(6, 6)), time(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidRoadUser2() {
    rm.addObjectAtSamePosition(create(new Point(1, 1), new Point(2, 2)), dp2);
    rm.addObjectAtSamePosition(new PlainTestParcel(new Point(6, 6)), dp2);
  }

  @Test
  public void test1() {
    dp1 = new DefaultParcel(
        ParcelDTO.builder(new Point(1, 0), new Point(0, 7))
            .pickupTimeWindow(DEFAULT_TW)
            .deliveryTimeWindow(DEFAULT_TW)
            .neededCapacity(0)
            .orderAnnounceTime(0L)
            .pickupDuration(2L)
            .deliveryDuration(0L)
            .build()
        );

    rm.addObjectAt(dp1, dp1.dto.pickupLocation);
    pm.register(dp1);

    assertNull(rm.getDestinationToParcel(dv1));
    rm.moveTo(dv1, dp1, time(7));
    assertEquals(dp1, rm.getDestinationToParcel(dv1));

    rm.moveTo(dv1, dp1, time(4));
    assertEquals(dp1, rm.getDestinationToParcel(dv1));

    pm.pickup(dv1, dp1, time(1));

    assertNull(rm.getDestinationToParcel(dv1));
    assertFalse(rm.containsObject(dp1));
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(dp1));
    dv1.tick(time(1));
    assertEquals(ParcelState.IN_CARGO, pm.getParcelState(dp1));

    rm.moveTo(dv1, dp1, time(1));
    assertEquals(dp1, rm.getDestinationToParcel(dv1));

    rm.moveTo(dv1, dp1, time(80));
    assertEquals(dp1, rm.getDestinationToParcel(dv1));
    pm.deliver(dv1, dp1, time(1));
    assertNull(rm.getDestinationToParcel(dv1));

    assertEquals(ParcelState.AVAILABLE, pm.getParcelState(dp2));
    rm.moveTo(dv1, dp2, time(50));
    assertEquals(dp2, rm.getDestinationToParcel(dv1));
  }

  /**
   * Check whether diversion within a route is correctly handled depending on
   * the configuration.
   */
  @Test
  public void divertFromDest() {
    boolean success = true;
    try {
      rm.moveTo(dv1, dp1, time(7));
      rm.moveTo(dv1, dp2, time(4));
    } catch (final IllegalArgumentException e) {
      success = false;
    }
    assertEquals(allowDiversion, success);
  }

  /**
   * Check whether diversion (to the same parcel!) after route completion but
   * before pickup is correctly handled depending on the configuration.
   */
  @Test
  public void divertToSameBeforePickup() {
    boolean success = true;
    try {
      rm.moveTo(dv1, dp1, time(7));
      rm.moveTo(dv1, dp1, time(4));
      assertEquals(rm.getPosition(dv1), rm.getPosition(dp1));
      rm.moveTo(dv1, dp1, time(80));
    } catch (final IllegalArgumentException e) {
      success = false;
    }
    assertEquals(allowDiversion, success);
  }

  /**
   * Check whether diversion after route completion but before pickup is
   * correctly handled depending on the configuration.
   */
  @Test
  public void divertToOtherBeforePickup() {
    boolean success = true;
    try {
      rm.moveTo(dv1, dp1, time(7));
      rm.moveTo(dv1, dp1, time(4));
      assertEquals(rm.getPosition(dv1), rm.getPosition(dp1));
      rm.moveTo(dv1, dp2, time(80));
    } catch (final IllegalArgumentException e) {
      success = false;
    }
    assertEquals(allowDiversion, success);
  }

  /**
   * Check whether diversion within a route before delivery is correctly handled
   * depending on the configuration.
   */
  @Test
  public void divertToOtherBeforeDelivery() {
    boolean success = true;
    try {
      rm.moveTo(dv1, dp1, time(10));
      assertEquals(rm.getPosition(dv1), rm.getPosition(dp1));
      pm.pickup(dv1, dp1, time(10));
      assertTrue(pm.containerContains(dv1, dp1));
      rm.moveTo(dv1, dp1, time(1));
      rm.moveTo(dv1, dp2, time(80));
    } catch (final IllegalArgumentException e) {
      success = false;
    }
    assertEquals(allowDiversion, success);
  }

  /**
   * It is not allowed to go to a delivery location twice.
   */
  @Test(expected = IllegalArgumentException.class)
  public void attemptVisitFor3rdTime() {
    // first pickup
    rm.moveTo(dv1, dp1, time(11));
    rm.removeObject(dp1);
    // then deliver
    rm.moveTo(dv1, dp1, time(80));
    // 3rd time is not allowed!
    rm.moveTo(dv1, dp1, time(80));
  }

  /**
   * It is not allowed to go to a delivery location twice.
   */
  @Test(expected = IllegalArgumentException.class)
  public void attemptVisitFor3rdTime2() {
    // first pickup
    rm.moveTo(dv1, dp1, time(11));
    assertTrue(rm.equalPosition(dv1, dp1));
    rm.removeObject(dp1);
    // then deliver
    rm.moveTo(dv1, dp1, time(80));
    assertEquals(rm.getPosition(dv1), dp1.getDestination());

    rm.moveTo(dv1, dp2, time(180));
    assertTrue(rm.equalPosition(dv1, dp2));
    rm.removeObject(dp2);

    // it is not allowed to revisit this position
    rm.moveTo(dv1, dp1, time(80));
  }

  /**
   * {@link RoadModel#moveTo(com.github.rinde.rinsim.core.model.road.MovingRoadUser, Point, TimeLapse)}
   * is only supported when diversion is allowed.
   */
  @Test
  public void invalidMoveTo() {
    boolean success = true;
    try {
      rm.moveTo(dv1, new Point(0, 0), time(10));
    } catch (final UnsupportedOperationException e) {
      success = false;
    }
    assertEquals(allowDiversion, success);
  }

  /**
   * {@link RoadModel#followPath(com.github.rinde.rinsim.core.model.road.MovingRoadUser, java.util.Queue, TimeLapse)}
   * is only supported when diversion is allowed.
   */
  @Test
  public void invalidFollowPath() {
    boolean success = true;
    try {
      rm.followPath(
          dv1,
          newLinkedList(rm
              .getShortestPathTo(new Point(0, 0), new Point(10, 10))),
          time(1));
    } catch (final UnsupportedOperationException e) {
      success = false;
    }
    assertEquals(allowDiversion, success);
  }

  static class TestVehicle extends DefaultVehicle {
    public TestVehicle(Point start) {
      super(VehicleDTO.builder()
          .startPosition(start)
          .speed(10d)
          .capacity(0)
          .availabilityTimeWindow(DEFAULT_TW)
          .build());
    }

    @Override
    protected void tickImpl(TimeLapse time) {}

    @Override
    public String toString() {
      return "TestVehicle";
    }
  }

  static class PlainTestParcel extends Parcel {
    PlainTestParcel(Point pDestination) {
      super(pDestination, 0, DEFAULT_TW, 0, DEFAULT_TW, 0);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
  }

  static TimeLapse time(long t) {
    return TimeLapseFactory.create(NonSI.HOUR, 0, t);
  }

  static DefaultParcel create(Point p1, Point p2) {
    return new DefaultParcel(
        ParcelDTO.builder(p1, p2)
            .pickupTimeWindow(DEFAULT_TW)
            .deliveryTimeWindow(DEFAULT_TW)
            .neededCapacity(0)
            .orderAnnounceTime(0L)
            .pickupDuration(0L)
            .deliveryDuration(0L)
            .build());
  }
}
