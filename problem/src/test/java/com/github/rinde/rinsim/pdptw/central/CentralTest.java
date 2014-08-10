/**
 * 
 */
package com.github.rinde.rinsim.pdptw.central;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.measure.Measure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.pdptw.DefaultDepot;
import com.github.rinde.rinsim.core.pdptw.DefaultParcel;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.fsm.State;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.central.Central;
import com.github.rinde.rinsim.pdptw.central.Solver;
import com.github.rinde.rinsim.pdptw.central.SolverValidator;
import com.github.rinde.rinsim.pdptw.central.Solvers;
import com.github.rinde.rinsim.pdptw.central.Solvers.SimulationConverter;
import com.github.rinde.rinsim.pdptw.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.pdptw.central.Solvers.StateContext;
import com.github.rinde.rinsim.pdptw.central.arrays.ArraysSolverValidator;
import com.github.rinde.rinsim.pdptw.central.arrays.MultiVehicleSolverAdapter;
import com.github.rinde.rinsim.pdptw.central.arrays.RandomMVArraysSolver;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.PDPTWTestUtil;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.experiment.Experiment;
import com.github.rinde.rinsim.pdptw.experiment.ExperimentResults;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06Scenario;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
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
  DefaultDepot depot;
  @SuppressWarnings("null")
  DefaultParcel p1, p2, p3;

  @Before
  public void setUp() {
    sim = new Simulator(new MersenneTwister(123), Measure.valueOf(1000L,
        SI.MILLI(SI.SECOND)));
    rm = new PDPRoadModel(new PlaneRoadModel(new Point(0, 0),
        new Point(10, 10), SI.KILOMETER, Measure.valueOf(300d,
            NonSI.KILOMETERS_PER_HOUR)), false);
    pm = new DefaultPDPModel(TimeWindowPolicies.TARDY_ALLOWED);

    depot = new DefaultDepot(new Point(5, 5));
    sim.register(rm);
    sim.register(pm);
    sim.configure();
    sim.register(depot);

    p1 = createParcel(new Point(3, 0), new Point(0, 3));
    p2 = createParcel(new Point(6, 9), new Point(2, 9));
    p3 = createParcel(new Point(2, 8), new Point(8, 2));
  }

  /**
   * Tests whether the SolverConfigurator works.
   */
  @Test
  public void testConfigurator() {
    final Gendreau06Scenario scenario = Gendreau06Parser.parse(
        new File("files/test/gendreau06/req_rapide_1_240_24"));

    final StochasticSupplier<Solver> s = new StochasticSupplier<Solver>() {
      @Override
      public Solver get(long seed) {
        return SolverValidator.wrap(new MultiVehicleSolverAdapter(
            ArraysSolverValidator.wrap(new RandomMVArraysSolver(
                new MersenneTwister(seed))), scenario.getTimeUnit()));
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
    assertEquals(2, res.state.vehicles.size());
    assertTrue(res.state.vehicles.get(0).contents.isEmpty());
    assertNull(res.state.vehicles.get(0).destination);
    assertEquals(3, res.state.availableParcels.size());
    assertEquals(v1.getWaitState(), v1.getState());

    // start moving: goto
    v1.setRoute(asList(p1, p2));
    assertEquals(asList(p1, p2), newArrayList(v1.getRoute()));
    assertEquals(new Point(0, 1), rm.getPosition(v1));
    sim.tick();

    while (v1.getState() == v1.getGotoState()) {
      assertFalse(new Point(0, 1).equals(rm.getPosition(v1)));
      res = s.convert(SolveArgs.create().useAllParcels().noCurrentRoutes());
      assertEquals(2, res.state.vehicles.size());
      assertTrue(res.state.vehicles.get(0).contents.isEmpty());
      assertEquals(p1.dto, res.state.vehicles.get(0).destination);
      assertEquals(3, res.state.availableParcels.size());
      assertEquals(v1.getGotoState(), v1.getState());
      sim.tick();
    }
    // arrived at parcel1: waitForService
    assertEquals(new Point(3, 0), rm.getPosition(v1));
    assertEquals(v1.getWaitForServiceState(), v1.getState());
    res = s.convert(SolveArgs.create().useAllParcels().noCurrentRoutes());
    assertEquals(2, res.state.vehicles.size());
    assertTrue(res.state.vehicles.get(0).contents.isEmpty());
    assertEquals(p1.dto, res.state.vehicles.get(0).destination);
    assertEquals(3, res.state.availableParcels.size());

    // start servicing: service
    sim.tick();
    assertEquals(v1.getServiceState(), v1.getState());
    res = s.convert(SolveArgs.create().useAllParcels().noCurrentRoutes());
    assertSame(p1.dto, res.state.vehicles.get(0).destination);
    assertEquals(v1.getServiceState(), v1.getState());
  }

  static DefaultParcel createParcel(Point origin, Point dest) {
    return new DefaultParcel(new ParcelDTO(origin, dest, new TimeWindow(380001,
        380002), new TimeWindow(0, 1000), 0, 0, 3000, 3000));
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
