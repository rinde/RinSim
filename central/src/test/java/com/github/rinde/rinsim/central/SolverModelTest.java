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

import static com.google.common.truth.Truth.assertThat;

import java.util.Collection;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;

/**
 * Tests the {@link SolverModel}.
 * @author Rinde van Lon
 */
public class SolverModelTest {

  @SuppressWarnings("null")
  Simulator sim;

  /**
   * Set up a simulator.
   */
  @Before
  public void setUp() {
    sim = Simulator.builder()
        .addModel(DefaultPDPModel.builder())
        .addModel(PDPRoadModel.builder(RoadModelBuilders.plane()))
        .addModel(SolverModel.builder())
        .build();
  }

  /**
   * Tests that the solver model provides are functional solver.
   */
  @Test
  public void test() {
    final Simulator s = sim;
    sim.register(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        if (timeLapse.getTime() >= 1.5 * 60 * 60 * 1000) {
          s.stop();
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    sim.register(new Depot(new Point(5, 5)));
    final Agent agent = new Agent();

    sim.register(agent);
    for (int i = 0; i < 10; i++) {
      sim.register(Parcel.builder(new Point(i / 2d, 5 + i / 2),
        new Point(10 - i / 3, i / 2))
          .build());
    }

    sim.start();

    final PDPModel pm = sim.getModelProvider().getModel(PDPModel.class);
    final Collection<Parcel> allParcels = pm.getParcels(ParcelState.values());
    final Collection<Parcel> deliveredParcels =
      pm.getParcels(ParcelState.DELIVERED);

    assertThat(allParcels).containsExactlyElementsIn(deliveredParcels);
  }

  static class Agent extends RouteFollowingVehicle implements SolverUser {

    Optional<SimSolver> solver;

    Agent() {
      super(VehicleDTO.builder()
          .startPosition(new Point(1, 1))
          .availabilityTimeWindow(TimeWindow.create(0, 1 * 60 * 60 * 1000))
          .build(), true);
      solver = Optional.absent();
    }

    @Override
    public void setSolverProvider(SimSolverBuilder provider) {
      solver = Optional.of(provider.setVehicle(this)
          .build(new RandomSolver(new MersenneTwister(123))));
    }

    @Override
    protected void preTick(TimeLapse time) {
      if (getRoute().isEmpty()
          && !getRoadModel().getObjectsOfType(Parcel.class).isEmpty()) {
        setRoute(solver.get().solve(SolveArgs.create()
            .useAllParcels()).get(0));
      }
    }
  }
}
