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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

/**
 * Model manager keeps track of all models used in the simulator. It is
 * responsible for adding a simulation object to the appropriate models
 *
 * @author Bartosz Michalik
 * @author Rinde van Lon
 */
public final class ModelManager implements ModelProvider {
  private final ImmutableMultimap<Class<?>, Model<?>> registry;

  /**
   * Instantiate a new model manager.
   * @param models
   */
  ModelManager(ImmutableSet<? extends Model<?>> models) {
    final ImmutableMultimap.Builder<Class<?>, Model<?>> builder =
      ImmutableMultimap.builder();
    builder.put(ModelReceiver.class, new ModelReceiverModel(this));

    for (final Model<?> m : models) {
      final Class<?> suptype = m.getSupportedType();
      checkNotNull(suptype,
        "Model.getSupportedType() must return a non-null, model: %s.", m);
      builder.put(suptype, m);
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

  @SuppressWarnings("unchecked")
  public <T> boolean unregister(T object) {
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
    return result;
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
  public ImmutableCollection<Model<?>> getModels() {
    return registry.values();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    final ImmutableSet.Builder<ModelBuilder<?, ?>> models;
    final Set<ModelBuilder<?, ?>> defaultModels;

    Builder() {
      models = ImmutableSet.builder();
      defaultModels = new LinkedHashSet<>();
    }

    public Builder add(Iterable<? extends ModelBuilder<?, ?>> builders) {
      models.addAll(builders);
      return this;
    }

    public Builder add(ModelBuilder<?, ?> builder) {
      models.add(builder);
      return this;
    }

    public Builder add(Supplier<? extends Model<?>> supplier) {
      models.add(DependencyResolver.adaptObj(supplier));
      return this;
    }

    public Builder setDefaultProvider(ModelBuilder<?, ?> provider) {
      checkArgument(!defaultModels.contains(provider));
      defaultModels.add(provider);
      return this;
    }

    public ModelManager build() {
      final DependencyResolver r = new DependencyResolver(models.build(),
        defaultModels);
      return new ModelManager(r.resolve());
    }
  }

  static class ModelReceiverModel extends AbstractModel<ModelReceiver> {
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
