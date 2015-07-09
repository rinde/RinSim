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
package com.github.rinde.rinsim.central.rt;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.RandomSolver;
import com.github.rinde.rinsim.central.SolverValidator;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.RealTimeClockController;
import com.github.rinde.rinsim.core.model.time.RealTimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Range;

/**
 * @author Rinde van Lon
 *
 */
public class RealtimeSolverTest {

  static Simulator.Builder init(boolean timeOut,
    TimedEventHandler<AddVehicleEvent> vehicleHandler) {

    Scenario.Builder b = Scenario.builder()
      .addEvent(AddDepotEvent.create(-1, new Point(5, 5)))
      .addEvent(AddVehicleEvent.create(-1, VehicleDTO.builder().build()))
      .addEvent(AddVehicleEvent.create(-1, VehicleDTO.builder().build()))
      .addEvent(
        AddParcelEvent
          .create(Parcel.builder(new Point(0, 0), new Point(3, 3))
            .orderAnnounceTime(200)
            .pickupTimeWindow(new TimeWindow(1000, 2000))
            .buildDTO()))
      .addEvent(
        AddParcelEvent
          .create(Parcel.builder(new Point(0, 0), new Point(3, 3))
            .orderAnnounceTime(60000)
            .pickupTimeWindow(new TimeWindow(60000, 80000))
            .serviceDuration(180000L)
            .buildDTO()));
    if (timeOut) {
      b.addEvent(TimeOutEvent.create(3000));
    }
    Scenario scenario = b.build();

    ScenarioController.Builder sb = ScenarioController.builder(scenario)
      .withEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler())
      .withEventHandler(AddVehicleEvent.class, vehicleHandler)
      .withEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
      .withOrStopCondition(StatsStopConditions.vehiclesDoneAndBackAtDepot())
      .withOrStopCondition(StatsStopConditions.timeOutEvent());

    if (timeOut) {
      sb = sb.withEventHandler(TimeOutEvent.class,
        TimeOutEvent.ignoreHandler());
    }

