/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
package com.github.rinde.rinsim.geom;

import org.junit.Test;

import com.github.rinde.rinsim.event.ListenerEventHistory;

public class ListenableGraphTest {

  Point a = new Point(0, 0);
  Point b = new Point(1, 0);
  Point c = new Point(2, 0);
  Point d = new Point(3, 0);
  Point e = new Point(4, 0);

  @Test
  public void test() {
    ListenerEventHistory history = new ListenerEventHistory();
    ListenableGraph<ConnectionData> graph = new ListenableGraph<>(
        new MultimapGraph<>());
    graph.getEventAPI().addListener(history,
        ListenableGraph.EventTypes.values());

    graph.addConnection(a, b);

    System.out.println(history.getHistory());

  }

}
