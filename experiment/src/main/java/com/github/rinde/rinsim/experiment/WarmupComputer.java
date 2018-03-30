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
package com.github.rinde.rinsim.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.experiment.Experiment.Builder;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;

class WarmupComputer implements Computer {
  static final Logger LOGGER = LoggerFactory.getLogger(WarmupComputer.class);
  final Computer delegate;

  WarmupComputer(Computer deleg) {
    delegate = deleg;
  }

  @Override
  public ExperimentResults compute(Builder builder, Set<SimArgs> inputs) {
    final Thread main = Thread.currentThread();
    Executors.newScheduledThreadPool(1).schedule(new Runnable() {
      @Override
      public void run() {
        LOGGER.trace("Interrupt {}", main);
        main.interrupt();
      }
    }, builder.warmupPeriodMs, TimeUnit.MILLISECONDS);

    // remove listeners only during warmup
    final List<ResultListener> list = new ArrayList<>(builder.resultListeners);
    builder.resultListeners.clear();
    final ExperimentResults results = delegate.compute(builder, inputs);
    builder.resultListeners.addAll(list);
    return results;
  }
}
