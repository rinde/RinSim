/**
 * 
 */
package rinde.sim.pdptw.generator;

import static java.util.Collections.nCopies;

import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.VehicleDTO;

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

  @Override
  public ImmutableList<AddVehicleEvent> generate(RandomGenerator rng) {
    return ImmutableList
        .copyOf(nCopies(n, new AddVehicleEvent(-1, vehicleDto)));
  }

  @Override
  public Unit<Velocity> getSpeedUnit() {
    throw new UnsupportedOperationException();
  }
}
