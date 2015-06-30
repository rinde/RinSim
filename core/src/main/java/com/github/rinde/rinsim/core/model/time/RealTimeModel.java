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

import static com.github.rinde.rinsim.core.model.time.RealTimeModel.SimpleState.INIT_FF;
import static com.github.rinde.rinsim.core.model.time.RealTimeModel.SimpleState.INIT_RT;
import static com.github.rinde.rinsim.core.model.time.RealTimeModel.SimpleState.STOPPED;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.github.rinde.rinsim.fsm.AbstractState;
import com.github.rinde.rinsim.fsm.StateMachine;
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
class RealTimeModel extends TimeModel implements RealTimeClockController {
  final StateMachine<Trigger, RealTimeModel> stateMachine;
  final Map<TickListener, TickListenerTimingChecker> decoratorMap;

  RealTimeModel(Builder builder) {
    super(builder);
    decoratorMap = new HashMap<>();

    final RealTime rt = new RealTime(timeLapse);
    final SimulatedTime ff = new SimulatedTime();
    stateMachine = StateMachine.create(INIT_RT)
      .addTransition(INIT_RT, Trigger.FAST_FORWARD, INIT_FF)
      .addTransition(INIT_RT, Trigger.START, rt)
      .addTransition(INIT_FF, Trigger.REAL_TIME, INIT_RT)
      .addTransition(INIT_FF, Trigger.START, ff)
      .addTransition(rt, Trigger.FAST_FORWARD, rt)
      .addTransition(rt, Trigger.REAL_TIME, rt)
      .addTransition(rt, Trigger.DO_FAST_FORWARD, ff)
      .addTransition(rt, Trigger.STOP, STOPPED)
      .addTransition(ff, Trigger.REAL_TIME, ff)
      .addTransition(ff, Trigger.FAST_FORWARD, ff)
      .addTransition(ff, Trigger.DO_REAL_TIME, rt)
      .addTransition(ff, Trigger.STOP, STOPPED)
      .build();
  }

  @Override
  void doStart() {
    checkState(stateMachine.isSupported(Trigger.START),
      "%s can be started only once",
      getClass().getSimpleName());
    stateMachine.handle(Trigger.START, this);
  }

  @Override
  public void stop() {
    checkState(stateMachine.isSupported(Trigger.STOP),
      "Can not stop in current state: %s",
      stateMachine.getCurrentState().name());
    stateMachine.handle(Trigger.STOP, this);
  }

  @Deprecated
  @Override
  public void tick() {
    throw new UnsupportedOperationException(
      "Calling tick directly is not supported in "
        + RealTimeModel.class.getSimpleName());
  }

  @Override
  public boolean isTicking() {
    return !stateMachine.stateIsOneOf(SimpleState.values());
  }

  @Override
  public void switchToRealTime() {
    stateMachine.handle(Trigger.REAL_TIME, this);
  }

  @Override
  public void switchToSimulatedTime() {
    stateMachine.handle(Trigger.FAST_FORWARD, this);
  }

  @Override
  public boolean register(TickListener tickListener) {
    checkArgument(!decoratorMap.containsKey(tickListener),
      "A TickListener can not be registered more than once: %s.", tickListener);
    final TickListenerTimingChecker decorated = new TickListenerTimingChecker(
      tickListener);
    decoratorMap.put(tickListener, decorated);
    return super.register(decorated);
  }

  @Override
  public boolean unregister(TickListener tickListener) {
    return super.unregister(decoratorMap.remove(tickListener));
  }

  enum Trigger {
    START, STOP, FAST_FORWARD, DO_FAST_FORWARD, REAL_TIME, DO_REAL_TIME;
  }

  static class SimulatedTime extends AbstractState<Trigger, RealTimeModel> {
    boolean isTicking;
    @Nullable
    Trigger nextTrigger;

    @Override
    @Nullable
    public Trigger handle(@Nullable Trigger trigger, RealTimeModel context) {
      if (trigger == Trigger.REAL_TIME) {
        isTicking = false;
        nextTrigger = Trigger.DO_REAL_TIME;
        return null;
      } else if (trigger == Trigger.FAST_FORWARD) {
        isTicking = true;
        nextTrigger = null;
        return null;
      }
      isTicking = true;
      while (isTicking) {
        context.tickImpl();
      }
      final Trigger t = nextTrigger;
      nextTrigger = null;
      return t;
    }

    @Override
    public void onExit(Trigger event, RealTimeModel context) {
      isTicking = false;
    }
  }