    return Simulator.builder()
      .addModel(PDPRoadModel.builder(RoadModelBuilders.plane()))
      .addModel(DefaultPDPModel.builder())
      .addModel(TimeModel.builder()
        .withRealTime()
        .withStartInClockMode(ClockMode.SIMULATED)
        .withTickLength(100))
      .addModel(sb)
      .addModel(StatsTracker.builder());

  }

  /**
   * This test tests whether the clock mode is switched automatically from
   * simulated to real time and back when needed.
   */
  @Test
  public void testDynamicClockModeSwitching() {
    Simulator sim = init(true, RtCentral.vehicleHandler())
      .addModel(
        RtCentral.builder(StochasticSuppliers.constant(new TestRtSolver())))
      .addModel(ClockInspectModel.builder())
      .build();

    sim.start();

    ClockInspectModel cim = sim.getModelProvider()
      .getModel(ClockInspectModel.class);

    cim.assertLog();
  }

  // TODO new test:
  // 1. problem changes (state1)
  // 2. solver start computing (state1)
  // 3. problem changes (state2)
  // 4. solver starts computing (state2)
  // 5. solver finishes computing (state1)
  // 6. solver finishes computing (state2)

  // TODO what if 5 and 6 happen in different order? can this happen?

  // TODO finish this test
  @Test
  public void test() {
    final List<RouteFollowingVehicle> vehicles = new ArrayList<>();
    Simulator sim = init(false, new TestVehicleHandler(vehicles))
      .addModel(
        RtCentral.builderAdapt(SolverValidator.wrap(RandomSolver.supplier())))
      .addModel(
        View.builder()
          .withAutoPlay()
          .with(PDPModelRenderer.builder())
          .with(RouteRenderer.builder())
          .with(RoadUserRenderer.builder())
          .with(PlaneRoadModelRenderer.builder())
          .with(TimeLinePanel.builder()))
      .build();

    sim.getModelProvider().getModel(PDPModel.class).getEventAPI()
      .addListener(new Listener() {

        @Override
        public void handleEvent(Event e) {
          System.out.println(e);

        }
      }, PDPModelEventType.values());

    sim.register(new TickListener() {

      @Override
      public void tick(TimeLapse timeLapse) {
        // System.out.println(timeLapse);

        // if (timeLapse.getStartTime() == 300) {
        for (int i = 0; i < vehicles.size(); i++) {

          // System.out.println(i + " " + vehicles.get(i).getRoute());
        }
        // }

      }

      @Override
      public void afterTick(TimeLapse timeLapse) {
        // if (timeLapse.getStartTime() == 300) {
        for (int i = 0; i < vehicles.size(); i++) {

          // System.out.println(i + " " + vehicles.get(i).getRoute());
        }
        // }

      }
    });

    sim.start();
  }

  static class TestVehicleHandler
    implements TimedEventHandler<AddVehicleEvent> {

    final List<RouteFollowingVehicle> vehicles;

    // all created vehicles will be added to this list
    TestVehicleHandler(List<RouteFollowingVehicle> vs) {
      vehicles = vs;
    }

    @Override
    public void handleTimedEvent(AddVehicleEvent event,
      SimulatorAPI simulator) {
      RouteFollowingVehicle v = new RouteFollowingVehicle(event.getVehicleDTO(),
        false);
      simulator.register(v);
      vehicles.add(v);
    }
  }

  static class ClockInspectModel extends AbstractModelVoid
    implements TickListener {
    RealTimeClockController clock;

    List<ClockLogEntry> clockLog;

    ClockInspectModel(RealTimeClockController c) {
      clock = c;
      clockLog = new ArrayList<>();
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      clockLog.add(
        new ClockLogEntry(System.nanoTime(), timeLapse, clock.getClockMode()));
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    void assertLog() {
      // events at 200 and 1000
      assertThat(clockLog).hasSize(31);

      assertClockModeIs(ClockMode.SIMULATED, 0, 2);
      assertClockModeIs(ClockMode.REAL_TIME, 3, 5);
      // 6 can be either, depending on thread scheduling
      assertClockModeIs(ClockMode.SIMULATED, 7, 10);
      assertClockModeIs(ClockMode.REAL_TIME, 11, 13);
      // 14 can be either, depending on thread scheduling
      assertClockModeIs(ClockMode.SIMULATED, 15, 30);

      assertTimeConsistency();
    }

    void assertClockModeIs(ClockMode m, int start, int end) {
      for (int i = start; i <= end; i++) {
        assertThat(clockLog.get(i).mode).named(Integer.toString(i))
          .isEqualTo(m);
      }
    }

    void assertTimeConsistency() {
      final PeekingIterator<ClockLogEntry> it = Iterators
        .peekingIterator(clockLog.iterator());
      final List<Double> interArrivalTimes = new ArrayList<>();
      for (ClockLogEntry l1 = it.next(); it.hasNext(); l1 = it.next()) {
        final ClockLogEntry l2 = it.peek();
        interArrivalTimes.add((l2.wallTime - l1.wallTime) / 1000000d);
      }

      Range<Double> acceptableDuration = Range.closed(90d, 110d);
      assertThat(sum(interArrivalTimes.subList(0, 3))).isAtMost(100d);
      assertThat(interArrivalTimes.get(3)).isIn(acceptableDuration);
      assertThat(interArrivalTimes.get(4)).isIn(acceptableDuration);
      assertThat(sum(interArrivalTimes.subList(6, 11))).isAtMost(100d);
      assertThat(interArrivalTimes.get(11)).isIn(acceptableDuration);
      assertThat(interArrivalTimes.get(12)).isIn(acceptableDuration);
      assertThat(sum(interArrivalTimes.subList(14, 30))).isAtMost(100d);
    }

    double sum(List<Double> list) {
      double sum = 0d;
      for (double l : list) {
        sum += l;
      }
      return sum;
    }

    static Builder builder() {
      return new AutoValue_RealtimeSolverTest_ClockInspectModel_Builder();
    }

    static class ClockLogEntry {
      final long wallTime;
      final Range<Long> timeLapse;
      final ClockMode mode;

      ClockLogEntry(long wt, TimeLapse tl, ClockMode m) {
        wallTime = wt;
        timeLapse = Range.closedOpen(tl.getStartTime(), tl.getEndTime());
        mode = m;
      }
    }

    @AutoValue
    abstract static class Builder
      extends AbstractModelBuilder<ClockInspectModel, Void> {

      Builder() {
        setDependencies(RealTimeClockController.class);
      }

      @Override
      public ClockInspectModel build(DependencyProvider dependencyProvider) {
        RealTimeClockController c = dependencyProvider
          .get(RealTimeClockController.class);
        return new ClockInspectModel(c);
      }
    }
  }

  static class TestRtSolver implements RealtimeSolver {
    Optional<Scheduler> scheduler = Optional.absent();

    @Override
    public void init(Scheduler s) {
      boolean fail = false;
      try {
        s.getCurrentSchedule();
      } catch (IllegalStateException e) {
        fail = true;
        assertThat(e.getMessage()).contains("No schedule has been set");
      }
      assertThat(fail).isTrue();
      scheduler = Optional.of(s);
    }

    @Override
    public void receiveSnapshot(GlobalStateObject snapshot) {
      assertThat(scheduler.isPresent()).isTrue();
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
      scheduler.get().doneForNow();

      ImmutableList<ImmutableList<Parcel>> schedule = ImmutableList
        .of(ImmutableList.copyOf(snapshot.getAvailableParcels()),
          ImmutableList.<Parcel> of());

      scheduler.get().updateSchedule(schedule);
      assertThat(scheduler.get().getCurrentSchedule()).isEqualTo(schedule);
    }
  }
}
