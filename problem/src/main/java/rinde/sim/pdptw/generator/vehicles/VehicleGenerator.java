/**
 * 
 */
package rinde.sim.pdptw.generator.vehicles;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.pdptw.common.AddVehicleEvent;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface VehicleGenerator {

  ImmutableList<AddVehicleEvent> generate(RandomGenerator rng);

}
