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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.util.LinkedHashBiMap;
import com.google.common.base.Supplier;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;

class DependencyResolver implements DependencyProvider {
  final Map<Class<?>, Dependency> providerMap;
  final Multimap<Dependency, Class<?>> dependencyMap;
  final BiMap<Class<?>, Dependency> modelTypeMap;
  final List<Dependency> builders;

  DependencyResolver(List<ModelBuilder<?>> models, Simulator.Builder simBui) {
    providerMap = new LinkedHashMap<>();
    dependencyMap = LinkedHashMultimap.create();
    modelTypeMap = LinkedHashBiMap.create();
    builders = new ArrayList<>();

    for (final ModelBuilder<?> o : models) {
      final Dependency mb = new Dependency(this, o);
      modelTypeMap.put(o.getAssociatedType(), mb);

      for (final Class<?> clz : o.getProvidingTypes()) {
        checkArgument(!providerMap.containsKey(clz),
          "A provider for %s already exists: %s.", clz, providerMap.get(clz));
        providerMap.put(clz, mb);
      }
      dependencyMap.putAll(mb, o.getDependencies());
      builders.add(mb);
    }
    if (!providerMap.containsKey(RandomProvider.class)) {
      final Dependency mb = new Dependency(this,
        RandomModel.builder(simBui.rng));
      providerMap.put(RandomProvider.class, mb);
      builders.add(mb);
    }
    if (!modelTypeMap.containsKey(TickListener.class)) {
      final Dependency mb = new Dependency(this, adapt(TimeModel.supplier(
        simBui.tickLength, simBui.timeUnit)));
      builders.add(mb);
    }
  }

  ImmutableSet<Model<?>> resolve() {
    final Multimap<Dependency, Dependency> dependencyGraph = LinkedHashMultimap
      .create();
    for (final Entry<Dependency, Class<?>> entry : dependencyMap
      .entries()) {
      checkArgument(providerMap.containsKey(entry.getValue()),
        "Could not resolve dependency for implementations of %s.",
        entry.getValue());
      dependencyGraph.put(entry.getKey(), providerMap.get(entry.getValue()));
    }
    while (!dependencyGraph.isEmpty()) {
      final List<Dependency> toRemove = new ArrayList<>();
      for (final Dependency dep : dependencyGraph.keys()) {
        final Collection<Dependency> dependencies = dependencyGraph.get(dep);
        boolean allResolved = true;
        for (final Dependency dependency : dependencies) {
          allResolved &= dependency.isResolved();
        }

        if (allResolved) {
          dep.build();
          toRemove.add(dep);
        }
      }

      for (final Dependency mb : toRemove) {
        dependencyGraph.removeAll(mb);
      }
      if (toRemove.isEmpty()) {
        throw new IllegalArgumentException(
          "Could not resolve dependencies for " + dependencyGraph.keySet()
            + ", most likely a circular dependency was declared.");
      }
    }

    final ImmutableSet.Builder<Model<?>> builder = ImmutableSet.builder();
    for (final Dependency cmb : builders) {
      builder.add(cmb.build());
    }
    return builder.build();
  }

  @Override
  public <T> T get(Class<T> type) {
    return providerMap.get(type).build().get(type);
  }

  static class SupplierAdapter<T> implements ModelBuilder<T>, Serializable {
    private static final long serialVersionUID = 1167482652694633842L;
    final Supplier<? extends Model<T>> supplier;
    Class<T> clazz;

    @SuppressWarnings({ "serial", "unchecked" })
    SupplierAdapter(Supplier<? extends Model<T>> sup) {
      clazz = (Class<T>) new TypeToken<T>(getClass()) {}.getRawType();
      supplier = sup;
    }

    @Override
    public Model<T> build(DependencyProvider dependencyProvider) {
      return supplier.get();
    }

    @Override
    public ImmutableSet<Class<?>> getProvidingTypes() {
      return ImmutableSet.<Class<?>> of();
    }

    @Override
    public ImmutableSet<Class<?>> getDependencies() {
      return ImmutableSet.of();
    }

    @Override
    public Class<T> getAssociatedType() {
      return clazz;
    }

    @Override
    public String toString() {
      return "AdapterOf-" + supplier.toString();
    }
  }

  @SuppressWarnings("unchecked")
  static <A extends Model<B>, B> ModelBuilder<B> adaptObj(final Object sup) {
    return adapt((Supplier<A>) sup);
  }

  static <A extends Model<B>, B> ModelBuilder<B> adapt(final Supplier<A> sup) {
    return new SupplierAdapter<>(sup);
  }

  static class Dependency implements Serializable {
    private static final long serialVersionUID = -3860607311745505548L;
    private final ModelBuilder<?> modelBuilder;
    private final DependencyProvider dependencyProvider;
    @Nullable
    private Model<?> value;

    Dependency(DependencyProvider dp, ModelBuilder<?> mb) {
      modelBuilder = mb;
      dependencyProvider = dp;
      if (modelBuilder.getDependencies().isEmpty()) {
        build();
      }
    }

    public Model<?> build() {
      if (value == null) {
        value = modelBuilder.build(dependencyProvider);
        checkNotNull(value, "%s returned null where a Model was expected.",
          modelBuilder);
      }
      return verifyNotNull(value);
    }

    public boolean isResolved() {
      return value != null;
    }

    @Override
    public String toString() {
      return "Dependency-" + modelBuilder.toString();
    }
  }
}
