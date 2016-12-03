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
package com.github.rinde.rinsim.pdptw.common;

import java.io.Serializable;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.auto.value.AutoValue;

/**
 * Event indicating that the maximal allowed speed on a connection should be
 * altered.
 * @author Vincent Van Gestel
 */
@AutoValue
public abstract class ChangeConnectionSpeedEvent implements TimedEvent {

  ChangeConnectionSpeedEvent() {}

  /**
   * @return The From point of the connection which will have its maximum
   *         allowed speed changed.
   */
  public abstract Point getFrom();

  /**
   * @return The To point of the connection which will have its maximum allowed
   *         speed changed.
   */
  public abstract Point getTo();

  /**
   * @return The factor with which the current speed should be changed.
   */
  public abstract double getFactor();

  /**
   * Creates a new {@link ChangeConnectionSpeedEvent}.
   * @param eventTiming The timing at which the change should occur.
   * @param conn The affected connection.
   * @param factor The factor with which the connection maximal allowed speed
   *          should be altered with.
   * @return A new instance.
   */
  public static ChangeConnectionSpeedEvent create(long eventTiming,
      Connection<MultiAttributeData> conn, double factor) {
    return new AutoValue_ChangeConnectionSpeedEvent(eventTiming, conn.from(),
      conn.to(),
      factor);
  }

  /**
   * Default {@link TimedEventHandler} that changes the maximal allowed speed by
   * multiplying the current speed on a {@link Connection} with the given
   * factor.
   * @return The default handler.
   */
  public static TimedEventHandler<ChangeConnectionSpeedEvent> defaultHandler() {
    return new DefaultChangeConnectionSpeedEventHandler();
  }

  static class DefaultChangeConnectionSpeedEventHandler
      implements TimedEventHandler<ChangeConnectionSpeedEvent>, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    DefaultChangeConnectionSpeedEventHandler() {}

    @Override
    public void handleTimedEvent(ChangeConnectionSpeedEvent event,
        SimulatorAPI simulator) {
      final Graph<MultiAttributeData> graph =
        (Graph<MultiAttributeData>) ((Simulator) simulator)
          .getModelProvider().getModel(PDPDynamicGraphRoadModel.class)
          .getGraph();
      final MultiAttributeData data =
        graph.connectionData(event.getFrom(), event.getTo()).get();
      graph.setConnectionData(
        event.getFrom(),
        event.getTo(),
        MultiAttributeData.builder()
          .addAllAttributes(data.getAttributes())
          .setLength(data.getLength().get())
          .setMaxSpeed(data.getMaxSpeed().get() * event.getFactor())
          .build());
    }

    @Override
    public String toString() {
      return ChangeConnectionSpeedEvent.class.getSimpleName()
        + ".defaultHandler()";
    }
  }

}
