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
package com.github.rinde.rinsim.experiment;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * This represents a configuration of a multi-agent system in a simulation.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class MASConfiguration implements Serializable {

  private static final long serialVersionUID = -4538293931753741399L;

  MASConfiguration() {}

  /**
   * @return The name of the configuration.
   */
  public abstract String getName();

  /**
   * @return A possibly empty list of model builders.
   */
  public abstract ImmutableSet<? extends ModelBuilder<?, ?>> getModels();

  /**
   * @return The event handlers of the configuration.
   */
  public abstract ImmutableMap<Class<? extends TimedEvent>, TimedEventHandler<?>> getEventHandlers();

  /**
   * @return A new {@link Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new {@link Builder} with default handlers for PDPTW problems. The
   * default handlers are:
   * <ul>
   * <li>{@link TimeOutEvent} is handled by {@link TimeOutEvent#ignoreHandler()}
   * </li>
   * <li>{@link AddParcelEvent} is handled by
   * {@link AddParcelEvent#defaultHandler()}</li>
   * <li>{@link AddDepotEvent} is handled by
   * {@link AddDepotEvent#defaultHandler()}</li>
   * </ul>
   * Each of these default handlers may be overridden by calls to
   * {@link Builder#addEventHandler(Class, TimedEventHandler)}.
   * @return A new {@link Builder}.
   */
  public static Builder pdptwBuilder() {
    return new Builder().addDefaultPDPTWHandlers();
  }

  /**
   * Builder for constructing {@link MASConfiguration} instances.
   * @author Rinde van Lon
   */
  public static final class Builder {
    String name;
    ImmutableSet.Builder<ModelBuilder<?, ?>> modelsBuilder;
    Map<Class<? extends TimedEvent>, TimedEventHandler<?>> eventHandlers;

    Builder() {
      name = MASConfiguration.class.getSimpleName();
      modelsBuilder = ImmutableSet.builder();
      eventHandlers = new LinkedHashMap<>();
    }

    /**
     * Sets the name of the configuration.
     * @param string The new name.
     * @return This, as per the builder pattern.
     */
    public Builder setName(String string) {
      name = string;
      return this;
    }

    /**
     * Adds the specified model builder.
     * @param model The model builder to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModel(ModelBuilder<?, ?> model) {
      checkSerializable(model);
      modelsBuilder.add(model);
      return this;
    }

    /**
     * Adds the specified model builders.
     * @param models The model builders to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModels(Iterable<? extends ModelBuilder<?, ?>> models) {
      for (ModelBuilder<?, ?> mb : models) {
        addModel(mb);
      }
      return this;
    }

    /**
     * Adds the specified {@link TimedEventHandler}s.
     * @param type The type of event to handle.
     * @param handler The handler to add.
     * @return This, as per the builder pattern.
     */
    public <T extends TimedEvent> Builder addEventHandler(Class<T> type,
      TimedEventHandler<T> handler) {
      checkSerializable(handler);
      eventHandlers.put(type, handler);
      return this;
    }

    Builder addDefaultPDPTWHandlers() {
      return addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
        .addEventHandler(AddDepotEvent.class, AddDepotEvent.defaultHandler())
        .addEventHandler(AddParcelEvent.class, AddParcelEvent.defaultHandler());
    }

    /**
     * @return A new {@link MASConfiguration} instance.
     */
    public MASConfiguration build() {
      return new AutoValue_MASConfiguration(
        name,
        modelsBuilder.build(),
        ImmutableMap.copyOf(eventHandlers));
    }

    static void checkSerializable(Object o) {
      checkArgument(o instanceof Serializable, "%s is not Serializable.", o);
    }
  }
}
