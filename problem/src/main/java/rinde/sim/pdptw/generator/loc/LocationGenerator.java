/**
 * 
 */
package rinde.sim.pdptw.generator.loc;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;

import com.google.common.collect.ImmutableList;

/**
 * A location generator generates locations for orders (aka tasks).
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface LocationGenerator {
  /**
   * Should generate locations for the specified number of orders (aka tasks).
   * There should be enough locations for each order. Typically this is
   * predefined as a ratio, e.g. <code>1:2</code> in case origin and destination
   * is required for each order.
   * @param numOrders The number of orders for which a location is required.
   * @param rng The {@link RandomGenerator} which should be used for drawing
   *          random numbers.
   * @return A list of locations for the orders.
   */
  ImmutableList<Point> generate(int numOrders, RandomGenerator rng);

  Unit<Length> getDistanceUnit();
}
