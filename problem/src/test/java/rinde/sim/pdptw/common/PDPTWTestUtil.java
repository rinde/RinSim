/**
 * 
 */
package rinde.sim.pdptw.common;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPObject;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class PDPTWTestUtil {

  /**
   * Registers the objects to the models.
   * @param rm
   * @param pm
   * @param objs
   */
  public static void register(RoadModel rm, PDPModel pm, PDPObject... objs) {
    for (final PDPObject obj : objs) {
      rm.register(obj);
      pm.register(obj);
      // if (obj instanceof DefaultVehicle) {
      // final DefaultVehicle dv = (DefaultVehicle) obj;
      // // rm.addObjectAt(dv, dv.getDTO().startPosition);
      // pm.register(dv);
      // } else {
      // final DefaultParcel dp = (DefaultParcel) obj;
      // // rm.addObjectAt(dp, dp.dto.pickupLocation);
      // pm.register(dp);
      // }
    }
  }

}
