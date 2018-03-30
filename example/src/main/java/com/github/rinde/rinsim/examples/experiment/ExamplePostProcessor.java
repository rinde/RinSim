/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
package com.github.rinde.rinsim.examples.experiment;

import java.util.Set;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.PostProcessor;

/**
 * This is an example implementation of a {@link PostProcessor}. In this example
 * the simulation result is a string.
 * @author Rinde van Lon
 */
public final class ExamplePostProcessor implements PostProcessor<String> {

  ExamplePostProcessor() {}

  @Override
  public String collectResults(Simulator sim, SimArgs args) {
    // Read state of simulator, check how many vehicles exist in road model:
    final Set<Vehicle> vehicles = sim.getModelProvider()
      .getModel(RoadModel.class).getObjectsOfType(Vehicle.class);

    // Construct a result string based on the simulator state, of course, in
    // actual code the result should not be a string but a value object
    // containing the values of interest.
    final StringBuilder sb = new StringBuilder();
    if (vehicles.isEmpty()) {
      sb.append("No vehicles were added");
    } else {
      sb.append(vehicles.size()).append(" vehicles were added");
    }

    if (sim.getCurrentTime() >= args.getScenario().getTimeWindow().end()) {
      sb.append(", simulation has completed.");
    } else {
      sb.append(", simulation was stopped prematurely.");
    }
    return sb.toString();
  }

  @Override
  public FailureStrategy handleFailure(Exception e, Simulator sim,
      SimArgs args) {
    // Signal that when an exception occurs the entire experiment should be
    // aborted.
    return FailureStrategy.ABORT_EXPERIMENT_RUN;
  }
}
