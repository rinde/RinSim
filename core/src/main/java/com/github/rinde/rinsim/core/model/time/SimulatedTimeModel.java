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
package com.github.rinde.rinsim.core.model.time;


/**
 * @author Rinde van Lon
 *
 */
class SimulatedTimeModel extends TimeModel {

  SimulatedTimeModel(Builder builder) {
    super(builder);
  }

  @Override
  void doStart() {
    try {
      while (isTicking()) {
        tickImpl();
      }
    } catch (final RuntimeException e) {
      cleanUpAfterException();
      throw e;
    }
  }

  @Override
  public void stop() {
    isTicking = false;
  }

  @Override
  public void tick() {
    tickImpl();
  }
}
