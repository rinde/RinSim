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
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.event.EventDispatcher;

/**
 * A very generic implementation of the {@link RoadModel} interface.
 * @author Rinde van Lon
 */
public abstract class GenericRoadModel extends AbstractModel<RoadUser>
    implements RoadModel {

  /**
   * The logger of the model.
   */
  protected static final Logger LOGGER = LoggerFactory
      .getLogger(GenericRoadModel.class);

  /**
   * The types of events this model can dispatch.
   * @author Rinde van Lon
   */
  public enum RoadEventType {
    /**
     * Indicates that a {@link MovingRoadUser} has moved.
     */
    MOVE,

    /**
     * Is dispatched when a new {@link RoadUser} has been added to the model and
     * has a position.
     */
    ADD_ROAD_USER,

    /**
     * Is dispatched when a {@link RoadUser} has been removed from the model.
     */
    REMOVE_ROAD_USER
  }

  // TODO event dispatching has to be tested
  /**
   * The {@link EventDispatcher} that dispatches all event for this model.
   */
  protected final EventDispatcher eventDispatcher;

  /**
   * Reference to the outermost decorator of this road model, or to
   * <code>this</code> if there are no decorators.
   */
  protected GenericRoadModel self = this;
  private boolean initialized;

  /**
   * Initializes the model, optionally adding more event types to the
   * {@link EventDispatcher}.
   * @param additionalEventTypes Additional event types (may be empty).
   */
  protected GenericRoadModel(Enum<?>... additionalEventTypes) {
    final Set<Enum<?>> events = new LinkedHashSet<>();
    events.addAll(asList(additionalEventTypes));
    events.addAll(asList(RoadEventType.values()));
    eventDispatcher = new EventDispatcher(events);
  }

  /**
   * Method which should only be called by a decorator of this instance.
   * @param rm The decorator to set as 'self'.
   */
  protected void setSelf(GenericRoadModel rm) {
    LOGGER.info("setSelf {}", rm);
    checkState(
        !initialized,
        "This road model is already initialized, it can only be decorated "
            + "before objects are registered.");
    self = rm;
  }

  @Override
  public final boolean register(RoadUser object) {
    initialized = true;
    return doRegister(object);
  }

  /**
   * Actual implementation of {@link #register(RoadUser)}.
   * @param object The object to register.
   * @return <code>true</code> when registration succeeded, <code>false</code>
   *         otherwise.
   */
  protected abstract boolean doRegister(RoadUser object);

}
