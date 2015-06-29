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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
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

  @Override
  void doStart() {
    checkState(!executor.isShutdown(), "%s can be started only once",
      getClass().getSimpleName());

    final List<Long> timings = new ArrayList<>();

    @SuppressWarnings("unchecked")
    final ListenableScheduledFuture<Object> f = (ListenableScheduledFuture<Object>) executor
      .scheduleAtFixedRate(
        new Runnable() {
          @Override
          public void run() {
            timings.add(System.nanoTime());
            checkConsistency(timings);
            tickImpl();
          }
        }, 0, tickNanoSeconds, TimeUnit.NANOSECONDS);

    final List<Throwable> exceptions = new ArrayList<>();
    Futures.addCallback(f, new FutureCallback<Object>() {
      @Override
      public void onSuccess(@Nullable Object result) {}

      @Override
      public void onFailure(Throwable t) {
        exceptions.add(t);
        stop();
      }
    });

    try {
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }

    if (!exceptions.isEmpty()) {
      if (exceptions.get(0) instanceof RuntimeException) {
        throw (RuntimeException) exceptions.get(0);
      }
      throw new IllegalStateException(exceptions.get(0));
    }
  }

  static final int CONSISTENCY_CHECK_LENGTH = 50;

  void checkConsistency(List<Long> timestamps) {
    if (timestamps.size() < CONSISTENCY_CHECK_LENGTH) {
      return;
    }
    final PeekingIterator<Long> it = Iterators.peekingIterator(timestamps
      .subList(timestamps.size() - CONSISTENCY_CHECK_LENGTH, timestamps.size())
      .iterator());

    final List<Long> interArrivalTimes = new ArrayList<>();
    for (long l1 = it.next(); it.hasNext(); l1 = it.next()) {
      final Long l2 = it.peek();
      interArrivalTimes.add(l2 - l1);
    }

    final SummaryStatistics ss = new SummaryStatistics();
    for (final Long n : interArrivalTimes) {
      ss.addValue(n.longValue());
    }
    final StatisticalSummary sum = ss.getSummary();
    System.out.printf("%1.2f +- %1.2f%n", sum.getMean() / 1000000d,
      sum.getStandardDeviation() / 1000000d);

    // standard deviation may not be greater than 5ms
    checkState(sum.getStandardDeviation() < 5000000d,
      "Std is above threshold of 5ms: %s.", sum.getStandardDeviation());
    // on average we don't want a deviation to the mean of more than 1 ms per
    // tick.
    checkState(Math.abs(tickNanoSeconds - sum.getMean()) < 1000000,
      "Mean interval is above threshold of 1ms: %s.", sum.getMean());
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
