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

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.collect.ImmutableList;

/**
 * Interface for solvers of the pickup-and-delivery problem with time windows
 * (PDPTW).
 * @author Rinde van Lon
 */
public interface Solver {

  /**
   * Computes a solution for the PDPTW as specified by the
   * {@link GlobalStateObject}. The returned solution does not necessarily need
   * to be optimal but it needs to be feasible. The {@link SolverValidator} can
   * check whether a {@link Solver} produces a valid solution and it can check
   * whether the input parameters of the {@link Solver} are valid.
   * @param state The state of the world, or problem instance.
   * @return A list of routes, one for every vehicle in the
   *         {@link GlobalStateObject}.
   */
  ImmutableList<ImmutableList<Parcel>> solve(GlobalStateObject state);
}
