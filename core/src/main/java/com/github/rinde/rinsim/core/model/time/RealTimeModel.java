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

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.unit.SI;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Rinde van Lon
 *
 */
class RealTimeModel extends TimeModel {

  final ListeningScheduledExecutorService executor;
  final long tickNanoSeconds;

  RealTimeModel(Builder builder) {
    super(builder);
    executor = MoreExecutors.listeningDecorator(
      Executors
        .newSingleThreadScheduledExecutor(PriorityThreadFactory.INSTANCE));
    tickNanoSeconds = Measure.valueOf(timeLapse.getTickLength(),
      timeLapse.getTimeUnit()).longValue(SI.NANO(SI.SECOND));
  }

  @Deprecated
  @Override
  public void tick() {
    throw new UnsupportedOperationException(
      "Calling tick directly is not supported in "
        + RealTimeModel.class.getSimpleName());
  }

  // FIXME test events
  @Override
  void doStart() {
    checkState(!executor.isShutdown(), "%s can be started only once",
      getClass().getSimpleName());

    executor.scheduleAtFixedRate(
      new Runnable() {
        @Override
        public void run() {
          tickImpl();
        }
      }, 0, tickNanoSeconds, TimeUnit.NANOSECONDS);

    try {
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  void doStop() {
    isTicking = false;
    executor.shutdown();
  }

  enum PriorityThreadFactory implements ThreadFactory {
    INSTANCE {
      @Override
      public Thread newThread(@Nullable Runnable r) {
        final Thread t = new Thread(r);
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
      }
    }
  }
}
