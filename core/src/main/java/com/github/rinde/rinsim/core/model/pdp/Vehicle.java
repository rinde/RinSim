/**
 * 
 */
package com.github.rinde.rinsim.core.model.pdp;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;

/**
 * Abstract base class for vehicle concept: moving {@link Container}.
 * @author Rinde van Lon 
 */
public abstract class Vehicle extends ContainerImpl implements MovingRoadUser,
    TickListener {

  @Override
  public final PDPType getType() {
    return PDPType.VEHICLE;
  }

  @Override
  public final void tick(TimeLapse time) {
    // finish previously started pickup and delivery actions that need to
    // consume time
    getPDPModel().continuePreviousActions(this, time);
    tickImpl(time);
  }

  /**
   * Is called every tick. This replaces the
   * {@link TickListener#tick(TimeLapse)} for vehicles.
   * @param time The time lapse that can be used.
   * @see TickListener#tick(TimeLapse)
   */
  protected abstract void tickImpl(TimeLapse time);

  @Override
  public void afterTick(TimeLapse time) {}
}
