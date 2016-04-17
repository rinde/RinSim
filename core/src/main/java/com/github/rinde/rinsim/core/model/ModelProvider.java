/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
  <T extends Model<?>> T tryGetModel(Class<T> clazz);

  /**
   * @param clazz The type of {@link Model}.
   * @param <T> The type of model.
   * @return A {@link Model} instance of the specified type.
   * @throws IllegalArgumentException if there is no known {@link Model} with
   *           the specified class.
   */
  <T extends Model<?>> T getModel(Class<T> clazz);
}
