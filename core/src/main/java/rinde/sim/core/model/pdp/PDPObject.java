/**
 * 
 */
package rinde.sim.core.model.pdp;

import rinde.sim.core.model.road.RoadUser;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
interface PDPObject extends RoadUser {

    PDPType getType();

    void initPDPObject(PDPModel model);

}
