/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.central;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import javax.measure.unit.SI;

import org.junit.Test;

import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.TravelTimes;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon
 *
 */
public class SolverValidatorTest {
  static final Point POINT = new Point(0, 0);
  static final Parcel p1 = parcel("p1");
  static final Parcel p2 = parcel("p2");
  static final Parcel p3 = parcel("p3");
  static final Parcel p4 = parcel("p4");
  static final Parcel p5 = parcel("p5");

  static final Optional<Connection<?>> absent = Optional.absent();

  @Test
  public void validateNegativeTime() {
    final GlobalStateObject state = GlobalStateObject.create(
      ImmutableSet.<Parcel>of(), ImmutableList.<VehicleStateObject>of(), -1,
      SI.SECOND, SI.METERS_PER_SECOND, SI.METER, mock(TravelTimes.class));
    boolean fail = false;
    try {
      SolverValidator.validateInputs(state);
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage()).contains("Time must be >= 0");
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void validateNegativeRemainingTime() {
    final VehicleStateObject vs1 =
      VehicleStateObject.create(vdto(), POINT, absent,
        ImmutableSet.of(p1), -1, p2, ImmutableList.<Parcel>of());
    final GlobalStateObject state = GlobalStateObject.create(
      ImmutableSet.<Parcel>of(),
      ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER,
      mock(TravelTimes.class));
    boolean fail = false;
    try {
      SolverValidator.validateInputs(state);
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage())
        .contains("Remaining service time must be >= 0");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void validateParcelAvailableAndInInventory() {
    final VehicleStateObject vs1 =
      VehicleStateObject.create(vdto(), POINT, absent,
        ImmutableSet.of(p1), 0, p2, ImmutableList.<Parcel>of());
    final GlobalStateObject state = GlobalStateObject.create(
      ImmutableSet.of(p1), ImmutableList.of(vs1), 0, SI.SECOND,
      SI.METERS_PER_SECOND, SI.METER, mock(TravelTimes.class));

    boolean fail = false;
    try {
      SolverValidator.validateInputs(state);
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage()).contains(
        "Parcels can not be available AND in the inventory of a vehicle");
    }
    assertThat(fail).isTrue();
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateParcelInTwoInventories() {
    final VehicleStateObject vs1 =
      VehicleStateObject.create(vdto(), POINT, absent,
        ImmutableSet.of(p1), 0, p2, ImmutableList.<Parcel>of());
    final VehicleStateObject vs2 =
      VehicleStateObject.create(vdto(), POINT, absent,
        ImmutableSet.of(p1), 0, p2, ImmutableList.<Parcel>of());
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final GlobalStateObject state = GlobalStateObject.create(empty,
      ImmutableList.of(vs1, vs2), 0, SI.SECOND, SI.METERS_PER_SECOND,
      SI.METER, mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  @Test(expected = IllegalArgumentException.class)
  public void valiateInputsDestinationNotInContents() {
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 =
      VehicleStateObject.create(vdto(), POINT, absent,
        empty, 0, p1, ImmutableList.<Parcel>of());
    final VehicleStateObject vs2 =
      VehicleStateObject.create(vdto(), POINT, absent,
        ImmutableSet.of(p2, p1), 0, p1, ImmutableList.<Parcel>of());

    final GlobalStateObject state = GlobalStateObject.create(empty,
      ImmutableList.of(vs1, vs2), 0, SI.SECOND, SI.METERS_PER_SECOND,
      SI.METER, mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  /**
   * One route is present, one is not.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute1() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(), p, absent,
      ImmutableSet.of(p1), 0, p1, ImmutableList.of(p1));
    final VehicleStateObject vs2 = VehicleStateObject.create(vdto(), p, absent,
      ImmutableSet.of(p2), 0, null, null);
    final ImmutableSet<Parcel> available = ImmutableSet.of(p3);
    final GlobalStateObject state = GlobalStateObject.create(available,
      ImmutableList.of(vs1, vs2), 0, SI.SECOND, SI.METERS_PER_SECOND,
      SI.METER, mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  /**
   * Parcel 2 occurs in two different routes.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute2() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(), p, absent,
      ImmutableSet.<Parcel>of(), 0, p1, ImmutableList.of(p1, p2, p1, p2));
    final VehicleStateObject vs2 = VehicleStateObject.create(vdto(), p, absent,
      ImmutableSet.<Parcel>of(), 0, null, ImmutableList.of(p2, p2));
    final ImmutableSet<Parcel> available = ImmutableSet.of(p1, p2);
    final GlobalStateObject state = GlobalStateObject.create(available,
      ImmutableList.of(vs1, vs2), 0, SI.SECOND, SI.METERS_PER_SECOND,
      SI.METER, mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  /**
   * Vehicle doesn't have its cargo in its route.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute3() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(), p, absent,
      ImmutableSet.of(p1), 0, p1, ImmutableList.of(p3));
    final ImmutableSet<Parcel> available = ImmutableSet.of(p3);
    final GlobalStateObject state = GlobalStateObject.create(available,
      ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER,
      mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  /**
   * The first location in a route must match the destination field if not null.
   * In this test it is another parcel.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute4a() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(), p, absent,
      ImmutableSet.of(p1), 0, p1, ImmutableList.of(p3, p1, p3));
    final ImmutableSet<Parcel> available = ImmutableSet.of(p3);
    final GlobalStateObject state = GlobalStateObject.create(available,
      ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER,
      mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  /**
   * The first location in a route must match the destination field if not null.
   * In this test the route is empty.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute4b() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(), p, absent,
      ImmutableSet.<Parcel>of(), 0, p1, ImmutableList.<Parcel>of());
    final ImmutableSet<Parcel> available = ImmutableSet.of(p1);
    final GlobalStateObject state = GlobalStateObject.create(available,
      ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER,
      mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  /**
   * Duplicate in route.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute5() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(), p, absent,
      ImmutableSet.of(p1), 0, p1, ImmutableList.of(p1, p1, p3));
    final ImmutableSet<Parcel> available = ImmutableSet.of(p3);
    final GlobalStateObject state = GlobalStateObject.create(available,
      ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER,
      mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  /**
   * Only once occurence of available parcel, should occur twice.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute6a() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(), p, absent,
      ImmutableSet.of(p1), 0, p1, ImmutableList.of(p1, p2));
    final ImmutableSet<Parcel> available = ImmutableSet.of(p2);
    final GlobalStateObject state = GlobalStateObject.create(available,
      ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER,
      mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  /**
   * Too many occurences of available parcel, should occur twice.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute6b() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(), p, absent,
      ImmutableSet.of(p1), 0, p1, ImmutableList.of(p1, p2, p2, p2));
    final ImmutableSet<Parcel> available = ImmutableSet.of(p2);
    final GlobalStateObject state = GlobalStateObject.create(available,
      ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER,
      mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  /**
   * Valid routes.
   */
  @Test
  public void validateValidCurrentRoutes() {
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 =
      VehicleStateObject.create(vdto(), POINT, absent,
        ImmutableSet.of(p1), 0, p1, ImmutableList.of(p1));
    final VehicleStateObject vs2 =
      VehicleStateObject.create(vdto(), POINT, absent,
        ImmutableSet.of(p2), 0, null, ImmutableList.of(p2));
    final VehicleStateObject vs3 =
      VehicleStateObject.create(vdto(), POINT, absent,
        empty, 0, p3, ImmutableList.<Parcel>of(p3, p3));
    final ImmutableSet<Parcel> available = ImmutableSet.of(p3);
    final GlobalStateObject state = GlobalStateObject.create(available,
      ImmutableList.of(vs1, vs2, vs3), 0, SI.SECOND, SI.METERS_PER_SECOND,
      SI.CENTIMETER, mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  @SuppressWarnings("null")
  @Test
  public void validateCorrectInputs() {
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 =
      VehicleStateObject.create(vdto(), POINT, absent,
        ImmutableSet.of(p1), 0, p1, null);
    final VehicleStateObject vs2 =
      VehicleStateObject.create(vdto(), POINT, absent,
        ImmutableSet.of(p2), 0, null, null);
    final VehicleStateObject vs3 =
      VehicleStateObject.create(vdto(), POINT, absent,
        empty, 0, p3, null);
    final ImmutableSet<Parcel> available = ImmutableSet.of(p3);
    final GlobalStateObject state = GlobalStateObject.create(available,
      ImmutableList.of(vs1, vs2, vs3), 0, SI.SECOND, SI.METERS_PER_SECOND,
      SI.CENTIMETER, mock(TravelTimes.class));
    SolverValidator.validateInputs(state);
  }

  @Test
  public void validateInvalidNumberOfRoutes() {
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      POINT, absent, ImmutableSet.<Parcel>of(), 0, null, null);
    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList.of();
    final GlobalStateObject state = GlobalStateObject.create(
      ImmutableSet.<Parcel>of(), ImmutableList.of(vs1), 0, SI.SECOND,
      SI.METERS_PER_SECOND, SI.CENTIMETER, mock(TravelTimes.class));
    boolean fail = false;
    try {
      SolverValidator.validateOutputs(routes, state);
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("found 0 routes with 1 vehicles");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void validateParcelInTwoRoutes() {
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, null, null);
    final VehicleStateObject vs2 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, null, null);

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList.of(
      ImmutableList.of(p1, p1), ImmutableList.of(p1, p1));
    final ImmutableSet<Parcel> availableParcels = ImmutableSet.of(p1);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1,
      vs2);
    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0, SI.SECOND, SI.METERS_PER_SECOND, SI.CENTIMETER,
      mock(TravelTimes.class));

    boolean fail = false;
    try {
      SolverValidator.validateOutputs(routes, state);
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage())
        .contains("Found a parcel which is already in another route");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void validateParcelTooManyTimes1() {
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, null, null);

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList
      .of(ImmutableList.of(p1, p1, p1));
    final ImmutableSet<Parcel> availableParcels = ImmutableSet.of(p1);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1);
    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0, SI.SECOND, SI.METERS_PER_SECOND, SI.CENTIMETER,
      mock(TravelTimes.class));
    boolean fail = false;
    try {
      SolverValidator.validateOutputs(routes, state);
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("found 3 occurence(s) of parcel p1");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void validateParcelTooManyTimes2() {
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      ImmutableSet.of(p1), 0, null, null);

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList
      .of(ImmutableList.of(p1, p1));
    final ImmutableSet<Parcel> availableParcels = ImmutableSet.of();
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1);
    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0, SI.SECOND, SI.METERS_PER_SECOND, SI.CENTIMETER,
      mock(TravelTimes.class));
    boolean fail = false;
    try {
      SolverValidator.validateOutputs(routes, state);
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("found 2 occurences of parcel p1");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void validateParcelNotInCargo() {
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, null, null);

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList
      .of(ImmutableList.of(p1));
    final ImmutableSet<Parcel> availableParcels = ImmutableSet.of();
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1);
    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0, SI.SECOND, SI.METERS_PER_SECOND, SI.CENTIMETER,
      mock(TravelTimes.class));
    boolean fail = false;
    try {
      SolverValidator.validateOutputs(routes, state);
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage())
        .contains("The parcel in this route is not available");
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void validateUnknownParcelInRoute() {
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, null, null);
    final VehicleStateObject vs2 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, null, null);

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList.of(
      ImmutableList.of(p1, p1), ImmutableList.of(p2, p3, p3, p2));
    final ImmutableSet<Parcel> availableParcels = ImmutableSet.of(p1, p2);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1,
      vs2);
    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0, SI.SECOND, SI.METERS_PER_SECOND, SI.CENTIMETER,
      mock(TravelTimes.class));
    boolean fail = false;
    try {
      SolverValidator.validateOutputs(routes, state);
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage())
        .contains("parcel in this route is not available");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void validateIncompleteRoute1() {
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, null, null);
    final VehicleStateObject vs2 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, null, null);

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList.of(
      ImmutableList.of(p1, p1), ImmutableList.of(p2, p2));
    final ImmutableSet<Parcel> availableParcels = ImmutableSet
      .of(p1, p2, p3);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1,
      vs2);
    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0, SI.SECOND, SI.METERS_PER_SECOND, SI.CENTIMETER,
      mock(TravelTimes.class));

    boolean fail = false;
    try {
      SolverValidator.validateOutputs(routes, state);
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(
        "number of distinct parcels in the routes should equal the number of parcels");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void validateIncompleteRouteForVehicle() {
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      ImmutableSet.of(p1), 0, null, null);

    final ImmutableList<Parcel> empty = ImmutableList.of();
    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList
      .of(empty);
    final ImmutableSet<Parcel> availableParcels = ImmutableSet.of();
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1);
    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0, SI.SECOND, SI.METERS_PER_SECOND, SI.CENTIMETER,
      mock(TravelTimes.class));

    boolean fail = false;
    try {
      SolverValidator.validateOutputs(routes, state);
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage())
        .contains("doesn't visit all parcels in its cargo");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void validateOutputDestinationNotFirstInRoute() {
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, p1, null);

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList
      .of(ImmutableList.of(p2, p1, p1, p2));

    final ImmutableSet<Parcel> availableParcels = ImmutableSet.of(p1, p2);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1);
    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0, SI.SECOND, SI.METERS_PER_SECOND, SI.CENTIMETER,
      mock(TravelTimes.class));

    boolean fail = false;
    try {
      SolverValidator.validateOutputs(routes, state);
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage())
        .contains("should start with its current destination");
      fail = true;
    }
    assertThat(fail).isTrue();
  }

  @SuppressWarnings("null")
  @Test
  public void validateCorrectOutput() {
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, null, null);
    final VehicleStateObject vs2 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      ImmutableSet.of(p3), 0, null, null);

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList.of(
      ImmutableList.of(p1, p1), ImmutableList.of(p2, p3, p2));
    final ImmutableSet<Parcel> availableParcels = ImmutableSet.of(p1, p2);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1,
      vs2);
    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0, SI.SECOND, SI.METERS_PER_SECOND, SI.CENTIMETER,
      mock(TravelTimes.class));
    SolverValidator.validateOutputs(routes, state);
  }

  @Test
  public void testWrap() throws InterruptedException {
    TestUtil.testPrivateConstructor(SolverValidator.class);
    final ImmutableSet<Parcel> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, null, null);
    final VehicleStateObject vs2 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      ImmutableSet.of(p3), 0, null, null);
    final VehicleStateObject vs3 = VehicleStateObject.create(vdto(),
      new Point(0,
        0),
      absent,
      empty, 0, p4, null);

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList.of(
      ImmutableList.of(p1, p1), ImmutableList.of(p2, p3, p2),
      ImmutableList.of(p4, p5, p5, p4));
    final ImmutableSet<Parcel> availableParcels = ImmutableSet.of(p1, p2,
      p4, p5);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1,
      vs2, vs3);
    @SuppressWarnings("null")
    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0, SI.SECOND, SI.METERS_PER_SECOND, SI.CENTIMETER,
      mock(TravelTimes.class));
    final Solver solver = SolverValidator.wrap(new FakeSolver(routes));
    solver.solve(state);
  }

  static Parcel parcel(final String name) {
    return new Parcel(Parcel.builder(new Point(0, 0), new Point(0, 0))
      .buildDTO()) {

      @Override
      public String toString() {
        return name;
      }
    };
  }

  static VehicleDTO vdto() {
    return VehicleDTO.builder()
      .startPosition(new Point(0, 0))
      .speed(1d)
      .capacity(1)
      .availabilityTimeWindow(TimeWindow.always())
      .build();
  }

  class FakeSolver implements Solver {
    ImmutableList<ImmutableList<Parcel>> answer;

    FakeSolver(ImmutableList<ImmutableList<Parcel>> answer) {
      this.answer = answer;
    }

    @Override
    public ImmutableList<ImmutableList<Parcel>> solve(GlobalStateObject state) {
      return answer;
    }
  }

}
