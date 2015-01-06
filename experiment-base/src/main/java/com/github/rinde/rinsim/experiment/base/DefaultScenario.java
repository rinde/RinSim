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
package com.github.rinde.rinsim.experiment.base;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import com.google.common.collect.ComparisonChain;

public class DefaultScenario implements Scenario {

  protected final ProblemClass problemClass;
  protected final String instanceId;

  public DefaultScenario(ProblemClass pc, String id) {
    problemClass = pc;
    instanceId = id;
  }

  @Override
  public ProblemClass getProblemClass() {
    return problemClass;
  }

  @Override
  public String getProblemInstanceId() {
    return instanceId;
  }

  @Override
  public int compareTo(@Nullable Scenario o) {
    requireNonNull(o);
    return ComparisonChain.start()
        .compare(problemClass, o.getProblemClass())
        .compare(instanceId, o.getProblemInstanceId())
        .result();
  }

  @Override
  public String toString() {
    return toStringHelper("DefaultScenario")
        .add("problemClass", problemClass)
        .add("instanceId", instanceId)
        .toString();
  }
}
