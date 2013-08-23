/**
 * 
 */
package rinde.sim.core.model.pdp;

import static com.google.common.base.Preconditions.checkState;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;

/**
 * Default implementation of {@link PDPObject}.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class PDPObjectImpl implements PDPObject {

  private PDPModel pdpModel;
  private RoadModel roadModel;
  private Point startPosition;
  private boolean isRegistered;

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
    checkState(pdpModel == null, "PDPModel can not be registered twice!");
    pdpModel = model;

    if (roadModel != null) {
      isRegistered = true;
      initRoadPDP(roadModel, pdpModel);
    }
  }

  @Override
  public final void initRoadUser(RoadModel model) {
    checkState(roadModel == null, "RoadModel can not be registered twice!");
    roadModel = model;
    if (startPosition != null) {
      model.addObjectAt(this, startPosition);
    }
    if (pdpModel != null) {
      isRegistered = true;
      initRoadPDP(roadModel, pdpModel);
    }
  }

  /**
   * @return <code>true</code> when this object has been registered in both the
   *         {@link RoadModel} and the {@link PDPModel}.
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
    checkState(!isRegistered, "this should be called before this object is registered, preferably in the constructor");
    startPosition = p;
  }

  /**
   * When this object is registered {@link #isRegistered()}, this method returns
   * the reference to the {@link PDPModel} on which this object lives.
   * @return The @{link PDPModel} reference if this object is registered,
   *         <code> null</code> otherwise.
   */
  protected PDPModel getPDPModel() {
    return pdpModel;
  }

  /**
   * When this object is registered {@link #isRegistered()}, this method returns
   * the reference to the {@link RoadModel} associated to this object.
   * @return The {@link RoadModel} reference if this object is registered,
   *         <code>null</code> otherwise.
   */
  protected RoadModel getRoadModel() {
    return roadModel;
  }

}
