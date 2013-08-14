/**
 * 
 */
package rinde.sim.problem.common;

import javax.annotation.Nullable;

import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface NoDiversionRoadModel extends RoadModel {

    /**
     * Returns the destination of the specified {@link MovingRoadUser} if
     * <i>all</i> of the following holds:
     * <ul>
     * <li>The {@link MovingRoadUser} is moving to some destination.</li>
     * <li>The destination is a {@link DefaultParcel}.</li>
     * <li>The {@link MovingRoadUser} has not yet reached its destination.</li>
     * </ul>
     * Returns <code>null</code> otherwise.
     * @param obj The {@link MovingRoadUser} to check its destination for.
     * @return The parcel the road user is heading to or <code>null</code>
     *         otherwise.
     */
    @Nullable
    DefaultParcel getDestinationToParcel(MovingRoadUser obj);

}
