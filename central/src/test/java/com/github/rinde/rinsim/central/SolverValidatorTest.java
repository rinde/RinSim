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
package com.github.rinde.rinsim.central;

import javax.measure.unit.SI;

import org.junit.Test;

import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TestUtil;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon
 * 
 */
public class SolverValidatorTest {

  protected static final ParcelDTO p1 = parcel();
  protected static final ParcelDTO p2 = parcel();
  protected static final ParcelDTO p3 = parcel();
  protected static final ParcelDTO p4 = parcel();
  protected static final ParcelDTO p5 = parcel();

  @Test(expected = IllegalArgumentException.class)
  public void validateNegativeTime() {
    @SuppressWarnings("null")
    final GlobalStateObject state = new GlobalStateObject(null, null, -1, null,
        null, null);
    SolverValidator.validateInputs(state);
  }

  @Test(expected = IllegalArgumentException.class)
  @SuppressWarnings("null")
  public void validateNegativeRemainingTime() {
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), null, null,
        -1, null, null);
    final GlobalStateObject state = new GlobalStateObject(null,
        ImmutableList.of(vs1), 0, null, null, null);
    SolverValidator.validateInputs(state);
  }

  @Test(expected = IllegalArgumentException.class)
  @SuppressWarnings("null")
  public void validateZeroSpeed() {
    final VehicleDTO dto1 = VehicleDTO.builder().speed(0d).build();
    final VehicleStateObject vs1 = new VehicleStateObject(dto1, null, null, 0,
        null, null);
    final GlobalStateObject state = new GlobalStateObject(null,
        ImmutableList.of(vs1), 0, null, null, null);
    SolverValidator.validateInputs(state);
  }

  @SuppressWarnings("null")
  @Test(expected = IllegalArgumentException.class)
  public void validateParcelAvailableAndInInventory() {
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), null,
        ImmutableSet.of(p1), 0, null, null);
    final GlobalStateObject state = new GlobalStateObject(ImmutableSet.of(p1),
        ImmutableList.of(vs1), 0, null, null, null);
    SolverValidator.validateInputs(state);
  }

  @SuppressWarnings("null")
  @Test(expected = IllegalArgumentException.class)
  public void validateParcelInTwoInventories() {
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), null,
        ImmutableSet.of(p1), 0, null, null);
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), null,
        ImmutableSet.of(p1), 0, null, null);
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final GlobalStateObject state = new GlobalStateObject(empty,
        ImmutableList.of(vs1, vs2), 0, null, null, null);
    SolverValidator.validateInputs(state);
  }

  @SuppressWarnings("null")
  @Test(expected = IllegalArgumentException.class)
  public void valiateInputsDestinationNotInContents() {
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), null, empty,
        0, p1, null);
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), null,
        ImmutableSet.of(p2, p1), 0, null, null);

    final GlobalStateObject state = new GlobalStateObject(empty,
        ImmutableList.of(vs1, vs2), 0, null, null, null);
    SolverValidator.validateInputs(state);
  }

  /**
   * One route is present, one is not.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute1() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), p,
        ImmutableSet.of(p1), 0, p1, ImmutableList.of(p1));
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), p,
        ImmutableSet.of(p2), 0, null, null);
    final ImmutableSet<ParcelDTO> available = ImmutableSet.of(p3);
    final GlobalStateObject state = new GlobalStateObject(available,
        ImmutableList.of(vs1, vs2), 0, SI.SECOND, SI.METERS_PER_SECOND,
        SI.METER);
    SolverValidator.validateInputs(state);
  }

  /**
   * Parcel 2 occurs in two different routes.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute2() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), p,
        ImmutableSet.<ParcelDTO> of(), 0, p1, ImmutableList.of(p1, p2, p1, p2));
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), p,
        ImmutableSet.<ParcelDTO> of(), 0, null, ImmutableList.of(p2, p2));
    final ImmutableSet<ParcelDTO> available = ImmutableSet.of(p1, p2);
    final GlobalStateObject state = new GlobalStateObject(available,
        ImmutableList.of(vs1, vs2), 0, SI.SECOND, SI.METERS_PER_SECOND,
        SI.METER);
    SolverValidator.validateInputs(state);
  }

  /**
   * Vehicle doesn't have its cargo in its route.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute3() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), p,
        ImmutableSet.of(p1), 0, p1, ImmutableList.of(p3));
    final ImmutableSet<ParcelDTO> available = ImmutableSet.of(p3);
    final GlobalStateObject state = new GlobalStateObject(available,
        ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER);
    SolverValidator.validateInputs(state);
  }

  /**
   * The first location in a route must match the destination field if not null.
   * In this test it is another parcel.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute4a() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), p,
        ImmutableSet.of(p1), 0, p1, ImmutableList.of(p3, p1, p3));
    final ImmutableSet<ParcelDTO> available = ImmutableSet.of(p3);
    final GlobalStateObject state = new GlobalStateObject(available,
        ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER);
    SolverValidator.validateInputs(state);
  }

  /**
   * The first location in a route must match the destination field if not null.
   * In this test the route is empty.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute4b() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), p,
        ImmutableSet.<ParcelDTO> of(), 0, p1, ImmutableList.<ParcelDTO> of());
    final ImmutableSet<ParcelDTO> available = ImmutableSet.of(p1);
    final GlobalStateObject state = new GlobalStateObject(available,
        ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER);
    SolverValidator.validateInputs(state);
  }

  /**
   * Duplicate in route.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute5() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), p,
        ImmutableSet.of(p1), 0, p1, ImmutableList.of(p1, p1, p3));
    final ImmutableSet<ParcelDTO> available = ImmutableSet.of(p3);
    final GlobalStateObject state = new GlobalStateObject(available,
        ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER);
    SolverValidator.validateInputs(state);
  }

  /**
   * Only once occurence of available parcel, should occur twice.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute6a() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), p,
        ImmutableSet.of(p1), 0, p1, ImmutableList.of(p1, p2));
    final ImmutableSet<ParcelDTO> available = ImmutableSet.of(p2);
    final GlobalStateObject state = new GlobalStateObject(available,
        ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER);
    SolverValidator.validateInputs(state);
  }

  /**
   * Too many occurences of available parcel, should occur twice.
   */
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidCurrentRoute6b() {
    final Point p = new Point(0, 0);
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), p,
        ImmutableSet.of(p1), 0, p1, ImmutableList.of(p1, p2, p2, p2));
    final ImmutableSet<ParcelDTO> available = ImmutableSet.of(p2);
    final GlobalStateObject state = new GlobalStateObject(available,
        ImmutableList.of(vs1), 0, SI.SECOND, SI.METERS_PER_SECOND, SI.METER);
    SolverValidator.validateInputs(state);
  }

  /**
   * Valid routes.
   */
  @Test
  public void validateValidCurrentRoutes() {
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), null,
        ImmutableSet.of(p1), 0, p1, ImmutableList.of(p1));
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), null,
        ImmutableSet.of(p2), 0, null, ImmutableList.of(p2));
    final VehicleStateObject vs3 = new VehicleStateObject(vdto(), null, empty,
        0, p3, ImmutableList.<ParcelDTO> of(p3, p3));
    final ImmutableSet<ParcelDTO> available = ImmutableSet.of(p3);
    final GlobalStateObject state = new GlobalStateObject(available,
        ImmutableList.of(vs1, vs2, vs3), 0, null, null, null);
    SolverValidator.validateInputs(state);
  }

  @SuppressWarnings("null")
  @Test
  public void validateCorrectInputs() {
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), null,
        ImmutableSet.of(p1), 0, p1, null);
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), null,
        ImmutableSet.of(p2), 0, null, null);
    final VehicleStateObject vs3 = new VehicleStateObject(vdto(), null, empty,
        0, p3, null);
    final ImmutableSet<ParcelDTO> available = ImmutableSet.of(p3);
    final GlobalStateObject state = new GlobalStateObject(available,
        ImmutableList.of(vs1, vs2, vs3), 0, null, null, null);
    SolverValidator.validateInputs(state);
  }

  @SuppressWarnings("null")
  @Test(expected = IllegalArgumentException.class)
  public void validateInvalidNumberOfRoutes() {
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), null, 0, null, null);
    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList.of();
    final GlobalStateObject state = new GlobalStateObject(null,
        ImmutableList.of(vs1), 0, null, null, null);
    SolverValidator.validateOutputs(routes, state);
  }

  @SuppressWarnings("null")
  @Test(expected = IllegalArgumentException.class)
  public void validateParcelInTwoRoutes() {
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, null, null);
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, null, null);

    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList.of(
        ImmutableList.of(p1, p1), ImmutableList.of(p1, p1));
    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of(p1);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1,
        vs2);
    final GlobalStateObject state = new GlobalStateObject(availableParcels,
        vehicles, 0, null, null, null);
    SolverValidator.validateOutputs(routes, state);
  }

  @SuppressWarnings("null")
  @Test(expected = IllegalArgumentException.class)
  public void validateParcelTooManyTimes1() {
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, null, null);

    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
        .of(ImmutableList.of(p1, p1, p1));
    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of(p1);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1);
    final GlobalStateObject state = new GlobalStateObject(availableParcels,
        vehicles, 0, null, null, null);
    SolverValidator.validateOutputs(routes, state);
  }

  @SuppressWarnings("null")
  @Test(expected = IllegalArgumentException.class)
  public void validateParcelTooManyTimes2() {
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), ImmutableSet.of(p1), 0, null, null);

    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
        .of(ImmutableList.of(p1, p1));
    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of();
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1);
    final GlobalStateObject state = new GlobalStateObject(availableParcels,
        vehicles, 0, null, null, null);
    SolverValidator.validateOutputs(routes, state);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateParcelNotInCargo() {
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, null, null);

    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
        .of(ImmutableList.of(p1));
    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of();
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1);
    @SuppressWarnings("null")
    final GlobalStateObject state = new GlobalStateObject(availableParcels,
        vehicles, 0, null, null, null);
    SolverValidator.validateOutputs(routes, state);
  }

  @SuppressWarnings("null")
  @Test(expected = IllegalArgumentException.class)
  public void validateUnknownParcelInRoute() {
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, null, null);
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, null, null);

    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList.of(
        ImmutableList.of(p1, p1), ImmutableList.of(p2, p3, p3, p2));
    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of(p1, p2);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1,
        vs2);
    final GlobalStateObject state = new GlobalStateObject(availableParcels,
        vehicles, 0, null, null, null);
    SolverValidator.validateOutputs(routes, state);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateIncompleteRoute1() {
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, null, null);
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, null, null);

    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList.of(
        ImmutableList.of(p1, p1), ImmutableList.of(p2, p2));
    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet
        .of(p1, p2, p3);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1,
        vs2);
    @SuppressWarnings("null")
    final GlobalStateObject state = new GlobalStateObject(availableParcels,
        vehicles, 0, null, null, null);
    SolverValidator.validateOutputs(routes, state);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateIncompleteRouteForVehicle() {
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), ImmutableSet.of(p1), 0, null, null);

    final ImmutableList<ParcelDTO> empty = ImmutableList.of();
    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
        .of(empty);
    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of();
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1);
    @SuppressWarnings("null")
    final GlobalStateObject state = new GlobalStateObject(availableParcels,
        vehicles, 0, null, null, null);
    SolverValidator.validateOutputs(routes, state);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateOutputDestinationNotFirstInRoute() {
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, p1, null);

    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
        .of(ImmutableList.of(p2, p1, p1, p2));

    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of(p1, p2);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1);
    final GlobalStateObject state = new GlobalStateObject(availableParcels,
        vehicles, 0, null, null, null);
    SolverValidator.validateOutputs(routes, state);
  }

  @SuppressWarnings("null")
  @Test
  public void validateCorrectOutput() {
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, null, null);
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), new Point(0,
        0), ImmutableSet.of(p3), 0, null, null);

    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList.of(
        ImmutableList.of(p1, p1), ImmutableList.of(p2, p3, p2));
    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of(p1, p2);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1,
        vs2);
    final GlobalStateObject state = new GlobalStateObject(availableParcels,
        vehicles, 0, null, null, null);
    SolverValidator.validateOutputs(routes, state);
  }

  @Test
  public void testWrap() {
    TestUtil.testPrivateConstructor(SolverValidator.class);
    final ImmutableSet<ParcelDTO> empty = ImmutableSet.of();
    final VehicleStateObject vs1 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, null, null);
    final VehicleStateObject vs2 = new VehicleStateObject(vdto(), new Point(0,
        0), ImmutableSet.of(p3), 0, null, null);
    final VehicleStateObject vs3 = new VehicleStateObject(vdto(), new Point(0,
        0), empty, 0, p4, null);

    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList.of(
        ImmutableList.of(p1, p1), ImmutableList.of(p2, p3, p2),
        ImmutableList.of(p4, p5, p5, p4));
    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet.of(p1, p2,
        p4, p5);
    final ImmutableList<VehicleStateObject> vehicles = ImmutableList.of(vs1,
        vs2, vs3);
    @SuppressWarnings("null")
    final GlobalStateObject state = new GlobalStateObject(availableParcels,
        vehicles, 0, null, null, null);
    final Solver solver = SolverValidator.wrap(new FakeSolver(routes));
    solver.solve(state);
  }

  static ParcelDTO parcel() {
    return ParcelDTO.builder(new Point(0, 0), new Point(0, 0)).build();
  }

  static VehicleDTO vdto() {
    return VehicleDTO.builder()
        .startPosition(new Point(0, 0))
        .speed(1d)
        .capacity(1)
        .availabilityTimeWindow(TimeWindow.ALWAYS)
        .build();
  }

  class FakeSolver implements Solver {
    ImmutableList<ImmutableList<ParcelDTO>> answer;

    FakeSolver(ImmutableList<ImmutableList<ParcelDTO>> answer) {
      this.answer = answer;
    }

    @Override
    public ImmutableList<ImmutableList<ParcelDTO>> solve(GlobalStateObject state) {
      return answer;
    }
  }

}
