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
package com.github.rinde.rinsim.scenario.gendreau06;

import static com.google.common.collect.Lists.newArrayList;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEvent.TimeComparator;

/**
 * @author Rinde van Lon
 *
 */
public class GendreauTestUtil {

  public static Gendreau06Scenario create(
      List<? extends TimedEvent> events, long ts) {
    Collections.sort(events, TimeComparator.INSTANCE);
    return Gendreau06Scenario.create(events, ts,
      GendreauProblemClass.SHORT_LOW_FREQ, 1, false, false);
  }

  public static Gendreau06Scenario create(List<? extends TimedEvent> parcels) {
    return createWithTrucks(parcels, 1);
  }

  public static Gendreau06Scenario createWithTrucks(
      List<? extends TimedEvent> parcels, int numTrucks) {

    final Gendreau06Scenario gs = Gendreau06Parser
      .parser()
      .addFile(new ByteArrayInputStream("".getBytes()), "req_rapide_1_240_24")
      .setNumVehicles(numTrucks)
      .parse().get(0);

    final List<TimedEvent> events = newArrayList();
    events.addAll(gs.getEvents());
    events.addAll(parcels);
    return create(events, 1000L);
  }
}
