/**
 * 
 */
package rinde.sim.core.model.pdp;

import static com.google.common.base.Preconditions.checkState;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class PDPObjectImpl implements PDPObject {

    private PDPModel pdpModel;
    private RoadModel roadModel;
    private Point startPosition = null;
    private boolean isRegistered = false;

    public abstract void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel);

    @Override
    public abstract PDPType getType();

    @Override
    public final void initPDPObject(PDPModel model) {
        checkState(!isRegistered, "can not be registered twice!");
        pdpModel = model;

        if (roadModel != null) {
            isRegistered = true;
            initRoadPDP(roadModel, pdpModel);
        }
    }

    @Override
    public final void initRoadUser(RoadModel model) {
        checkState(!isRegistered, "can not be registered twice!");
        roadModel = model;
        if (startPosition != null) {
            model.addObjectAt(this, startPosition);
        }
        if (pdpModel != null) {
            isRegistered = true;
            initRoadPDP(roadModel, pdpModel);
        }
    }

    protected final boolean isRegistered() {
        return isRegistered;
    }

    protected final void setStartPosition(Point p) {
        checkState(!isRegistered, "this should be called before this object is registered, preferably in the constructor");
        startPosition = p;
    }

    protected PDPModel getPDPModel() {
        return pdpModel;
    }

    protected RoadModel getRoadModel() {
        return roadModel;
    }

}
