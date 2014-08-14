package com.github.rinde.rinsim.cli;

import com.google.common.base.Optional;

/**
 * Implementations should handle the activation of an option.
 * @param <S> The type of subject this handler expects.
 * @param <V> The type of argument this handler expects.
 * @author Rinde van Lon 
 */
public interface ArgHandler<S, V> {
  /**
   * Is called when an option is activated.
   * @param subject The subject of the handler.
   * @param argument The argument that is passed to the option.
   */
  void execute(S subject, Optional<V> argument);
}
