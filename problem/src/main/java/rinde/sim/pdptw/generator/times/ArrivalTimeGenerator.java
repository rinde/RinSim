package rinde.sim.pdptw.generator.times;

import com.google.common.collect.ImmutableList;

/**
 * Generator of event arrival times.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
// TODO rename to TimeSeriesGenerator?
public interface ArrivalTimeGenerator {
  /**
   * Should generate a list of arrival times.
   * @param seed The random seed to use.
   * @return An immutable list of arrival times in ascending order, may contain
   *         duplicates.
   */
  ImmutableList<Double> generate(long seed);
}
