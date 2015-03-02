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
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Models manager keeps track of all models used in the simulator. It is
 * responsible for adding a simulation object to the appropriate models
 *
 * @author Bartosz Michalik
 * @author Rinde van Lon
 */
public class ModelManager implements ModelProvider {

  private final Multimap<Class<? extends Object>, Model<? extends Object>> registry;
  private final List<Model<? extends Object>> models;
  private boolean configured;

  /**
   * Instantiate a new model manager.
   */
  public ModelManager() {
    registry = LinkedHashMultimap.create();
    models = new LinkedList<Model<? extends Object>>();
  }

  /**
   * Adds a model to the manager. Note: a model can be added only once.
   * @param model The model to be added.
   * @return true when the addition was successful, false otherwise.
   * @throws IllegalStateException when method called after calling configure
   */
  public boolean add(Model<?> model) {
    checkState(!configured, "model can not be registered after configure()");
    final Class<?> supportedType = model.getSupportedType();
    checkArgument(supportedType != null,
        "model must implement getSupportedType() and return a non-null");
    models.add(model);
    final boolean result = registry.put(supportedType, model);
    if (!result) {
      models.remove(model);
    }
    return result;
  }

  /**
   * Method that allows for initialization of the manager (e.g., resolution of
   * the dependencies between models) Should be called after all models were
   * registered in the manager.
   */
  public void configure() {
    for (final Model<?> m : models) {
      if (m instanceof ModelReceiver) {
        ((ModelReceiver) m).registerModelProvider(this);
      }
    }
    configured = true;
  }

  /**
   * Add object to all models that support a given object.
   * @param object object to register
   * @param <T> the type of object to register
   * @return <code>true</code> if object was added to at least one model
   */
  @SuppressWarnings("unchecked")
  public <T> boolean register(T object) {
    if (object instanceof Model) {
      return add((Model<?>) object);
    }
    checkState(configured,
        "can not register an object if configure() has not been called");

    boolean result = false;
    final Set<Class<?>> modelSupportedTypes = registry.keySet();
    for (final Class<?> modelSupportedType : modelSupportedTypes) {
      if (modelSupportedType.isAssignableFrom(object.getClass())) {
        final Collection<Model<?>> assignableModels = registry
            .get(modelSupportedType);
        for (final Model<?> m : assignableModels) {
          result |= ((Model<T>) m).register(object);
        }
      }
    }
    return result;
  }

  /**
   * Unregister an object from all models it was attached to.
   * @param object object to unregister
   * @param <T> the type of object to unregister
   * @return <code>true</code> when the unregistration succeeded in at least one
   *         model
   * @throws IllegalStateException if the method is called before simulator is
   *           configured
   */
  @SuppressWarnings("unchecked")
  public <T> boolean unregister(T object) {
    checkArgument(!(object instanceof Model), "can not unregister a model");
    checkState(configured,
        "can not unregister when not configured, call configure() first");

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
    return result;
  }

  /**
   * @return An unmodifiable view on all registered models.
   */
  public List<Model<?>> getModels() {
    return Collections.unmodifiableList(models);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public <T extends Model<?>> T tryGetModel(Class<T> clazz) {
    for (final Model<?> model : models) {
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
}
