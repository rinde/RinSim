package com.github.rinde.rinsim.core.model;

import javax.annotation.Nullable;

/**
 * Implementations of this interface may provide access to any {@link Model}s it
 * knows.
 * @author Rinde van Lon 
 */
public interface ModelProvider {

  /**
   * @param clazz The type of {@link Model}.
   * @param <T> The type of model.
   * @return A {@link Model} instance of the specified type if it knows about
   *         it, <code>null</code> otherwise.
   */
  @Nullable
  <T extends Model<?>> T getModel(Class<T> clazz);

}
