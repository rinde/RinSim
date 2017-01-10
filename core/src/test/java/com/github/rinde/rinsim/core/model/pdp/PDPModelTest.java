/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model.pdp;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nullable;
import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel.PickupAction;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.VehicleState;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Point;

/**
 * @author Rinde van Lon
 *
 */
@RunWith(Parameterized.class)
public class PDPModelTest {

  static final double EPSILON = 0.0000001;

  PDPModel model;
  RoadModel rm;

  ModelBuilder<PDPModel, PDPObject> modelSupplier;

  public PDPModelTest(ModelBuilder<PDPModel, PDPObject> builder) {
    modelSupplier = builder;
  }

  @Parameters
  public static Collection<Object[]> configs() {
    return Arrays.asList(new Object[][] {
      {DefaultPDPModel.builder()},
      {forwardingBuilder().setPDPModel(DefaultPDPModel.builder())},
      {forwardingBuilder().setPDPModel(
        forwardingBuilder().setPDPModel(DefaultPDPModel.builder()))}
    });
  }

  static ForwardingPDPModelBuilder forwardingBuilder() {
    return new ForwardingPDPModelBuilder();
  }

  static class ForwardingPDPModelBuilder extends
      AbstractModelBuilder<PDPModel, PDPObject> {

    ModelBuilder<? extends PDPModel, PDPObject> delegateBuilder;

    ForwardingPDPModelBuilder() {
      setProvidingTypes(PDPModel.class);
    }

    public ForwardingPDPModelBuilder setPDPModel(
        ModelBuilder<? extends PDPModel, PDPObject> mb) {
      setDependencies(mb.getDependencies());
      delegateBuilder = mb;
      return this;
    }

    @Override
    public ForwardingPDPModel build(DependencyProvider dependencyProvider) {
      final PDPModel delegate = delegateBuilder
        .build(dependencyProvider);
      return new ForwardingPDPModel(delegate);
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return this == other;
    }
  }

