package rinde.sim.core.model.road;

import static com.google.common.base.Preconditions.checkState;
import rinde.sim.core.model.AbstractModel;

/**
 * A very generic implementation of the {@link RoadModel} interface.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class GenericRoadModel extends AbstractModel<RoadUser>
    implements RoadModel {

  /**
   * Reference to the outermost decorator of this road model, or to
   * <code>this</code> if there are no decorators.
   */
  protected GenericRoadModel self = this;
  private boolean initialized = false;

  /**
   * Method which should only be called by a decorator of this instance.
   * @param rm The decorator to set as 'self'.
   */
  protected void setSelf(GenericRoadModel rm) {
    checkState(
        !initialized,
        "This road model is already initialized, it can only be decorated before objects are registered.");
    self = rm;
  }

  @Override
  public final boolean register(RoadUser object) {
    initialized = true;
    return doRegister(object);
  }

  /**
   * Actual implementation of {@link #register(RoadUser)}.
   * @param object The object to register.
   * @return <code>true</code> when registration succeeded, <code>false</code>
   *         otherwise.
   */
  protected abstract boolean doRegister(RoadUser object);

}
