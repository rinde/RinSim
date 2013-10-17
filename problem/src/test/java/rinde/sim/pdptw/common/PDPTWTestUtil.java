/**
 * 
 */
package rinde.sim.pdptw.common;

import rinde.sim.core.Simulator;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPObject;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class PDPTWTestUtil {

  /**
   * Registers the objects to the two supplied models.
   * @param rm The road model.
   * @param pm The pdp model.
   * @param objs The objects to register.
   */
  public static void register(RoadModel rm, PDPModel pm, PDPObject... objs) {
    for (final PDPObject obj : objs) {
      rm.register(obj);
      pm.register(obj);
    }
  }

  /**
   * Shorthand for registering objects in the simulator.
   * @param sim Sim instance.
   * @param objs The objects to register.
   */
  public static void register(Simulator sim, Object... objs) {
    for (final Object o : objs) {
      sim.register(o);
    }
  }

}
