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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.central.Central.VehicleCreator;
import com.github.rinde.rinsim.central.Solvers.SimulationConverter;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.Solvers.StateContext;
import com.github.rinde.rinsim.central.arrays.ArraysSolverValidator;
import com.github.rinde.rinsim.central.arrays.MultiVehicleSolverAdapter;
import com.github.rinde.rinsim.central.arrays.RandomMVArraysSolver;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.fsm.State;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.PDPTWTestUtil;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Scenario;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * @author Rinde van Lon
 * 
 */
public class CentralTest {

  @SuppressWarnings("null")
  Simulator sim;
  @SuppressWarnings("null")
  PDPRoadModel rm;
  @SuppressWarnings("null")
  PDPModel pm;
  @SuppressWarnings("null")
  Depot depot;
  @SuppressWarnings("null")
  Parcel p1, p2, p3;

  @Before
  public void setUp() {
    sim = Simulator.builder()
      .addModel(
        PDPRoadModel.builder(
          RoadModelBuilders.plane()
            .withMaxSpeed(300d))
          .withAllowVehicleDiversion(false))
      .addModel(
        DefaultPDPModel.builder()
          .withTimeWindowPolicy(TimeWindowPolicies.TARDY_ALLOWED))
      .build();

    rm = sim.getModelProvider().getModel(PDPRoadModel.class);
    pm = sim.getModelProvider().getModel(PDPModel.class);

    depot = new Depot(new Point(5, 5));
    sim.register(depot);

    p1 = createParcel(new Point(3, 0), new Point(0, 3));
    p2 = createParcel(new Point(6, 9), new Point(2, 9));
    p3 = createParcel(new Point(2, 8), new Point(8, 2));

    TestUtil.testPrivateConstructor(Central.class);
    TestUtil.testPrivateConstructor(Solvers.class);
    TestUtil.testEnum(VehicleCreator.class);
  }

  /**
   * Tests whether the SolverConfigurator works.
   */
  @Test
  public void testConfigurator() {
    final Gendreau06Scenario scenario = Gendreau06Parser.parse(
      new File(ScenarioPaths.GENDREAU));

    final StochasticSupplier<Solver> s = new StochasticSupplier<Solver>() {
      @Override
      public Solver get(long seed) {
        return SolverValidator.wrap(new MultiVehicleSolverAdapter(
          ArraysSolverValidator.wrap(new RandomMVArraysSolver(
            new MersenneTwister(seed))),
          SI.MILLI(SI.SECOND)));
      }
    };
    final Experiment.Builder builder = Experiment
      .build(Gendreau06ObjectiveFunction.instance())
      .addScenario(scenario)
      .addConfiguration(Central.solverConfiguration(s))
      .withRandomSeed(123);

    final ExperimentResults res1 = builder.perform();
    final ExperimentResults res2 = builder.perform();

    assertEquals(res1.results, res2.results);
  }

  @Test
  public void test() {
    final TestVehicle v1 = new TestVehicle(new Point(0, 1));
    final TestVehicle v2 = new TestVehicle(new Point(0, 1));
    PDPTWTestUtil.register(sim, p1, p2, p3, v1, v2);

    final SimulationConverter s = Solvers.converterBuilder().with(sim).build();

    StateContext res = s.convert(SolveArgs.create().useAllParcels()
      .noCurrentRoutes());
    assertEquals(2, res.state.getVehicles().size());
    assertTrue(res.state.getVehicles().get(0).getContents().isEmpty());
    assertFalse(res.state.getVehicles().get(0).getDestination().isPresent());
    assertEquals(3, res.state.getAvailableParcels().size());
    assertEquals(v1.getWaitState(), v1.getState());

    // start moving: goto
    v1.setRoute(asList(p1, p2));
    assertEquals(asList(p1, p2), newArrayList(v1.getRoute()));
    assertEquals(new Point(0, 1), rm.getPosition(v1));
    sim.tick();

    while (v1.getState() == v1.getGotoState()) {
      assertFalse(new Point(0, 1).equals(rm.getPosition(v1)));
      res = s.convert(SolveArgs.create().useAllParcels().noCurrentRoutes());
      assertEquals(2, res.state.getVehicles().size());
      assertTrue(res.state.getVehicles().get(0).getContents().isEmpty());
      assertEquals(p1, res.state.getVehicles().get(0).getDestination().get());
      assertEquals(3, res.state.getAvailableParcels().size());
      assertEquals(v1.getGotoState(), v1.getState());
      sim.tick();
    }
    // arrived at parcel1: waitForService
    assertEquals(new Point(3, 0), rm.getPosition(v1));
    assertEquals(v1.getWaitForServiceState(), v1.getState());
    res = s.convert(SolveArgs.create().useAllParcels().noCurrentRoutes());
    assertEquals(2, res.state.getVehicles().size());
    assertTrue(res.state.getVehicles().get(0).getContents().isEmpty());
    assertEquals(p1, res.state.getVehicles().get(0).getDestination().get());
    assertEquals(3, res.state.getAvailableParcels().size());

    // start servicing: service
    sim.tick();
    assertEquals(v1.getServiceState(), v1.getState());
    res = s.convert(SolveArgs.create().useAllParcels().noCurrentRoutes());
    assertSame(p1, res.state.getVehicles().get(0).getDestination().get());
    assertEquals(v1.getServiceState(), v1.getState());
  }

  static Parcel createParcel(Point origin, Point dest) {
    return new Parcel(
      Parcel.builder(origin, dest)
        .pickupTimeWindow(new TimeWindow(380001, 380002))
        .deliveryTimeWindow(new TimeWindow(0, 1000))
        .neededCapacity(0)
        .orderAnnounceTime(0L)
        .pickupDuration(3000L)
        .deliveryDuration(3000L)
        .buildDTO());
  }

  static class TestVehicle extends RouteFollowingVehicle {

    TestVehicle(Point start) {
      super(VehicleDTO.builder()
        .startPosition(start)
        .speed(30d)
        .capacity(1)
        .availabilityTimeWindow(new TimeWindow(0, 1000))
        .build(),
        false);
    }

    public State<StateEvent, RouteFollowingVehicle> getState() {
      return stateMachine.getCurrentState();
    }

    public State<StateEvent, RouteFollowingVehicle> getWaitState() {
      return waitState;
    }

    public State<StateEvent, RouteFollowingVehicle> getWaitForServiceState() {
      return waitForServiceState;
    }

    public State<StateEvent, RouteFollowingVehicle> getGotoState() {
      return gotoState;
    }

    public State<StateEvent, RouteFollowingVehicle> getServiceState() {
      return serviceState;
    }
  }

}
