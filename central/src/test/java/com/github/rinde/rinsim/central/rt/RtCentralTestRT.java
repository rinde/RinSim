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
package com.github.rinde.rinsim.central.rt;

import static com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode.REAL_TIME;
import static com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode.SIMULATED;
import static com.google.common.truth.Truth.assertThat;

import java.util.Set;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.central.RandomSolver;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.core.model.time.TimeUtil;
import com.github.rinde.rinsim.core.model.time.TimeUtil.TimeTracker;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.testutil.RealtimeTests;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.collect.ImmutableSet;

/**
 * Tests for {@link RtCentral} that are dependent on a real-time clock.
 * @author Rinde van Lon
 */
@Category(RealtimeTests.class)
public class RtCentralTestRT {

  /**
   * Tests that switch to simulated time is ignored at the moment that a parcel
   * is announced.
   */
  @Test
  public void testStayInRt() {
    final Set<TimedEvent> events = ImmutableSet.<TimedEvent>builder()
      .add(AddParcelEvent.create(
        Parcel.builder(new Point(0, 0), new Point(1, 0))
          .orderAnnounceTime(300)
          .pickupTimeWindow(TimeWindow.create(400, 3000))
          .buildDTO()))
      .add(AddParcelEvent.create(
        Parcel.builder(new Point(0, 0), new Point(1, 0))
          .orderAnnounceTime(800)
          .pickupTimeWindow(TimeWindow.create(800, 3000))
          .buildDTO()))
      .add(TimeOutEvent.create(1500))
      .build();

    final Simulator sim = RealtimeTestHelper
      .init(RtCentral.vehicleHandler(), events)
      .addModel(TimeUtil.timeTracker())
      .addModel(RtCentral.builderAdapt(RandomSolver.supplier()))
      .build();

    final RealtimeClockController clock =
      (RealtimeClockController) sim.getModelProvider()
        .getModel(TimeModel.class);

    // 200 -> switch to RT, switches to ST should be ignored
    // 300 -> new parcel
    // 500 -> computation done, switch to ST in next tick
    // 600 -> manual switch to RT
    // 700 -> switch to RT because of new parcel in next tick, attempt to
    // switch back to ST by solver model should be ignored here
    // 800 -> new parcel
    // 1000 -> computation done, switch to ST in next tick
    // 1500 -> switch to RT

    final TimeTracker tt = sim.getModelProvider().getModel(TimeTracker.class);
    sim.register(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        if (timeLapse.getStartTime() == 200) {
          clock.switchToSimulatedTime();
        } else if (timeLapse.getStartTime() == 600) {
          clock.switchToRealTime();
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });
    sim.start();

    assertThat(tt.getClockModes().subList(0, 3))
      .containsExactly(SIMULATED, SIMULATED, SIMULATED).inOrder();

    assertThat(tt.getClockModes().subList(3, 7))
      .containsExactly(REAL_TIME, REAL_TIME, REAL_TIME, REAL_TIME).inOrder();

    assertThat(tt.getClockModes().get(7)).isEqualTo(REAL_TIME);
    assertThat(tt.getClockModes().get(8)).isEqualTo(REAL_TIME);
    assertThat(tt.getClockModes().get(9)).isEqualTo(REAL_TIME);
    assertThat(tt.getClockModes().get(10)).isEqualTo(REAL_TIME);
    assertThat(tt.getClockModes().get(11)).isEqualTo(REAL_TIME);
    assertThat(tt.getClockModes().subList(12, 15))
      .containsExactly(SIMULATED, SIMULATED, SIMULATED)
      .inOrder();
    assertThat(tt.getClockModes().get(15)).isEqualTo(REAL_TIME);
  }
}
