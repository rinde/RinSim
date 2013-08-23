package rinde.sim.serializers;

import rinde.sim.core.graph.Point;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 * @param <T> describes edge related data
 */
public interface SerializerFilter<T> {
  /**
   * Ignore a given edge during serialization or deserialization
   * @param from starting point
   * @param to end point
   * @return <code>true</code> when the connection should be ignored
   */
  boolean filterOut(Point from, Point to);

  /**
   * Ignore a given edge during serialization or deserialization
   * @param from starting point
   * @param to end point
   * @param
   * @return <code>true</code> when the connection should be ignored
   */
  boolean filterOut(Point from, Point to, T data);
}
