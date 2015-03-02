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
