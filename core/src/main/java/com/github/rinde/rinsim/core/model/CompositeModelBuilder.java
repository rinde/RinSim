/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import com.google.common.collect.ImmutableList;

/**
 * An extension of {@link ModelBuilder} that allows to specify a number of
 * children {@link ModelBuilder}s.
 * @param <T> The model type.
 * @param <U> The associated type.
 * @author Rinde van Lon
 */
public interface CompositeModelBuilder<T extends Model<? extends U>, U> extends
  ModelBuilder<T, U> {

  /**
   * @return A list of {@link ModelBuilder}s.
   */
  ImmutableList<ModelBuilder<?, ?>> getChildren();
}
