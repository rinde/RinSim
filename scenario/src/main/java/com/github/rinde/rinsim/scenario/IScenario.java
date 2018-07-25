package com.github.rinde.rinsim.scenario;

/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
import java.util.Queue;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public interface IScenario {

  /**
   * Return a scenario as a list of (time sorted) events.
   * @return the list of events.
   */
  ImmutableList<TimedEvent> getEvents();

  /**
   * @return A queue containing all events of this scenario.
   */
  Queue<TimedEvent> asQueue();

  /**
   * @return Should return a list of {@link ModelBuilder}s which will be used
   *         for creating the models for this scenario.
   */
  ImmutableSet<ModelBuilder<?, ?>> getModelBuilders();

  /**
   * @return The {@link TimeWindow} of the scenario indicates the start and end
   *         of the scenario.
   */
  TimeWindow getTimeWindow();

  /**
   * @return The stop condition indicating when a simulation should end.
   */
  StopCondition getStopCondition();

  /**
   * @return The 'class' to which this scenario belongs.
   */
  ProblemClass getProblemClass();

  /**
   * @return The instance id of this scenario.
   */
  String getProblemInstanceId();

  @Override
  boolean equals(@Nullable Object other);

  @Override
  int hashCode();

}
