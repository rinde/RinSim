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
package com.github.rinde.rinsim.pdptw.common;

/**
 * Instances of this interface define an objective on which a
 * {@link StatisticsDTO} can be measured.
 * <p>
 * Implementations should not keep any internal state.
 * @author Rinde van Lon 
 */
public interface ObjectiveFunction {

  /**
   * Determines whether a simulation run is valid. Usually this checks whether
   * certain requirements have been met during the simulation, e.g. have all
   * vehicles returned to the depot?
   * @param stats The {@link StatisticsDTO} to inspect.
   * @return <code>true</code> if the {@link StatisticsDTO} represents a valid
   *         result according to this objective function, <code>false</code>
   *         otherwise.
   */
  boolean isValidResult(StatisticsDTO stats);

  /**
   * Computes the cost (i.e. objective value) of the specified
   * {@link StatisticsDTO}.
   * @param stats The {@link StatisticsDTO} to inspect.
   * @return The cost.
   */
  double computeCost(StatisticsDTO stats);

  /**
   * @param stats The {@link StatisticsDTO} to inspect.
   * @return Should return a human readable string containing the objective
   *         value and any other relevant measures.
   */
  String printHumanReadableFormat(StatisticsDTO stats);

}
