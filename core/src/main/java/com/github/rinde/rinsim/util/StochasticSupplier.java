/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.util;

/**
 * Factory class that can supply values based on a random seed.
 * @author Rinde van Lon
 * @param <T> The type of objects to supply.
 */
public interface StochasticSupplier<T> {

  /**
   * This method may or may not create new instances.
   * @param seed The random seed to use.
   * @return An object of the appropriate type.
   */
  T get(long seed);
}
