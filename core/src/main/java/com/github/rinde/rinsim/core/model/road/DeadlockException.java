/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
package com.github.rinde.rinsim.core.model.road;

import com.github.rinde.rinsim.geom.Connection;

/**
 * A special {@link IllegalArgumentException} indicating that a deadlock
 * situation has been detected. If this exception would not have been thrown the
 * {@link CollisionGraphRoadModel} would have been in a deadlock state. This
 * exception can be caught in order to reroute the agent.
 * @author Rinde van Lon
 */
public final class DeadlockException extends IllegalArgumentException {
  private static final long serialVersionUID = 5935544267152959099L;
  private final Connection<?> connection;

  DeadlockException(Connection<?> conn) {
    super(
        "There is a vehicle driving in the opposite direction on the target connection.");
    connection = conn;
  }

  /**
   * @return The connection the vehicle attempted to enter when the deadlock was
   *         detected.
   */
  public Connection<?> getConnection() {
    return connection;
  }
}
