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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * The solver model facilitates the use of {@link Solver}s from within agents.
 * By implementing {@link SolverUser} an agent can obtain an
 * {@link SimSolver} instance.
 * <p>
 * <b>Model properties</b>
 * <ul>
 * <li><i>Associated type:</i> {@link SolverUser}.</li>
 * <li><i>Provides:</i> {@link SimulationSolverBuilder}.</li>
 * <li><i>Dependencies:</i> {@link Clock}, {@link PDPRoadModel},
 * {@link PDPModel}.</li>
 * </ul>
 * @author Rinde van Lon
 */
public class SolverModel extends AbstractModel<SolverUser> {

  private final Clock clock;
  private final PDPRoadModel roadModel;
  private final PDPModel pdpModel;

  SolverModel(Clock c, PDPRoadModel rm, PDPModel pm) {
    clock = c;
    roadModel = rm;
    pdpModel = pm;
  }

  @Override
  public boolean register(SolverUser element) {
    element.setSolverProvider(new Provider());
    return true;
  }

  @Override
  public boolean unregister(SolverUser element) {
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <U> U get(Class<U> clazz) {
    checkArgument(clazz == SimulationSolverBuilder.class);
    return (U) new Provider();
  }

  /**
   * @return A new {@link Builder} instance.
   */
  public static Builder builder() {
    return Builder.create();
  }

  class Provider extends SimulationSolverBuilder {
    ImmutableList<Vehicle> vehiclesList;

    Provider() {
      vehiclesList = ImmutableList.of();
    }

    @Override
    public SimulationSolverBuilder setVehicle(Vehicle v) {
      vehiclesList = ImmutableList.of(v);
      return this;
    }

    @Override
    public SimulationSolverBuilder setVehicles(
      Iterable<? extends Vehicle> vehicles) {
      vehiclesList = ImmutableList.copyOf(vehicles);
      return this;
    }

    @Override
    public SimSolver build(Solver s) {
      return new SimSolver(Optional.of(s), roadModel, pdpModel, clock,
        vehiclesList);
    }
  }

  /**
   * Builder for {@link SolverModel}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder extends
    AbstractModelBuilder<SolverModel, SolverUser> implements Serializable {

    private static final long serialVersionUID = -4369279105715776943L;

    Builder() {
      setDependencies(Clock.class, PDPRoadModel.class, PDPModel.class);
      setProvidingTypes(SimulationSolverBuilder.class);
    }

    @Override
    public SolverModel build(DependencyProvider dependencyProvider) {
      Clock c = dependencyProvider.get(Clock.class);
      PDPRoadModel rm = dependencyProvider.get(PDPRoadModel.class);
      PDPModel pm = dependencyProvider.get(PDPModel.class);
      return new SolverModel(c, rm, pm);
    }

    static Builder create() {
      return new AutoValue_SolverModel_Builder();
    }
  }
}
