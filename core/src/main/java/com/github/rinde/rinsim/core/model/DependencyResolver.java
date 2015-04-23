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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.util.LinkedHashBiMap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

class DependencyResolver extends DependencyProvider {
  final Map<Class<?>, Dependency> providerMap;
  final Multimap<Dependency, Class<?>> dependencyMap;
  final BiMap<Class<?>, Dependency> modelTypeMap;
  final Set<Dependency> builders;
  final Set<ModelBuilder<?, ?>> defaultModels;

  DependencyResolver() {
    providerMap = new LinkedHashMap<>();
    dependencyMap = LinkedHashMultimap.create();
    modelTypeMap = LinkedHashBiMap.create();
    builders = new LinkedHashSet<>();
    defaultModels = new LinkedHashSet<>();
  }

  void addDefault(ModelBuilder<?, ?> mb) {
    checkArgument(!defaultModels.contains(mb));
    defaultModels.add(mb);
  }

  void add(ModelBuilder<?, ?> mb) {
    final ImmutableSet<Class<?>> deps = mb.getDependencies();
    final Dependency dep = new Dependency(this, mb, deps);
    modelTypeMap.put(mb.getAssociatedType(), dep);

    for (final Class<?> clz : mb.getProvidingTypes()) {
      checkArgument(!providerMap.containsKey(clz),
        "A provider for %s already exists: %s.", clz,
        providerMap.get(clz));
      providerMap.put(clz, dep);
    }
    dependencyMap.putAll(dep, deps);
    verify(!builders.contains(dep));
    builders.add(dep);
  }

  Multimap<Dependency, Dependency> constructDependencyGraph() {
    final Multimap<Dependency, Dependency> dependencyGraph = LinkedHashMultimap
      .create();
    for (final Entry<Dependency, Class<?>> entry : dependencyMap
      .entries()) {
      checkArgument(
        providerMap.containsKey(entry.getValue()),
        "Could not resolve dependency for implementations of %s, as requested "
          + "by %s.",
        entry.getValue(), entry.getKey().modelBuilder);
      dependencyGraph.put(entry.getKey(), providerMap.get(entry.getValue()));
    }
    return dependencyGraph;
  }

  void addDefaultModels() {
    for (final ModelBuilder<?, ?> b : defaultModels) {
      final ImmutableSet<Class<?>> providingTypes = b.getProvidingTypes();
      if (providingTypes.isEmpty()
        || !providerMap.keySet().containsAll(providingTypes)) {
        checkArgument(Sets.intersection(providerMap.keySet(), providingTypes)
          .isEmpty());
        add(b);
      }
    }
  }

  ImmutableSet<Model<?>> resolve() {
    addDefaultModels();
    final Multimap<Dependency, Dependency> dependencyGraph =
      constructDependencyGraph();

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

  static class DependencyProviderAccessDecorator extends DependencyProvider {
    final DependencyProvider delegate;
    final ImmutableSet<Class<?>> knownDependencies;
    final Set<Class<?>> requestedDependencies;
    final ModelBuilder<?, ?> modelBuilder;

    DependencyProviderAccessDecorator(DependencyProvider dp,
      ImmutableSet<Class<?>> allowed, ModelBuilder<?, ?> mb) {
      delegate = dp;
      knownDependencies = allowed;
      modelBuilder = mb;
      requestedDependencies = new HashSet<>();
    }

    boolean areAllDependenciesRequested() {
      return requestedDependencies.equals(knownDependencies);
    }

    Set<Class<?>> getUnusedDependencies() {
      return Sets.difference(knownDependencies, requestedDependencies);
    }

    @Override
    public <T> T get(Class<T> type) {
      checkArgument(!knownDependencies.isEmpty(),
        "%s did not declare any dependencies.", modelBuilder);
      checkArgument(
        knownDependencies.contains(type),
        "%s is not a type that %s declared as a dependency, "
          + "known dependencies: %s.",
        type, modelBuilder, knownDependencies);
      checkArgument(
        !requestedDependencies.contains(type),
        "%s is already requested by %s, each type must be requested "
          + "exactly once.",
        type, modelBuilder);
      requestedDependencies.add(type);
      return delegate.get(type);
    }
  }

  static class Dependency {
    final ModelBuilder<?, ?> modelBuilder;
    private final DependencyProviderAccessDecorator dependencyProvider;
    @Nullable
    private Model<?> value;

    Dependency(DependencyProvider dp, ModelBuilder<?, ?> mb,
      ImmutableSet<Class<?>> deps) {
      modelBuilder = mb;
      dependencyProvider = new DependencyProviderAccessDecorator(dp, deps, mb);
      if (deps.isEmpty()) {
        build();
      }
    }

    final Model<?> build() {
      if (value == null) {
        value = modelBuilder.build(dependencyProvider);
        checkNotNull(value, "%s returned null where a Model was expected.",
          modelBuilder);
        checkState(
          dependencyProvider.areAllDependenciesRequested(),
          "All declared dependencies MUST be requested from the dependency "
            + "provider, %s has unused dependencies: %s.",
          modelBuilder, dependencyProvider.getUnusedDependencies());
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
