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

import com.github.rinde.rinsim.central.GlobalStateObject;

/**
 * Real-time version of {@link com.github.rinde.rinsim.central.Solver Solver}. A
 * real-time solver receives a static view of the problem via
 * {@link #receiveSnapshot(GlobalStateObject)}.
 * <p>
 * A {@link RealtimeSolver} can be used to schedule all vehicles in a simulation
 * via {@link RtCentral}, or it can be used to compute a route for a single
 * vehicle, this can be done by letting the vehicle implement
 * {@link RtSolverUser}.
 * <p>
 * When a {@link RealtimeSolver} is added to the simulation
 * {@link #init(Scheduler)} is called. Via this method a {@link Scheduler} is
 * made available that can be used to update the schedule of the vehicle(s).
 * Each time the problem definition changes,
 * {@link #receiveSnapshot(GlobalStateObject)} is called with a new view of the
 * current state. Using the received {@link GlobalStateObject} a new route
 * assignment should be computed which can be set to the vehicle(s) using
 * {@link Scheduler#updateSchedule(com.google.common.collect.ImmutableList)}.
 *
 * @author Rinde van Lon
 */
public interface RealtimeSolver {

  /**
   * This method is called only once and before any calls to
   * {@link #receiveSnapshot(GlobalStateObject)}. Implementations of this method
   * should store the reference to {@link Scheduler} such that it can be used in
   * {@link #receiveSnapshot(GlobalStateObject)}.
   * @param scheduler The scheduler that allows assigning routes to vehicles.
   */
  void init(Scheduler scheduler);

  /**
   * This method is called when the problem has changed, it is called at most
   * once per simulation tick. The specified {@link GlobalStateObject} is a
   * snapshot of the current state of the world. Implementations of this method
   * should start computing a new route assignment.
   * <p>
   * The simulator calls this method from a dedicated thread. This has two
   * implications:
   * <ul>
   * <li>Calling {@link Thread#sleep(long)} does not delay the advancing of the
   * simulation time.</li>
   * <li>If the problem has changed but a previous invocation of this method is
   * still computing (and therefore blocking the thread) a new invocation of
   * this method will be delayed until the previous invocation is finished.
   * Therefore it is best to minimize blocking of this thread by keeping
   * execution time of this method as short as possible and perform heavy
   * calculations in a different thread.</li>
   * </ul>
   * @param snapshot The snapshot that describes the current view of the world.
   */
  void receiveSnapshot(GlobalStateObject snapshot);

}
