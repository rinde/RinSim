/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.core.model.rand;

import org.apache.commons.math3.random.RandomGenerator;

/**
 * A random provider can provide at most one {@link RandomGenerator} or seed. If
 * a second instance or seed is requested a {@link IllegalStateException} will
 * be thrown.
 * @author Rinde van Lon
 */
public interface RandomProvider {

  /**
   * Generates a new random seed which can be used by the client to construct
   * their own random generator.
   * @return A new random seed.
   * @throws IllegalStateException If this method or another method has already
   *           been called.
   */
  long getSeed();

  /**
   * @return The {@link RandomGenerator} that is used as the master random
   *         generator in the application. Note that this instance is
   *         unmodifiable in the sense that the seed can not be changed.
   * @throws IllegalStateException If this method or another method has already
   *           been called.
   */
  RandomGenerator masterInstance();

  /**
   * @return A new {@link RandomGenerator} instance that uses a seed that is
   *         derived from the master random generator.
   * @throws IllegalStateException If this method or another method has already
   *           been called.
   */
  RandomGenerator newInstance();

  /**
   * Allows to share {@link RandomGenerator} instances between instances of the
   * same class. For each class exactly one {@link RandomGenerator} will be
   * created. Note that the returned instance is unmodifiable in the sense that
   * the seed can not be changed.
   * @param clazz The class to request a {@link RandomGenerator} for.
   * @return A shared {@link RandomGenerator}.
   * @throws IllegalStateException If this method or another method has already
   *           been called.
   */
  RandomGenerator sharedInstance(Class<?> clazz);
}
