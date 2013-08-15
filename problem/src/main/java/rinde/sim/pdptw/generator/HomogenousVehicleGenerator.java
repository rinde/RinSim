/**
 * 
 */
package rinde.sim.pdptw.generator;

import static java.util.Collections.nCopies;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.problem.common.AddVehicleEvent;
import rinde.sim.problem.common.VehicleDTO;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class HomogenousVehicleGenerator implements VehicleGenerator {

	private final VehicleDTO vehicleDto;
	private final int n;

	public HomogenousVehicleGenerator(int numberOfVehicles, VehicleDTO dto) {
		vehicleDto = dto;
		n = numberOfVehicles;
	}

	public ImmutableList<AddVehicleEvent> generate(RandomGenerator rng) {
		return ImmutableList.copyOf(nCopies(n, new AddVehicleEvent(-1, vehicleDto)));
	}
}
