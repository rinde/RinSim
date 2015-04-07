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

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;

/**
 * Interface for builders of {@link Model}s. An implementation of the
 * {@link ModelBuilder} interface can specify three different properties of the
 * {@link Model} its constructing:
 * <ul>
 * <li>Associated type, as advertised via {@link #getAssociatedType()}</li>
 * <li>Provided types, as advertised via {@link #getProvidingTypes()}</li>
 * <li>Dependent types, as advertised via {@link #getDependencies()}</li>
 * </ul>
 * <p>
 * <b>Associated type</b> Objects (not models) that are added to the simulator
 * with a type that is assignable to the associated type are automatically
 * registered to the model.
 * <p>
 * <b>Provided types</b> These are the types that the model that is constructed
 * by this builder provides to the simulator. Instances of these types should be
 * provided via {@link Model#get(Class)}.
 * <p>
 * <b>Dependent types</b> These are the types that the model that is constructed
 * by this builder depends on. Instances of this type can be requested via a
 * {@link DependencyProvider} that is made available when
 * {@link #build(DependencyProvider)} is called.
 *
 * @param <T> The associated type.
 * @author Rinde van Lon
 * @see AbstractModelBuilder
 */
public interface ModelBuilder<T> {

  /**
   * @return The type parameter of the {@link Model} that is constructed by this
   *         builder.
   */
  Class<T> getAssociatedType();

  /**
   * @return A set of types that are provided by the {@link Model}.
   */
  ImmutableSet<Class<?>> getProvidingTypes();

  /**
   * @return A set of types that are dependencies of this {@link Model}.
   */
  ImmutableSet<Class<?>> getDependencies();

  /**
   * Should build the model. The {@link DependencyProvider} allows to request
   * instances with any of the types specified by {@link #getDependencies()}.
   * Each declared dependency <b>must</b> be requested of the
   * {@link DependencyProvider}. References to the {@link DependencyProvider}
   * should not be kept, the provider is <i>guaranteed</i> to be unusable after
   * this method has been invoked.
   * @param dependencyProvider The dependency provider.
   * @return A new {@link Model} instance.
   */
  Model<T> build(DependencyProvider dependencyProvider);

  /**
   * Abstract implementation of {@link ModelBuilder} that provides default
   * implementations all methods except {@link #build(DependencyProvider)}.
   * @author Rinde van Lon
   * @param <T> The associated type.
   */
  public abstract class AbstractModelBuilder<T> implements ModelBuilder<T> {
    private final ImmutableSet<Class<?>> provTypes;
    private final Class<T> clazz;

    /**
     * Construct a new instance.
     * @param providingTypes The providing types of this builder, see
     *          {@link ModelBuilder} header comment for more info.
     */
    @SuppressWarnings({ "unchecked", "serial" })
    protected AbstractModelBuilder(Class<?>... providingTypes) {
      provTypes = ImmutableSet.copyOf(providingTypes);
      clazz = (Class<T>) new TypeToken<T>(getClass()) {}.getRawType();
    }

    @Override
    public final Class<T> getAssociatedType() {
      return clazz;
    }

    @Override
    public final ImmutableSet<Class<?>> getProvidingTypes() {
      return provTypes;
    }

    @Override
    public ImmutableSet<Class<?>> getDependencies() {
      return ImmutableSet.of();
    }
  }
}
