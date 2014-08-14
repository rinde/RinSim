/**
 * 
 */
package com.github.rinde.rinsim.core.model.pdp;

import static com.google.common.base.Preconditions.checkState;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * Default implementation of {@link PDPObject}.
 * 
 * @author Rinde van Lon 
 */
public abstract class PDPObjectImpl implements PDPObject {

  Optional<PDPModel> pdpModel;
  private Optional<RoadModel> roadModel;
  private Optional<Point> startPosition;
  private boolean isRegistered;

  PDPObjectImpl() {
    pdpModel = Optional.absent();
    roadModel = Optional.absent();
    startPosition = Optional.absent();
  }

  // TODO should this be mandatory (abstract) implemented?? may be remove
  // abstract keyword to make it optional. pros/cons?
  /**
   * Is called when the object has been registered in both models:
   * {@link RoadModel} and {@link PDPModel}.
   * @param pRoadModel The {@link RoadModel} on which this object is situated.
   * @param pPdpModel The {@link PDPModel} which is used for transportation.
   */
  public abstract void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel);

  @Override
  public final void initPDPObject(PDPModel model) {
    checkState(!pdpModel.isPresent(), "PDPModel can not be registered twice!");
    pdpModel = Optional.of(model);

    if (roadModel.isPresent()) {
      isRegistered = true;
      initRoadPDP(roadModel.get(), pdpModel.get());
    }
  }

  @Override
  public final void initRoadUser(RoadModel model) {
    checkState(!roadModel.isPresent(), "RoadModel can not be registered twice!");
    roadModel = Optional.of(model);
    if (startPosition.isPresent()) {
      model.addObjectAt(this, startPosition.get());
    }
    if (pdpModel.isPresent()) {
      isRegistered = true;
      initRoadPDP(roadModel.get(), pdpModel.get());
    }
  }

  /**
   * @return <code>true</code> when this object has been registered in both the
   *         {@link RoadModel} and the {@link DefaultPDPModel}.
   */
  protected final boolean isRegistered() {
    return isRegistered;
  }

  /**
   * Sets the start position of this object. When set the object is
   * automatically added to the {@link RoadModel} at the specified position.
   * @param p The start position.
   */
  protected final void setStartPosition(Point p) {
    checkState(
        !isRegistered,
        "this should be called before this object is registered, preferably in the constructor");
    startPosition = Optional.of(p);
  }

  /**
   * When this object is registered {@link #isRegistered()}, this method returns
   * the reference to the {@link DefaultPDPModel} on which this object lives.
   * @return The @{link PDPModel} reference if this object is registered.
   * @throws IllegalStateException if this object is not registered.
   */
  protected PDPModel getPDPModel() {
    return pdpModel.get();
  }

  /**
   * When this object is registered {@link #isRegistered()}, this method returns
   * the reference to the {@link RoadModel} associated to this object.
   * @return The {@link RoadModel} reference if this object is registered.
   * @throws IllegalStateException if this object is not registered.
   */
  protected RoadModel getRoadModel() {
    return roadModel.get();
  }
}