  static class RealTime extends AbstractState<Trigger, RealTimeModel> {
    // number of ticks that will be checked for consistency
    static final int CONSISTENCY_CHECK_LENGTH = 50;
    // max standard deviation = 5ms
    static final double MAX_STD_NS = 5000000d;
    // max mean deviation = 1ms
    static final double MAX_MEAN_DEVIATION_NS = 1000000d;

    ListeningScheduledExecutorService executor;
    final long tickNanoSeconds;
    final List<Throwable> exceptions;

    @Nullable
    Trigger nextTrigger;

    @SuppressWarnings("null")
    RealTime(TimeLapse timeLapse) {
      tickNanoSeconds = Measure.valueOf(timeLapse.getTickLength(),
        timeLapse.getTimeUnit()).longValue(SI.NANO(SI.SECOND));
      exceptions = new ArrayList<>();
    }

    @Override
    public void onEntry(Trigger event, RealTimeModel context) {
      initExecutor();
    }

    @Override
    @Nullable
    public Trigger handle(@Nullable Trigger event,
      final RealTimeModel context) {
      if (event == Trigger.FAST_FORWARD) {
        nextTrigger = Trigger.DO_FAST_FORWARD;
        return null;
      } else if (event == Trigger.REAL_TIME) {
        nextTrigger = null;
        return null;
      }

      final List<Long> timings = new ArrayList<>();
      final RealTime ref = this;
      @SuppressWarnings("unchecked")
      final ListenableScheduledFuture<Object> f = (ListenableScheduledFuture<Object>) executor
        .scheduleAtFixedRate(
          new Runnable() {
            @Override
            public void run() {
              timings.add(System.nanoTime());
              checkConsistency(timings);
              context.tickImpl();
              if (ref.nextTrigger != null) {
                executor.shutdown();
              }
            }
          }, 0, tickNanoSeconds, TimeUnit.NANOSECONDS);

      Futures.addCallback(f, new FutureCallback<Object>() {
        @Override
        public void onFailure(Throwable t) {
          exceptions.add(t);
          executor.shutdown();
        }

        @Override
        public void onSuccess(@Nullable Object result) {}
      });
      awaitTermination();
      final Trigger t = nextTrigger;
      nextTrigger = null;
      return t;
    }

    @Override
    public void onExit(Trigger event, RealTimeModel context) {
      if (!executor.isShutdown()) {
        executor.shutdown();
      }
    }

    void initExecutor() {
      executor = MoreExecutors.listeningDecorator(
        Executors
          .newSingleThreadScheduledExecutor(PriorityThreadFactory.INSTANCE));
    }

    void awaitTermination() {
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

    void checkConsistency(List<Long> timestamps) {
      if (timestamps.size() < CONSISTENCY_CHECK_LENGTH) {
        return;
      }
      final PeekingIterator<Long> it = Iterators.peekingIterator(timestamps
        .subList(timestamps.size() - CONSISTENCY_CHECK_LENGTH,
          timestamps.size())
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

      // standard deviation may not be greater than 5ms
      checkState(sum.getStandardDeviation() < MAX_STD_NS,
        "Std is above threshold of 5ms: %s.", sum.getStandardDeviation());
      // on average we don't want a deviation to the mean of more than 1 ms per
      // tick.
      checkState(
        Math.abs(tickNanoSeconds - sum.getMean()) < MAX_MEAN_DEVIATION_NS,
        "Mean interval is above threshold of 1ms: %s.", sum.getMean());
    }
  }

  // for some reason the complete package name is required for the Java compiler
  @SuppressWarnings("null")
  enum SimpleState
    implements com.github.rinde.rinsim.fsm.State<Trigger, RealTimeModel> {
    INIT_RT, INIT_FF, STOPPED;

    @Override
    @Nullable
    public Trigger handle(@Nullable Trigger event, RealTimeModel context) {
      return null;
    }

    @Override
    public void onEntry(Trigger event, RealTimeModel context) {}

    @Override
    public void onExit(Trigger event, RealTimeModel context) {}
  }

  static class TickListenerTimingChecker implements TickListener {
    final TickListener delegate;

    long tickDuration;

    TickListenerTimingChecker(TickListener tl) {
      delegate = tl;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
      final long start = System.nanoTime();
      delegate.afterTick(timeLapse);
      final long afterTickDuration = System.nanoTime() - start;

      checkState(
        tickDuration + afterTickDuration < timeLapse.getTickLength() * 1000000,
        "%s took too much time, max combined computation time is tick length "
          + "(%sms). Tick duration: %sns, after tick duration: %sns.",
        delegate, timeLapse.getTickLength(), tickDuration, afterTickDuration);
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      final long start = System.nanoTime();
      delegate.tick(timeLapse);
      tickDuration = System.nanoTime() - start;
    }

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
