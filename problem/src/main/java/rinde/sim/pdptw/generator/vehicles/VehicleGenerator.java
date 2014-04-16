/**
 * 
 */
package rinde.sim.pdptw.generator.vehicles;

import rinde.sim.pdptw.common.AddVehicleEvent;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface VehicleGenerator {

  // supply default vehicle time window: scenario length

  ImmutableList<AddVehicleEvent> generate(long seed);

}
