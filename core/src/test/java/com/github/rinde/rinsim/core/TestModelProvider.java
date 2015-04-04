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
package com.github.rinde.rinsim.core;

import java.util.List;

import com.github.rinde.rinsim.core.Model;
import com.github.rinde.rinsim.core.ModelProvider;
import com.google.common.base.Optional;

/**
 * @author Rinde van Lon
 *
 */
public class TestModelProvider implements ModelProvider {

  List<? extends Model<?>> models;

  public TestModelProvider(List<? extends Model<?>> ms) {
    models = ms;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Model<?>> T tryGetModel(Class<T> clazz) {
    for (final Model<?> model : models) {
      if (clazz.isInstance(model)) {
        return (T) model;
      }
    }
    throw new IllegalArgumentException("There is no model of type: "
        + clazz);
  }

  @Override
  public <T extends Model<?>> T getModel(Class<T> clazz) {
    return Optional.fromNullable(tryGetModel(clazz)).get();
  }
}
