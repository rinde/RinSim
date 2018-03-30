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
package com.github.rinde.rinsim.central.rt;

/**
 * Objects implementing this interface advertise that they want to use a
 * {@link RtSimSolver}. This interface allows injection of a
 * {@link RtSimSolverBuilder} to obtain an instance of {@link RtSimSolver}. In
 * order for this to work the {@link RtSolverModel} needs to be present in the
 * simulator.
 * @author Rinde van Lon
 */
public interface RtSolverUser {

  /**
   * Injection of the {@link RtSimSolverBuilder}. Will be called at time of
   * object registration in the simulator, it is called at maximum once. The
   * simulator needs to be configured with an instance of {@link RtSolverModel}
   * in order for this method to work.
   * @param builder An instance of {@link RtSimSolverBuilder} that allows to
   *          construct a {@link RtSimSolver} instance.
   */
  void setSolverProvider(RtSimSolverBuilder builder);
}
