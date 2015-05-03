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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

/**
 * Model manager is a utility class that manages {@link Model}s. It has two main
 * responsibilities:
 * <ul>
 * <li>Resolving inter-model dependencies, this is done at construction time via
 * {@link #builder()}.</li>
 * <li>Registering and unregistering objects to models, via
 * {@link #register(Object)} and {@link #unregister(Object)}.</li>
 * </ul>
 *
 * @author Bartosz Michalik
 * @author Rinde van Lon
 */
public final class ModelManager implements ModelProvider {
  private final ImmutableSet<Model<?>> models;
  private final ImmutableSetMultimap<Class<?>, Model<?>> registry;
  private final Optional<UserInterface> userInterface;

  /**
   * Instantiate a new model manager.
   * @param ms
   */
  @SuppressWarnings("unchecked")
  ModelManager(ImmutableSet<? extends Model<?>> ms) {
    models = (ImmutableSet<Model<?>>) ms;
    final ImmutableSetMultimap.Builder<Class<?>, Model<?>> builder =
      ImmutableSetMultimap.builder();
    builder.put(ModelReceiver.class, new ModelReceiverModel(this));

    final Set<UserInterface> uis = new LinkedHashSet<>();
    for (final Model<?> m : models) {
      final Class<?> suptype = m.getSupportedType();
      checkNotNull(suptype,
        "Model.getSupportedType() must return a non-null, model: %s.", m);
      builder.put(suptype, m);
      if (m instanceof UserInterface) {
        uis.add((UserInterface) m);
      }
    }

    checkState(uis.size() <= 1,
      "At most one implementor of %s can be defined, found %s.",
      UserInterface.class, uis);
    if (uis.isEmpty()) {
      userInterface = Optional.absent();
    } else {
      userInterface = Optional.of(uis.iterator().next());
    }

    registry = builder.build();
    for (final Model<?> m : models) {
      doRegister(m);
    }
  }

  @SuppressWarnings("unchecked")
  <T> boolean doRegister(T object) {
    boolean success = false;
    final Set<Class<?>> modelSupportedTypes = registry.keySet();
    for (final Class<?> modelSupportedType : modelSupportedTypes) {
      if (modelSupportedType.isAssignableFrom(object.getClass())) {
        final Collection<Model<?>> assignableModels = registry
          .get(modelSupportedType);
        for (final Model<?> m : assignableModels) {
          success |= ((Model<T>) m).register(object);
        }
      }
    }
    return success;
  }

  /**
   * Registers the given object.
   * @param object The object to register.
   * @throws IllegalArgumentException if an instance of {@link Model} is
   *           provided or if the model could not be registered to any model.
   */
  public <T> void register(T object) {
    checkArgument(
      !(object instanceof Model<?>),
      "Can not register a model: %s. "
        + "Models can be added via Simulator.builder().",
      object);
    final boolean success = doRegister(object);
    checkArgument(
      success,
      "The object %s with type %s can not be registered to any model.",
      object, object.getClass());
  }

  /**
   * Unregisters the given object.
   * @param object The object to unregister.
   * @throws IllegalArgumentException if an instance of {@link Model} is
   *           provided or if the model could not be unregistered from any
   *           model.
   */
  @SuppressWarnings("unchecked")
  public <T> void unregister(T object) {
    checkArgument(!(object instanceof Model), "can not unregister a model");
    boolean result = false;
    final Set<Class<?>> modelSupportedTypes = registry.keySet();
    for (final Class<?> modelSupportedType : modelSupportedTypes) {
      // check if object is from a known type
      if (modelSupportedType.isAssignableFrom(object.getClass())) {
        final Collection<Model<?>> assignableModels = registry
          .get(modelSupportedType);
        for (final Model<?> m : assignableModels) {
          result |= ((Model<T>) m).unregister(object);
        }
      }
    }
    checkArgument(result, "Object %s with type %s can not be unregistered.",
      object, object.getClass());
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public <T extends Model<?>> T tryGetModel(Class<T> clazz) {
    for (final Model<?> model : registry.values()) {
      if (clazz.isInstance(model)) {
        return (T) model;
      }
    }
    throw new IllegalArgumentException("There is no model of type: " + clazz);
  }

  @Override
  public <T extends Model<?>> T getModel(Class<T> clazz) {
    final T m = tryGetModel(clazz);
    checkArgument(m != null, "The specified model %s does not exist.", clazz);
    return m;
  }

  /**
   * @return The {@link Model}s that are registered.
   */
  public ImmutableSet<Model<?>> getModels() {
    return models;
  }

  public Optional<UserInterface> getUserInterface() {
    return userInterface;
  }

  /**
   * @return A new {@link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for constructing {@link ModelManager} instances.
   * @author Rinde van Lon
   */
  public static final class Builder {
    private final DependencyResolver resolver;

    Builder() {
      resolver = new DependencyResolver();
    }

    /**
     * Adds the specified {@link ModelBuilder} to the manager. The
     * {@link ModelBuilder} will be used to obtain a {@link Model} instance.
     * @param builder The builder to add.
     * @return This, as per the builder pattern.
     */
    public Builder add(ModelBuilder<?, ?> builder) {
      if (builder instanceof CompositeModelBuilder<?, ?>) {
        return add((CompositeModelBuilder<?, ?>) builder);
      }
      doAdd(builder);
      return this;
    }

    /**
     * Adds the specified {@link CompositeModelBuilder} and its children to the
     * manager.
     * @param builder The builder to add.
     * @return This, as per the builder pattern.
     */
    public Builder add(CompositeModelBuilder<?, ?> builder) {
      doAdd(builder);
      for (final ModelBuilder<?, ?> mb : builder.getChildren()) {
        add(mb);
      }
      return this;
    }

    void doAdd(ModelBuilder<?, ?> b) {
      resolver.add(b);
    }

    /**
     * Adds the specified {@link ModelBuilder} to the manager as a default
     * provider. A default provider will only be used if there is no regular
     * {@link ModelBuilder} (added via {@link #add(ModelBuilder)}) that provides
     * the same types.
     * @param provider The builder to add.
     * @return This, as per the builder pattern.
     */
    public Builder addDefaultProvider(ModelBuilder<?, ?> provider) {
      if (provider instanceof CompositeModelBuilder<?, ?>) {
        return addDefaultProvider((CompositeModelBuilder<?, ?>) provider);
      }
      resolver.addDefault(provider);
      return this;
    }

    /**
     * Adds the specified {@link CompositeModelBuilder} and its children to the
     * manager as a default provider.
     * @param provider The provider to add.
     * @return This, as per the builder pattern.
     */
    public Builder addDefaultProvider(CompositeModelBuilder<?, ?> provider) {
      resolver.addDefault(provider);
      for (final ModelBuilder<?, ?> mb : provider.getChildren()) {
        addDefaultProvider(mb);
      }
      return this;
    }

    /**
     * @return A new {@link ModelManager} instance.
     */
    public ModelManager build() {
      return new ModelManager(resolver.resolve());
    }
  }

  static final class ModelReceiverModel extends AbstractModel<ModelReceiver> {
    private final ModelManager modelManager;

    ModelReceiverModel(ModelManager mm) {
      modelManager = mm;
    }

    @Override
    public boolean register(ModelReceiver element) {
      element.registerModelProvider(modelManager);
      return true;
    }

    @Override
    public boolean unregister(ModelReceiver element) {
      return false;
    }
  }
}
