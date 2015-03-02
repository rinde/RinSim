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

import com.google.common.reflect.TypeToken;

/**
 * Basic implementation that provides a getSupportedType method implementation.
 * @author Bartosz Michalik 
 * 
 * @param <T> The type that is supported by this model.
 */
public abstract class AbstractModel<T> implements Model<T> {

  private final Class<T> clazz;

  /**
   * Create a new model.
   */
  @SuppressWarnings({ "serial", "unchecked" })
  protected AbstractModel() {
    this.clazz = (Class<T>) new TypeToken<T>(getClass()) {}.getRawType();
  }

  @Override
  public final Class<T> getSupportedType() {
    return clazz;
  }

}
