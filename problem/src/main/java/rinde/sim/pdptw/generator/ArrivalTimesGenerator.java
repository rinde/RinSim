package rinde.sim.pdptw.generator;

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.collect.ImmutableList;

public interface ArrivalTimesGenerator {
  /**
   * 
   * @param rng
   * @return An immutable list of arrival times in ascending order, may contain
   *         duplicates.
   */
  ImmutableList<Double> generate(RandomGenerator rng);
}
