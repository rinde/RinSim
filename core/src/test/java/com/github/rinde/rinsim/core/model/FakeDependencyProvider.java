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
package com.github.rinde.rinsim.core.model;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;

/**
 * @author Rinde van Lon
 *
 */
public class FakeDependencyProvider extends DependencyProvider {
  final Map<Class<?>, Model<?>> map;
  final ClassToInstanceMap<Object> classMap;

  FakeDependencyProvider(Map<Class<?>, Model<?>> m,
      ClassToInstanceMap<Object> cm) {
    map = m;
    classMap = cm;
  }

  @Override
  public <T> T get(Class<T> type) {
    assertThat(classMap.containsKey(type) || map.containsKey(type)).isTrue();
    if (classMap.containsKey(type)) {
      return classMap.getInstance(type);
    }
    return map.get(type).get(type);
  }

  public static DependencyProvider empty() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    ImmutableMap.Builder<Class<?>, Model<?>> modelMapBuilder;
    ImmutableClassToInstanceMap.Builder<Object> classMapBuilder;

    Builder() {
      modelMapBuilder = ImmutableMap.builder();
      classMapBuilder = ImmutableClassToInstanceMap.builder();
    }

    public Builder add(Model<?> model, Class<?> providedType,
        Class<?>... moreProvidedTypes) {
      modelMapBuilder.put(providedType, model);
      for (final Class<?> clz : moreProvidedTypes) {
        modelMapBuilder.put(clz, model);
      }
      return this;
    }

    public Builder add(Model<?> model, Iterable<Class<?>> moreProvidedTypes) {
      for (final Class<?> clz : moreProvidedTypes) {
        modelMapBuilder.put(clz, model);
      }
      return this;
    }

    public Builder add(ModelBuilder<?, ?> mb) {
      return add(mb.build(mock(DependencyProvider.class)),
        mb.getProvidingTypes());
    }

    public <U> Builder add(U instance, Class<U> type) {
      classMapBuilder.put(type, instance);
      return this;
    }

    public DependencyProvider build() {
      return new FakeDependencyProvider(
        modelMapBuilder.build(),
        classMapBuilder.build());
    }
  }

}
