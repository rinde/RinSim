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

import static com.google.common.collect.Lists.asList;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

/**
 * @author Rinde van Lon
 * @param <T>
 *
 */
public interface ModelBuilder<T> {

  Class<T> getAssociatedType();

  Model<T> build(DependencyProvider dependencyProvider);

  ImmutableSet<Class<?>> getProvidingTypes();

  ImmutableSet<Class<?>> getDependencies();

  public static abstract class AbstractModelBuilder<T> implements
    ModelBuilder<T> {

    private final ImmutableSet<Class<?>> providingTypes;
    private final Class<T> clazz;

    @SuppressWarnings({ "unchecked", "serial" })
    protected AbstractModelBuilder(Class<?> type, Class<?>... moreTypes) {
      providingTypes = ImmutableSet.copyOf(asList(type, moreTypes));
      clazz = (Class<T>) new TypeToken<T>(getClass()) {}.getRawType();
    }

    @Override
    public final Class<T> getAssociatedType() {
      return clazz;
    }

    @Override
    public final ImmutableSet<Class<?>> getProvidingTypes() {
      return providingTypes;
    }

    @Override
    public ImmutableSet<Class<?>> getDependencies() {
      return ImmutableSet.of();
    }
  }
}
