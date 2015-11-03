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

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * A scheduler is a facade that allows controlling the routes of one or more
 * {@link com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle vehicles}.
 * Instances are meant to be used by implementations of {@link RealtimeSolver}.
 * @author Rinde van Lon
 */
public abstract class Scheduler {

  Scheduler() {}

  /**
   * Updates the schedule of the vehicles in the next tick.
   * @param routes The new schedule, one (potentially empty) list per vehicle.
   */
  public abstract void updateSchedule(
      ImmutableList<ImmutableList<Parcel>> routes);

  /**
   * Retrieves the schedule previously set via
   * {@link #updateSchedule(ImmutableList)}.
   * @return The previously set schedule.
   * @throws IllegalStateException If no schedule has been set.
   */
  public abstract ImmutableList<ImmutableList<Parcel>> getCurrentSchedule();

  /**
   * Tells the scheduler that no computations are currently done. This will
   * switch the simulator clock into simulated time mode in the next tick. The
   * clock will go back to real-time as soon as the problem changes again (as
   * notified via
   * {@link RealtimeSolver#problemChanged(com.github.rinde.rinsim.central.GlobalStateObject)}
   * . Calling this method can greatly increase the speed of a simulation
   * because real-time is only used when it is needed, and the simulation is
   * fast forwarded when it is not needed.
   */
  public abstract void doneForNow();

  /**
   * This method provides access to the shared listening executor service
   * (thread pool) that is used by {@link RtSolverModel}. Since this executor
   * service is shared, there are several rules that {@link RtSimSolver}s should
   * adhere to:
   * <ul>
   * <li>Each {@link RtSimSolver} may have at most one job computing at the same
   * time. If that is not enough, you are allowed to spawn your own threads
   * outside of the shared executor.</li>
   * <li>{@link ListeningExecutorService#shutdown()} and
   * {@link ListeningExecutorService#shutdownNow()} may not be called.</li>
   * </ul>
   * @return The executor service.
   */
  public abstract ListeningExecutorService getSharedExecutor();

  /**
   * Allows an orderly shutdown of the simulator whenever an exception occurred
   * in a different thread owned by a {@link RealtimeSolver}. By default an
   * exception thrown in a separate thread only crashes that particular thread,
   * to improve error diagnosis this method should be used to stop the
   * simulation and rethrow the exception.
   * @param t The exception that occurred.
   */
  public abstract void reportException(Throwable t);
}
