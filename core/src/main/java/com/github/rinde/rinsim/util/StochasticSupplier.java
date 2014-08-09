package com.github.rinde.rinsim.util;

/**
 * Factory class that can supply values based on a random seed.
 * @param <T> The type of objects to supply.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface StochasticSupplier<T> {

  /**
   * This method may or may not create new instances.
   * @param seed The random seed to use.
   * @return An object of the appropriate type.
   */
  T get(long seed);
}
