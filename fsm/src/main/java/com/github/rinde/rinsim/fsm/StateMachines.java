/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.fsm;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Table.Cell;

/**
 * Utility class for {@link StateMachine}.
 * @author Rinde van Lon
 */
public final class StateMachines {
  // constants used for serialization
  private static final String NL = System.getProperty("line.separator");
  private static final String NODE = "node";
  private static final String NODE_DEFINITION = "[label=\"\",shape=point]" + NL;
  private static final String CONN = " -> ";
  private static final String LABEL_OPEN = "[label=\"";
  private static final String LABEL_CLOSE = "\"]" + NL;
  private static final String FILE_OPEN = "digraph stategraph {" + NL;
  private static final String FILE_CLOSE = "}";

  private StateMachines() {}

  /**
   * @param fsm The state machine to create a dot graph for.
   * @return A dot representation of a state machine, can be used for
   *         visualizing the transition table.
   */
  public static String toDot(StateMachine<?, ?> fsm) {
    int id = 0;
    final StringBuilder builder = new StringBuilder();
    builder.append(FILE_OPEN);
    final Set<State<?, ?>> allStates = newHashSet();
    allStates.addAll(fsm.transitionTable.rowKeySet());
    allStates.addAll(fsm.transitionTable.values());
    final Map<State<?, ?>, Integer> idMap = newHashMap();
    for (final State<?, ?> s : allStates) {
      builder.append(NODE).append(id).append(LABEL_OPEN).append(s.name())
        .append(LABEL_CLOSE);
      idMap.put(s, id);
      id++;
    }
    builder.append(NODE).append(id).append(NODE_DEFINITION);
    builder.append(NODE).append(id).append(CONN).append(NODE)
      .append(idMap.get(fsm.startState)).append(NL);

    for (final Cell<?, ?, ?> cell : fsm.transitionTable.cellSet()) {
      final int id1 = idMap.get(cell.getRowKey());
      final int id2 = idMap.get(cell.getValue());
      builder.append(NODE).append(id1).append(CONN).append(NODE).append(id2)
        .append(LABEL_OPEN).append(cell.getColumnKey()).append(LABEL_CLOSE);
    }
    builder.append(FILE_CLOSE);
    return builder.toString();
  }
}
