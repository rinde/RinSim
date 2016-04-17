/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import javax.annotation.Nonnull;

import com.google.common.reflect.TypeToken;

/**
 * @author Bartosz Michalik
 * @author Rinde van Lon
 * @param <T> basic type of element supported by model
 */
public interface Model<T> {

  /**
   * Register element in a model.
   * @param element the <code>! null</code> should be imposed
   * @return true if the object was successfully registered
   */
  boolean register(T element);

  /**
   * Unregister element from a model.
   * @param element the <code>! null</code> should be imposed
   * @return true if the unregistration changed the model (element was part of
   *         the model and it was succesfully removed)
   */
  boolean unregister(T element);

  /**
   * @return The class of the type supported by this model.
   */
  Class<T> getSupportedType();

  /**
   * Implementors of this method should return an object of the specified type.
   * A {@link Model} can advertise the types it can provide via
   * {@link ModelBuilder#getProvidingTypes()}.
   * @param clazz The type.
   * @param <U> The type.
   * @return A new instance of type <code>U</code>.
   * @throws IllegalArgumentException For classes for which it has no support.
   */
  <U> U get(Class<U> clazz);

  /**
   * Basic implementation that provides a getSupportedType method
   * implementation.
   * @author Bartosz Michalik
   * @author Rinde van Lon
   * @param <T> The type that is supported by this model.
   */
  abstract class AbstractModel<T> implements Model<T> {

    private final Class<T> supportedType;

    /**
     * Create a new model.
     */
    @SuppressWarnings({"serial", "unchecked"})
    protected AbstractModel() {
      supportedType = (Class<T>) new TypeToken<T>(getClass()) {}.getRawType();
    }

    @Override
    public final Class<T> getSupportedType() {
      return supportedType;
    }

    @Override
    @Nonnull
    public <U> U get(Class<U> clazz) {
      throw new IllegalArgumentException(
        "This model (" + getClass().getSimpleName()
          + ") does not support providing any objects (" + toString()
          + ").");
    }
  }

  /**
   * Abstract implementation of a model without supported type.
   * @author Rinde van Lon
   */
  abstract class AbstractModelVoid extends AbstractModel<Void> {

    @Override
    public final boolean register(Void element) {
      return false;
    }

    @Override
    public final boolean unregister(Void element) {
      return false;
    }
  }
}
