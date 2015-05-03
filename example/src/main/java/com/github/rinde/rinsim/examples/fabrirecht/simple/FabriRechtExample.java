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
package com.github.rinde.rinsim.examples.fabrirecht.simple;

import java.io.File;
import java.io.IOException;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.pdptw.DefaultParcel;
import com.github.rinde.rinsim.core.pdptw.DefaultVehicle;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.Creator;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.scenario.fabrirecht.FabriRechtParser;
import com.github.rinde.rinsim.scenario.fabrirecht.FabriRechtScenario;
import com.github.rinde.rinsim.ui.View;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

/**
 * Simplest example showing how the Fabri {@literal &} Recht problem can be
 * configured using a custom vehicle.
 *
 * @author Rinde van Lon
 */
public final class FabriRechtExample {

  private FabriRechtExample() {}

  public static void main2(String[] args) throws IOException {
    // we load a problem instance from disk, we instantiate it with 8
    // trucks, each with a capacity of 20 units
    final FabriRechtScenario scenario = FabriRechtParser
      .fromJson(Files.toString(
        new File("../problem/data/test/fabri-recht/lc101.scenario"),
        Charsets.UTF_8), 8, 20);

    // we plug our custom vehicle in by specifying a creator
    final ImmutableMap<Class<?>, TimedEventHandler<?>> m = ImmutableMap
      .<Class<?>, TimedEventHandler<?>> of(
        AddVehicleEvent.class,
        DynamicPDPTWProblem.adaptCreator(new Creator<AddVehicleEvent>() {
          @Override
          public boolean create(Simulator sim, AddVehicleEvent event) {
            sim.register(new Truck(event.vehicleDTO));
            return true;
          }
        }));

    // instantiate the problem using the scenario and a random seed (which
    // will not be used in this example)
    final DynamicPDPTWProblem problem = new DynamicPDPTWProblem(scenario, 123,
      ImmutableList.<ModelBuilder<?, ?>> of(
        View.create()
          .enableAutoPlay()

        ), m);

    // enable the default UI
    // problem.enableUI();

    // start the simulation
    problem.simulate();

    // simulation is done, lets print the statistics!
    System.out.println(problem.getStatistics());
  }
}

/**
 * This truck implementation only picks parcels up, it does not deliver them.
 *
 * @author Rinde van Lon
 */
class Truck extends DefaultVehicle {

  public Truck(VehicleDTO pDto) {
    super(pDto);
  }

  @Override
  protected void tickImpl(TimeLapse time) {
    final RoadModel rm = roadModel.get();
    final PDPModel pm = pdpModel.get();
    // we always go to the closest available parcel
    final DefaultParcel closest = (DefaultParcel) RoadModels
      .findClosestObject(rm.getPosition(this), rm, new Predicate<RoadUser>() {
        @Override
        public boolean apply(RoadUser input) {
          return input instanceof DefaultParcel
            && pm.getParcelState((DefaultParcel) input) == ParcelState.AVAILABLE;
        }
      });

    if (closest != null) {
      rm.moveTo(this, closest, time);
      if (rm.equalPosition(closest, this)
        && pm
          .getTimeWindowPolicy()
          .canPickup(closest.getPickupTimeWindow(), time.getTime(),
            closest.getPickupDuration())) {
        pm.pickup(this, closest, time);
      }
    }
  }
}
