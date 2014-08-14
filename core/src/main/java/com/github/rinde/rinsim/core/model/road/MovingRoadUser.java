package com.github.rinde.rinsim.core.model.road;

/**
 * Used to represent road users that are able to move.
 * @author Bartosz Michalik
 * @author Rinde van Lon
 * @since 2.0
 */
public interface MovingRoadUser extends RoadUser {
  /**
   * Get preferred speed of the road user. This speed is used as a maximum
   * speed, generally there is no guarantee that the object will always be
   * moving using this speed.
   * @return The preferred speed of this road user.
   */
  double getSpeed();
}
