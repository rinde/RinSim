package rinde.sim.pdptw.generator.times;

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.collect.ImmutableList;

/**
 * Generator of event arrival times.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface ArrivalTimesGenerator {
  /**
   * Should generate a list of arrival times.
   * @param rng The random generator to use.
   * @return An immutable list of arrival times in ascending order, may contain
   *         duplicates.
   */
  ImmutableList<Double> generate(RandomGenerator rng);
}
