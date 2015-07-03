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

import java.util.List;

import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.RealTimeClockController;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon
 *
 */
public abstract class RtSimSolver {

  RtSimSolver() {}

  //
  // API towards solver users (e.g. agents or models)
  //

  // switch back to real time
  // start computation
  public abstract void solve(SolveArgs args);

  public abstract boolean hasChanged();

  public abstract List<List<Parcel>> getCurrentSchedule();

  static class RtSimSolverImpl extends RtSimSolver implements Scheduler {

    RtSimSolverImpl(RealTimeClockController clock, RealtimeSolver solver,
      PDPRoadModel rm, PDPModel pm) {

    }

    @Override
    public void solve(SolveArgs args) {
      // TODO Auto-generated method stub

    }

    @Override
    public boolean hasChanged() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public List<List<Parcel>> getCurrentSchedule() {
      // TODO Auto-generated method stub
      return null;
    }

    //
    // API towards RealtimeSolver
    //

    @Override
    public void updateSchedule(
      ImmutableList<ImmutableList<? extends Parcel>> routes) {
      // TODO Auto-generated method stub

    }

    @Override
    public void doneForNow() {
      // TODO Auto-generated method stub
    }

  }
}
