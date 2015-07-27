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
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.google.common.collect.ImmutableList;

/**
 * Solver decorator that sleeps before calling the delegate solver. Handy for
 * testing 'long' computations.
 * @author Rinde van Lon
 */
public class SleepySolver implements Solver {
  final Solver delegate;
  final long sleepTime;

  SleepySolver(long sleep, Solver s) {
    delegate = s;
    sleepTime = sleep;
  }

  @Override
  public ImmutableList<ImmutableList<Parcel>> solve(GlobalStateObject state) {
    try {
      Thread.sleep(sleepTime);
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }
    return delegate.solve(state);
  }

  /**
   * Create a new instance.
   * @param sleepTime The time to sleep each time
   *          {@link Solver#solve(GlobalStateObject)} is called.
   * @param delegate The {@link Solver} to delegate the actual solving to.
   * @return A new instance.
   */
  public static Solver create(long sleepTime, Solver delegate) {
    return new SleepySolver(sleepTime, delegate);
  }
}
