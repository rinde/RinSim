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

import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.collect.ImmutableList;

/**
 * {@link RtSimSolver} is a usage-friendly facade for {@link RealtimeSolver}s.
 * It provides a convenient API to use a {@link RealtimeSolver} inside a
 * simulation.
 * <p>
 * Instances of {@link RtSimSolver} can only be constructed using
 * {@link RtSimSolverBuilder}, which can be obtained via {@link RtSolverModel}.
 * Note that it is not possible to construct your own subclass of
 * {@link RtSimSolver}.
 * @author Rinde van Lon
 */
public abstract class RtSimSolver {

  RtSimSolver() {}

  // switch back to real time
  // start computation
  public abstract void solve(SolveArgs args);

  public abstract boolean isScheduleUpdated();

  public abstract ImmutableList<ImmutableList<Parcel>> getCurrentSchedule();

}
