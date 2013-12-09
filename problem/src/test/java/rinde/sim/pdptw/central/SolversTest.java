/**
 * 
 */
package rinde.sim.pdptw.central;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static rinde.sim.core.TimeLapseFactory.create;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.TimeLapseFactory;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.TestModelProvider;
import rinde.sim.core.model.pdp.DefaultPDPModel;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.event.EventAPI;
import rinde.sim.pdptw.central.GlobalStateObject.VehicleStateObject;
import rinde.sim.pdptw.central.Solvers.SimulationConverter;
import rinde.sim.pdptw.central.Solvers.SolveArgs;
import rinde.sim.pdptw.central.Solvers.StateContext;
import rinde.sim.pdptw.central.arrays.MultiVehicleSolverAdapter;
import rinde.sim.pdptw.central.arrays.RandomMVArraysSolver;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.DefaultVehicle;
import rinde.sim.pdptw.common.PDPRoadModel;
import rinde.sim.pdptw.common.PDPTWTestUtil;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.util.TimeWindow;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class SolversTest {

  // TODO check for determinism of outputs

  PDPRoadModel rm;
  PDPModel pm;
  ModelProvider mp;

  TestVehicle v1;
  TestVehicle v2;
  TestVehicle v3;

  DefaultParcel p1;
  DefaultParcel p2;
  DefaultParcel p3;

  /**
   * Setup an environment with three vehicles and three parcels.
   */
  @Before
  public void setUp() {
    rm = new PDPRoadModel(new PlaneRoadModel(new Point(0, 0),
        new Point(10, 10), SI.KILOMETER, Measure.valueOf(300d,
            NonSI.KILOMETERS_PER_HOUR)), false);
    pm = new DefaultPDPModel(new TardyAllowedPolicy());
    mp = new TestModelProvider(new ArrayList<Model<?>>(
        Arrays.<Model<?>> asList(rm, pm)));
    rm.registerModelProvider(mp);
    pm.registerModelProvider(mp);

    v1 = new TestVehicle(new Point(0, 1));
    v2 = new TestVehicle(new Point(0, 2));
    v3 = new TestVehicle(new Point(0, 3));

    p1 = createParcel(new Point(3, 0), new Point(0, 3));
    p2 = createParcel(new Point(6, 9), new Point(2, 9));
    p3 = createParcel(new Point(2, 8), new Point(8, 2));
  }

  @Test
  public void convertTest() {
    // time unit = hour
    PDPTWTestUtil.register(rm, pm, v1, p1);

    final TestSimAPI simAPI = new TestSimAPI(0, 1, NonSI.MINUTE);
    final SimulationConverter handle = Solvers.converterBuilder().with(mp)
        .with(simAPI).build();

    final StateContext sc = handle.convert(SolveArgs.create().useAllParcels()
        .noCurrentRoutes());
    assertEquals(ImmutableMap.of(v1.dto, v1), sc.vehicleMap);
    assertEquals(ImmutableMap.of(p1.dto, p1), sc.parcelMap);

    assertEquals(ImmutableSet.of(p1.dto), sc.state.availableParcels);
    checkVehicles(asList(v1), sc.state.vehicles);

    rm.moveTo(v1, p1, create(NonSI.HOUR, 0L, 1L));

    checkVehicles(
        asList(v1),
        handle.convert(SolveArgs.create().useAllParcels().noCurrentRoutes()).state.vehicles);

    rm.moveTo(v1, p1, create(NonSI.HOUR, 0, 40));
    assertTrue(rm.equalPosition(v1, p1));
    pm.service(v1, p1, create(NonSI.HOUR, 0, 1));

    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(v1));
    final StateContext sc2 = handle.convert(SolveArgs.create().useAllParcels()
        .noCurrentRoutes());

    assertTrue(sc2.state.availableParcels.contains(p1.dto));
    assertFalse(sc2.state.vehicles.get(0).contents.contains(p1.dto));
    assertSame(p1.dto, sc2.state.vehicles.get(0).destination);
    assertEquals(29, sc2.state.vehicles.get(0).remainingServiceTime);
    assertFalse(sc2.state.vehicles.get(0).route.isPresent());

    // checkVehicles(asList(v1), sc2.state.vehicles);
  }

  /**
   * Checks whether conversion performs correctly in case a parcel is not
   * indicated as being available but is still listed as a destination (and is
   * available).
   */
  @Test
  public void convertWithAbsentDestination() {
    PDPTWTestUtil.register(rm, pm, v1, p1);
    final TestSimAPI simAPI = new TestSimAPI(0, 1, NonSI.MINUTE);

    final DefaultParcel destination = rm.getObjectsOfType(DefaultParcel.class)
        .iterator().next();
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000));
    assertEquals(destination, rm.getDestinationToParcel(v1));
    assertEquals(ParcelState.AVAILABLE, pm.getParcelState(p1));

    final SimulationConverter handle = Solvers.converterBuilder().with(mp)
        .with(simAPI).build();

    final StateContext sc = handle.convert(SolveArgs.create()
        .useParcels(ImmutableSet.<DefaultParcel> of())
        .noCurrentRoutes());
    assertEquals(1, sc.state.availableParcels.size());
    assertEquals(0, sc.state.vehicles.get(0).contents.size());
    final Solver solver = SolverValidator.wrap(new MultiVehicleSolverAdapter(
        new RandomMVArraysSolver(new MersenneTwister(123)), NonSI.MINUTE));
    Solvers.solverBuilder(solver).with(mp).with(simAPI).build().solve(sc);

    // give enough time to reach destination
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000000000));
    assertEquals(rm.getPosition(destination), rm.getPosition(v1));

    pm.pickup(v1, destination, TimeLapseFactory.create(0, 1));
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(v1));

    final StateContext sc2 = handle.convert(SolveArgs.create()
        .useParcels(ImmutableSet.<DefaultParcel> of())
        .noCurrentRoutes());
    assertEquals(1, sc2.state.availableParcels.size());
    assertEquals(0, sc2.state.vehicles.get(0).contents.size());

    // finish pickup operation
    v1.tick(TimeLapseFactory.create(0, 1000000000));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(v1));

    final StateContext sc3 = handle.convert(SolveArgs.create()
        .useParcels(ImmutableSet.<DefaultParcel> of())
        .noCurrentRoutes());
    assertTrue(sc3.state.availableParcels.isEmpty());
    assertEquals(1, sc3.state.vehicles.get(0).contents.size());

    // move to delivery location
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000));
    assertEquals(destination, rm.getDestinationToParcel(v1));
    assertEquals(ParcelState.IN_CARGO, pm.getParcelState(p1));

    final StateContext sc4 = handle.convert(SolveArgs.create()
        .useParcels(ImmutableSet.<DefaultParcel> of())
        .noCurrentRoutes());
    assertEquals(1, sc4.state.vehicles.get(0).contents.size());
    assertTrue(sc4.state.availableParcels.isEmpty());

    // service delivery
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000000000));
    assertEquals(destination.dto.destinationLocation, rm.getPosition(v1));
    pm.deliver(v1, p1, TimeLapseFactory.create(0, 1));
    assertNull(rm.getDestinationToParcel(v1));
    assertEquals(VehicleState.DELIVERING, pm.getVehicleState(v1));
    final StateContext sc5 = handle.convert(SolveArgs.create()
        .useParcels(ImmutableSet.<DefaultParcel> of())
        .noCurrentRoutes());
    assertEquals(1, sc5.state.vehicles.get(0).contents.size());
    assertTrue(sc5.state.availableParcels.isEmpty());

    // finish delivery operation
    v1.tick(TimeLapseFactory.create(0, 1000000000));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(v1));

    final StateContext sc6 = handle.convert(SolveArgs.create()
        .useParcels(ImmutableSet.<DefaultParcel> of())
        .noCurrentRoutes());
    assertTrue(sc6.state.vehicles.get(0).contents.isEmpty());
    assertTrue(sc6.state.availableParcels.isEmpty());
  }

  // doesn't check the contents!
  void checkVehicles(List<? extends TestVehicle> expected,
      ImmutableList<VehicleStateObject> states) {

    assertEquals(expected.size(), states.size());

    for (int i = 0; i < expected.size(); i++) {

      final TestVehicle vehicle = expected.get(i);
      final VehicleDTO dto = vehicle.dto;
      final VehicleStateObject vs = states.get(i);

      assertEquals(dto.availabilityTimeWindow, vs.availabilityTimeWindow);
      assertEquals(dto.capacity, vs.capacity);
      assertEquals(dto.speed, vs.speed, 0);
      assertEquals(dto.startPosition, vs.startPosition);

      assertEquals(rm.getPosition(expected.get(i)), vs.location);

      final DefaultParcel dest = rm.getDestinationToParcel(vehicle);
      if (dest == null) {
        assertNull(vs.destination);
      } else {
        assertEquals(dest.dto, vs.destination);
      }

      if (pm.getVehicleState(vehicle) == VehicleState.IDLE) {
        assertEquals(0, vs.remainingServiceTime);
      } else {
        assertEquals(pm.getVehicleActionInfo(vehicle).timeNeeded(),
            vs.remainingServiceTime);
      }
    }
  }

  static Set<ParcelDTO> toParcelDTOs(Collection<? extends Parcel> ps) {
    final ImmutableSet.Builder<ParcelDTO> builder = ImmutableSet.builder();
    for (final Parcel p : ps) {
      builder.add(((DefaultParcel) p).dto);
    }
    return builder.build();
  }

  static final TimeWindow TW = new TimeWindow(0, 1000);

  static DefaultParcel createParcel(Point origin, Point dest) {
    return new DefaultParcel(new ParcelDTO(origin, dest, TW, TW, 0, 0, 30, 30));
  }

  static class TestVehicle extends DefaultVehicle {
    public final VehicleDTO dto;

    TestVehicle(Point start) {
      super(new VehicleDTO(start, .1, 1, TW));
      dto = getDTO();
    }

    @Override
    protected void tickImpl(TimeLapse time) {}
  }

  static class TestSimAPI implements SimulatorAPI {

    long time;
    final long step;
    final Unit<Duration> unit;

    TestSimAPI(long currentTime, long step, Unit<Duration> unit) {
      time = currentTime;
      this.step = step;
      this.unit = unit;
    }

    @Override
    public boolean register(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean unregister(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public RandomGenerator getRandomGenerator() {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getCurrentTime() {
      return time;
    }

    @Override
    public long getTimeStep() {
      return step;
    }

    @Override
    public Unit<Duration> getTimeUnit() {
      return unit;
    }

    public void setTime(long t) {
      time = t;
    }

    @Override
    public EventAPI getEventAPI() {
      throw new UnsupportedOperationException();
    }
  }
}