  @Before
  public void setUp() {
    final DependencyProvider dp = mock(DependencyProvider.class);

    rm = RoadModelBuilders.plane()
      .withDistanceUnit(SI.METER)
      .withMaxSpeed(Double.POSITIVE_INFINITY)
      .withSpeedUnit(SI.METERS_PER_SECOND)
      .build(dp);

    when(dp.get(RoadModel.class)).thenReturn(rm);
    model = modelSupplier.build(dp);

    // checks whether the events contain the decorated instance
    final PDPModel modelRef = model;
    model.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event event) {
        assertSame(event.toString(), modelRef, event.getIssuer());
      }
    }, PDPModelEventType.values());

    /*
     * Added to remove noise in coverage tool.
     */
    PDPType.values();
    PDPType.valueOf("PARCEL");
    ParcelState.values();
    ParcelState.valueOf("AVAILABLE");
    VehicleState.values();
    VehicleState.valueOf("IDLE");
    PDPModelEventType.values();
    PDPModelEventType.valueOf("START_PICKUP");

    assertTrue(model.getSupportedType().equals(PDPObject.class));
  }

  @Test
  public void testDrop() {
    final Vehicle truck = new TestVehicle(VehicleDTO.builder()
      .startPosition(new Point(1, 1))
      .capacity(10)
      .speed(1.0)
      .build());
    model.register(truck);
    rm.register(truck);

    final Parcel pack = Parcel.builder(new Point(1, 2), new Point(2, 2))
      .pickupDuration(100L)
      .deliveryDuration(100L)
      .neededCapacity(2d)
      .build();

    model.register(pack);
    rm.register(pack);

    rm.followPath(truck, newLinkedList(asList(new Point(1, 2))),
      TimeLapseFactory.create(0, 3600000));
    model.pickup(truck, pack, TimeLapseFactory.create(0, 40));
    model.continuePreviousActions(truck, TimeLapseFactory.create(0, 40));

    final TimeLapse tl = TimeLapseFactory.create(0, 40);
    model.continuePreviousActions(truck, tl);
    assertFalse(model.getContents(truck).isEmpty());
    assertEquals(ParcelState.IN_CARGO, model.getParcelState(pack));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));

    // riding to other spot to drop
    rm.followPath(truck, newLinkedList(asList(new Point(2, 2))),
      TimeLapseFactory.create(0, 3600000));
    model.drop(truck, pack, TimeLapseFactory.create(0, 50));
    // not enough time for dropping
    assertFalse(model.getContents(truck).isEmpty());
    assertEquals(VehicleState.DELIVERING, model.getVehicleState(truck));
    assertEquals(ParcelState.DELIVERING, model.getParcelState(pack));
    model.continuePreviousActions(truck, TimeLapseFactory.create(0, 50));
    assertEquals(0, model.getContentsSize(truck), EPSILON);
    assertTrue(model.getContents(truck).isEmpty());
    assertEquals(ParcelState.AVAILABLE, model.getParcelState(pack));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));

  }

  @Test
  public void testPickupAfterDrop() {
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder()
        .startPosition(new Point(1, 1))
        .capacity(10)
        .speed(1.0)
        .build());

    model.register(truck);
    rm.register(truck);

    final Parcel pack2 = Parcel.builder(new Point(1, 2), new Point(2, 2))
      .pickupDuration(100L)
      .deliveryDuration(100L)
      .neededCapacity(2d)
      .build();

    model.register(pack2);
    rm.register(pack2);

    rm.followPath(truck, newLinkedList(asList(new Point(1, 2))),
      TimeLapseFactory.create(0, 3600000));
    model.pickup(truck, pack2, TimeLapseFactory.create(0, 40));
    model.continuePreviousActions(truck, TimeLapseFactory.create(0, 40));

    final TimeLapse tl = TimeLapseFactory.create(0, 40);
    model.continuePreviousActions(truck, tl);
    assertFalse(model.getContents(truck).isEmpty());
    assertEquals(ParcelState.IN_CARGO, model.getParcelState(pack2));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));

    // riding to other spot to drop
    rm.followPath(truck, newLinkedList(asList(new Point(2, 2))),
      TimeLapseFactory.create(0, 3600000));

    model.drop(truck, pack2, TimeLapseFactory.create(0, 100));

    final Vehicle truck2 = new TestVehicle(
      VehicleDTO.builder()
        .startPosition(new Point(2, 2))
        .capacity(10)
        .speed(1.0)
        .build());
    model.register(truck2);
    rm.register(truck2);

    model.pickup(truck2, pack2, TimeLapseFactory.create(0, 40));
    model.continuePreviousActions(truck2, TimeLapseFactory.create(0, 40));

    final TimeLapse tl2 = TimeLapseFactory.create(0, 40);
    model.continuePreviousActions(truck2, tl2);
    assertFalse(model.getContents(truck2).isEmpty());
    assertEquals(ParcelState.IN_CARGO, model.getParcelState(pack2));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));
  }

  @Test
  public void testPickup() {
    final Parcel pack1 = Parcel.builder(new Point(1, 1), new Point(2, 2))
      .neededCapacity(2d)
      .build();

    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder()
        .startPosition(new Point(1, 1))
        .capacity(10)
        .speed(1.0)
        .build());
    model.register(pack1);
    model.register(truck);
    rm.register(pack1);
    rm.register(truck);

    // nothing should happen
    truck.tick(TimeLapseFactory.create(0, 10000));

    assertEquals(0, model.getContentsSize(truck), EPSILON);
    assertTrue(model.getContents(truck).isEmpty());
    assertEquals(ParcelState.AVAILABLE, model.getParcelState(pack1));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));
    model.pickup(truck, pack1, TimeLapseFactory.create(0, 10000));
    assertEquals(ParcelState.IN_CARGO, model.getParcelState(pack1));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));

    assertFalse(rm.containsObject(pack1));
    assertTrue(model.containerContains(truck, pack1));
    assertEquals(2, model.getContentsSize(truck), EPSILON);
    assertTrue(model.getContents(truck).contains(pack1));
    assertEquals(1, model.getContents(truck).size());

    final Parcel pack2 = Parcel.builder(new Point(1, 2), new Point(2, 2))
      .serviceDuration(100L)
      .neededCapacity(2d)
      .build();
    model.register(pack2);
    rm.register(pack2);

    rm.followPath(truck, newLinkedList(asList(new Point(1, 2))),
      TimeLapseFactory.create(0, 3600000));

    assertEquals(new Point(1, 2), rm.getPosition(truck));
    assertEquals(new Point(1, 2), rm.getPosition(pack2));
    assertEquals(ParcelState.AVAILABLE, model.getParcelState(pack2));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));
    model.pickup(truck, pack2, TimeLapseFactory.create(0, 40));
    assertFalse(rm.containsObject(pack2));
    final PickupAction action = (PickupAction) model
      .getVehicleActionInfo(truck);
    assertFalse(action.isDone());
    assertEquals(60, action.timeNeeded());
    assertEquals(ParcelState.PICKING_UP, model.getParcelState(pack2));
    assertEquals(VehicleState.PICKING_UP, model.getVehicleState(truck));

    model.continuePreviousActions(truck, TimeLapseFactory.create(0, 40));
    assertFalse(action.isDone());
    assertEquals(20, action.timeNeeded());
    assertEquals(ParcelState.PICKING_UP, model.getParcelState(pack2));
    assertEquals(VehicleState.PICKING_UP, model.getVehicleState(truck));

    final TimeLapse tl = TimeLapseFactory.create(0, 40);
    model.continuePreviousActions(truck, tl);
    assertTrue(action.isDone());
    assertEquals(0, action.timeNeeded());
    assertEquals(20, tl.getTimeLeft());
    assertEquals(ParcelState.IN_CARGO, model.getParcelState(pack2));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));

  }

  @Test
  public void testDelayedPickup() {

    final Parcel pack1 = Parcel.builder(new Point(1, 1), new Point(2, 2))
      .serviceDuration(10L)
      .neededCapacity(2d)
      .build();
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder()
        .startPosition(new Point(1, 1))
        .capacity(10)
        .speed(1.0)
        .build());
    model.register(pack1);
    model.register(truck);
    rm.register(pack1);
    rm.register(truck);

    model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
    assertTrue(model.getContents(truck).isEmpty());
    assertEquals(model.getContentsSize(truck), 0, EPSILON);

    truck.tick(TimeLapseFactory.create(1, 10));
    assertFalse(model.getContents(truck).isEmpty());
    assertTrue(model.getContents(truck).contains(pack1));
    assertEquals(model.getContentsSize(truck), 2, EPSILON);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPickupFail1() {
    // truck not in roadmodel
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder().build());
    model.pickup(truck, null, TimeLapseFactory.create(0, 1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPickupFail2() {
    // package not in roadmodel
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder().startPosition(new Point(1, 1)).build());
    rm.register(truck);
    final Parcel pack1 = Parcel.builder(new Point(0, 0), new Point(2, 2))
      .neededCapacity(2d)
      .build();
    model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPickupFail3A() {
    // package not in available state (it is already been picked up)
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder().startPosition(new Point(1, 1)).build());
    rm.register(truck);
    model.register(truck);
    final Parcel pack1 = Parcel.builder(new Point(0, 0), new Point(2, 2))
      .pickupDuration(10)
      .deliveryDuration(10)
      .neededCapacity(2d)
      .build();
    rm.register(pack1);
    model.register(pack1);
    rm.addObjectAtSamePosition(pack1, truck);
    assertTrue(rm.equalPosition(truck, pack1));
    assertEquals(ParcelState.AVAILABLE, model.getParcelState(pack1));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));

    model.pickup(truck, pack1, TimeLapseFactory.create(0, 100));
    assertEquals(ParcelState.IN_CARGO, model.getParcelState(pack1));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));

    // checks what happens when you add a package to the roadmodel which has
    // already been picked up
    rm.addObjectAt(pack1, new Point(1, 1));
    model.pickup(truck, pack1, TimeLapseFactory.create(0, 100));

  }

  @Test(expected = IllegalArgumentException.class)
  public void testPickupFail3B() {
    // package is not registered
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder().startPosition(new Point(1, 1)).build());
    rm.register(truck);
    final Parcel pack1 = Parcel.builder(new Point(0, 0), new Point(2, 2))
      .neededCapacity(2d)
      .build();
    rm.register(pack1);
    rm.addObjectAtSamePosition(pack1, truck);
    model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPickupFail4() {
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder().startPosition(new Point(1, 1)).build());
    rm.register(truck);
    final Parcel pack1 = Parcel.builder(new Point(0, 0), new Point(2, 2))
      .neededCapacity(2d)
      .build();
    rm.register(pack1);
    rm.addObjectAtSamePosition(pack1, truck);
    model.register(pack1);
    model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPickupFail5() {
    // wrong position
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder().startPosition(new Point(1, 1)).build());
    rm.register(truck);
    model.register(truck);
    final Parcel pack1 = Parcel.builder(new Point(0, 0), new Point(2, 2))
      .neededCapacity(2d)
      .build();
    rm.register(pack1);
    rm.addObjectAt(pack1, new Point(0, 0));
    model.register(pack1);
    model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPickupFail6A() {
    // package does not fit in truck
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder().startPosition(new Point(1, 1)).build());
    rm.register(truck);
    model.register(truck);
    final Parcel pack1 = Parcel.builder(new Point(0, 0), new Point(2, 2))
      .neededCapacity(2d)
      .build();
    rm.register(pack1);
    model.register(pack1);
    rm.addObjectAtSamePosition(pack1, truck);
    assertTrue(rm.equalPosition(truck, pack1));
    model.pickup(truck, pack1, TimeLapseFactory.create(0, 100));
  }

  @Test
  public void testPickupFail6B() {
    // last package does not fit in truck
    final Vehicle truck = new TestVehicle(VehicleDTO.builder()
      .startPosition(new Point(1, 1))
      .capacity(10)
      .build());
    rm.register(truck);
    model.register(truck);

    for (int i = 0; i < 9; i++) {
      final Parcel newPack = Parcel
        .builder(new Point(1, 1), new Point(2, 2))
        .neededCapacity(1d)
        .build();
      rm.register(newPack);
      model.register(newPack);
      model.pickup(truck, newPack, TimeLapseFactory.create(0, 1));
    }
    assertEquals(model.getContents(truck).size(), 9);
    assertEquals(model.getContentsSize(truck), 9.0, EPSILON);

    final Parcel packTooMuch = Parcel
      .builder(new Point(1, 1), new Point(2, 2))
      .neededCapacity(1.1)
      .build();
    rm.register(packTooMuch);
    model.register(packTooMuch);
    boolean fail = false;
    try {
      model.pickup(truck, packTooMuch, TimeLapseFactory.create(0, 1));
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  @Test
  public void testDeliver() {
    final Vehicle truck = new TestVehicle(VehicleDTO.builder()
      .startPosition(new Point(1, 1))
      .capacity(10)
      .build());
    rm.register(truck);
    model.register(truck);
    final Parcel pack1 = Parcel.builder(new Point(1, 1), new Point(2, 2))
      .pickupDuration(10)
      .deliveryDuration(10)
      .neededCapacity(2d)
      .build();
    rm.register(pack1);
    model.register(pack1);
    assertTrue(rm.equalPosition(truck, pack1));
    assertEquals(ParcelState.AVAILABLE, model.getParcelState(pack1));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));
    assertEquals(newHashSet(pack1), model.getParcels(ParcelState.AVAILABLE));

    model.pickup(truck, pack1, TimeLapseFactory.create(0, 100));
    assertEquals(ParcelState.IN_CARGO, model.getParcelState(pack1));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));
    assertTrue(model.getParcels(ParcelState.AVAILABLE).isEmpty());

    rm.moveTo(truck, pack1.getDeliveryLocation(),
      TimeLapseFactory.create(0, 3600000 * 3));
    assertEquals(pack1.getDeliveryLocation(), rm.getPosition(truck));

    model.deliver(truck, pack1, TimeLapseFactory.create(0, 8));
    assertEquals(ParcelState.DELIVERING, model.getParcelState(pack1));
    assertEquals(VehicleState.DELIVERING, model.getVehicleState(truck));

    final TimeLapse tl = TimeLapseFactory.create(0, 10);
    model.continuePreviousActions(truck, tl);
    assertEquals(ParcelState.DELIVERED, model.getParcelState(pack1));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));
    assertEquals(8, tl.getTimeLeft());

    final Parcel pack2 = Parcel.builder(new Point(2, 2), new Point(2, 2))
      .pickupDuration(10)
      .deliveryDuration(10)
      .neededCapacity(2d)
      .build();
    rm.register(pack2);
    model.register(pack2);
    assertEquals(ParcelState.AVAILABLE, model.getParcelState(pack2));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));
    assertEquals(newHashSet(pack2), model.getParcels(ParcelState.AVAILABLE));

    model.pickup(truck, pack2, TimeLapseFactory.create(0, 10));
    assertEquals(ParcelState.IN_CARGO, model.getParcelState(pack2));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));
    assertTrue(model.getParcels(ParcelState.AVAILABLE).isEmpty());

    model.deliver(truck, pack2, TimeLapseFactory.create(0, 10));
    assertEquals(ParcelState.DELIVERED, model.getParcelState(pack2));
    assertEquals(VehicleState.IDLE, model.getVehicleState(truck));

  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeliverFail1() {
    // truck does not exist in roadmodel
    model.deliver(new TestVehicle(), null, TimeLapseFactory.create(0, 1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeliverFail2() {
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder().startPosition(new Point(1, 1)).build());
    rm.register(truck);
    model.register(truck);
    final Parcel pack1 = Parcel.builder(new Point(1, 1), new Point(2, 2))
      .pickupDuration(10)
      .deliveryDuration(10)
      .neededCapacity(2d)
      .build();
    rm.register(pack1);
    model.register(pack1);
    rm.addObjectAtSamePosition(pack1, truck);

    model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
    assertEquals(VehicleState.PICKING_UP, model.getVehicleState(truck));
    model.deliver(truck, pack1, TimeLapseFactory.create(0, 1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeliverFail3() {
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder().startPosition(new Point(1, 1)).build());
    rm.register(truck);
    model.register(truck);
    final Parcel pack1 = Parcel.builder(new Point(1, 1), new Point(2, 2))
      .pickupDuration(10)
      .deliveryDuration(10)
      .neededCapacity(2d)
      .build();
    rm.register(pack1);
    model.register(pack1);
    rm.addObjectAtSamePosition(pack1, truck);

    model.deliver(truck, pack1, TimeLapseFactory.create(0, 1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDeliverFail4() {
    final Vehicle truck = new TestVehicle(
      VehicleDTO.builder().startPosition(new Point(1, 1)).build());
    rm.register(truck);
    model.register(truck);
    final Parcel pack1 = Parcel.builder(new Point(1, 1), new Point(2, 2))
      .pickupDuration(10)
      .deliveryDuration(10)
      .neededCapacity(2d)
      .build();
    rm.register(pack1);
    model.register(pack1);
    rm.addObjectAtSamePosition(pack1, truck);

    model.pickup(truck, pack1, TimeLapseFactory.create(0, 10));
    assertTrue(model.getContents(truck).contains(pack1));
    model.deliver(truck, pack1, TimeLapseFactory.create(0, 1));
  }

  @Test
  public void addPackageIn() {
    assertTrue(model.getParcels(ParcelState.AVAILABLE).isEmpty());
    final Depot d = new TestDepot(10);
    final Parcel p1 = Parcel.builder(new Point(0, 0), new Point(0, 0))
      .neededCapacity(1d)
      .build();
    model.register(d);
    model.register(p1);
    rm.addObjectAt(d, new Point(0, 0));
    model.addParcelIn(d, p1);

    assertEquals(1, model.getContents(d).size());
    assertTrue(model.getContents(d).contains(p1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void addPackageInFail1() {
    final Depot d = new TestDepot(10);
    final Parcel p1 = Parcel.builder(new Point(0, 0), new Point(0, 0))
      .neededCapacity(1d)
      .build();
    rm.addObjectAt(p1, new Point(0, 0));
    model.addParcelIn(d, p1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addPackageInFail2() {
    final Depot d = new TestDepot(10);
    final Parcel p1 = Parcel.builder(new Point(0, 0), new Point(0, 0))
      .neededCapacity(1d)
      .build();
    model.addParcelIn(d, p1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addPackageInFail3() {
    final Depot d = new TestDepot(10);
    final Parcel p1 = Parcel.builder(new Point(0, 0), new Point(0, 0))
      .neededCapacity(1d)
      .build();
    model.register(p1);
    model.addParcelIn(d, p1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addPackageInFail4() {
    final Depot d = new TestDepot(10);
    final Parcel p1 = Parcel.builder(new Point(0, 0), new Point(0, 0))
      .neededCapacity(1d)
      .build();
    model.register(p1);
    model.register(d);
    model.addParcelIn(d, p1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addPackageInFail5() {
    final Depot d = new TestDepot(10);
    final Parcel p1 = Parcel.builder(new Point(0, 0), new Point(0, 0))
      .neededCapacity(11d)
      .build();
    model.register(p1);
    model.register(d);
    rm.addObjectAt(d, new Point(0, 0));
    model.addParcelIn(d, p1);
  }

  /**
   * Tests that the register call back injects the possibly decorated instance.
   */
  @Test
  public void register() {
    final Parcel p = Parcel.builder(new Point(0, 0), new Point(0, 0))
      .build();
    model.register(p);
    assertThat(model).isSameAs(p.pdpModel.get());

    final TestDepot d = new TestDepot(10);
    model.register(d);
    assertThat(model).isSameAs(d.pdpModel.get());

    final TestVehicle v = new TestVehicle();
    model.register(v);
    assertThat(model).isSameAs(v.pdpModel.get());
  }

  /**
   * Cannot register the same parcel twice.
   */
  @Test(expected = IllegalArgumentException.class)
  public void registerFail1() {
    final Parcel p = Parcel.builder(new Point(0, 0), new Point(1, 1))
      .build();
    model.register(p);
    model.register(p);
  }

  /**
   * Cannot register the same depot twice.
   */
  @Test(expected = IllegalArgumentException.class)
  public void registerFail2() {
    final Depot d = new TestDepot(10);
    model.register(d);
    model.register(d);
  }

  /**
   * Cannot register the same vehicle twice.
   */
  @Test(expected = IllegalArgumentException.class)
  public void registerFail3() {
    final TestVehicle v = new TestVehicle();
    model.register(v);
    model.register(v);
  }

  @Test
  // (expected = UnsupportedOperationException.class)
  public void unregisterFail() {
    // FIXME implement unregister test
    // model.unregister(null);
  }

  @Test(expected = IllegalStateException.class)
  public void containerSetCapacityFail() {
    final Depot d = new TestDepot(10);
    model.register(d);
    rm.register(d);
    d.setCapacity(20);
  }

  @Test(expected = IllegalStateException.class)
  public void objectSetStartPositionFail() {
    final Depot d = new TestDepot(10);
    model.register(d);
    assertEquals(model, d.getPDPModel());
    rm.register(d);
    assertEquals(rm, d.getRoadModel());
    d.setStartPosition(new Point(0, 0));
  }

  @Test(expected = IllegalStateException.class)
  public void objectCheckDoubleRegistrationFail1() {
    final Depot d = new TestDepot(10);
    d.initPDPObject(model);
    d.initPDPObject(model);
  }

  @Test(expected = IllegalStateException.class)
  public void objectCheckDoubleRegistrationFail2() {
    final Depot d = new TestDepot(10);
    d.initRoadUser(rm);
    d.initRoadUser(rm);
  }

  static class TestVehicle extends Vehicle {

    TestVehicle() {
      super(VehicleDTO.builder().build());
    }

    TestVehicle(VehicleDTO d) {
      super(d);
    }

    @Override
    protected void tickImpl(TimeLapse time) {}

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
  }

  static class TestDepot extends Depot {
    TestDepot(int pCapacity) {
      super(new Point(0, 0));
      setCapacity(pCapacity);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

  }

}
