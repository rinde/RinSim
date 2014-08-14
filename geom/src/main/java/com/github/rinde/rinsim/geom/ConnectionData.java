package com.github.rinde.rinsim.geom;

/**
 * Simple interface to represent data associated to a {@link Connection} in a
 * {@link Graph}.
 * @author Bartosz Michalik 
 * @author Rinde van Lon 
 * @since 2.0
 */
public interface ConnectionData {

  /**
   * This method can be implemented to override the default length (euclidean
   * distance).
   * @return The length of the {@link Connection}.
   */
  double getLength();
}
