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
package com.github.rinde.rinsim.core.model.road;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.google.common.base.Optional;

/**
 * A {@link RoadModel} that uses a {@link Graph} as road structure.
 * {@link RoadUser}s can only be added on nodes on the graph. This model assumes
 * that the {@link Graph} does <b>not</b> change. Modifying the graph after
 * passing it to the model may break this model, for support for modifications
 * take a look at {@link DynamicGraphRoadModelImpl}. The graph can define
 * {@link Connection} specific speed limits using {@link MultiAttributeData}.
 *
 * @author Rinde van Lon
 */
public interface GraphRoadModel extends RoadModel {

  /**
   * @return An unmodifiable view on the graph.
   */
  Graph<? extends ConnectionData> getGraph();

  /**
   * Retrieves the connection which the specified {@link RoadUser} is at. If the
   * road user is at a node, {@link Optional#absent()} is returned instead.
   * @param obj The object which position is checked.
   * @return A {@link Connection} if <code>obj</code> is on one,
   *         {@link Optional#absent()} otherwise.
   */
  Optional<? extends Connection<?>> getConnection(RoadUser obj);

}
