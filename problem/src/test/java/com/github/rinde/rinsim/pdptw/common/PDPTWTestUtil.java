/**
 * 
 */
package com.github.rinde.rinsim.pdptw.common;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPObject;
import com.github.rinde.rinsim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon 
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
