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

import static com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType.SWITCH_TO_REAL_TIME;
import static com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType.SWITCH_TO_SIM_TIME;
import static com.github.rinde.rinsim.core.model.time.RealtimeModel.SimpleState.INIT_RT;
import static com.github.rinde.rinsim.core.model.time.RealtimeModel.SimpleState.INIT_ST;
import static com.github.rinde.rinsim.core.model.time.RealtimeModel.SimpleState.STOPPED;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.fsm.AbstractState;
import com.github.rinde.rinsim.fsm.StateMachine;
import com.github.rinde.rinsim.fsm.StateMachine.StateMachineEvent;
import com.github.rinde.rinsim.fsm.StateMachine.StateTransitionEvent;
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
class RealtimeModel extends TimeModel implements RealtimeClockController {
  final StateMachine<Trigger, RealtimeModel> stateMachine;
  final Map<TickListener, TickListenerTimingChecker> decoratorMap;

  RealtimeModel(RealtimeBuilder builder) {
    super(builder, RtClockEventType.values());
    decoratorMap = new HashMap<>();

    final Realtime rt = new Realtime(timeLapse);
    final SimulatedTime st = new SimulatedTime();
    stateMachine = StateMachine
        .create(
            builder.getClockMode() == ClockMode.REAL_TIME ? INIT_RT : INIT_ST)
        .addTransition(INIT_RT, Trigger.SIMULATE, INIT_ST)
        .addTransition(INIT_RT, Trigger.START, rt)
        .addTransition(INIT_ST, Trigger.REAL_TIME, INIT_RT)
        .addTransition(INIT_ST, Trigger.START, st)
        .addTransition(rt, Trigger.SIMULATE, rt)
        .addTransition(rt, Trigger.REAL_TIME, rt)
        .addTransition(rt, Trigger.DO_SIMULATE, st)
        .addTransition(rt, Trigger.STOP, STOPPED)
        .addTransition(st, Trigger.REAL_TIME, st)
        .addTransition(st, Trigger.SIMULATE, st)
        .addTransition(st, Trigger.DO_REAL_TIME, rt)
        .addTransition(st, Trigger.STOP, STOPPED)
        .addTransition(STOPPED, Trigger.STOP, STOPPED)
        .build();

    final RealtimeModel ref = this;
    stateMachine.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        @SuppressWarnings("unchecked")
        final StateTransitionEvent<Trigger, RealtimeModel> event =
            (StateTransitionEvent<Trigger, RealtimeModel>) e;
        if ((event.newState == rt || event.newState == INIT_RT)
            && eventDispatcher.hasListenerFor(SWITCH_TO_REAL_TIME)) {
          eventDispatcher.dispatchEvent(new Event(SWITCH_TO_REAL_TIME, ref));
        } else if ((event.newState == st || event.newState == INIT_ST)
            && eventDispatcher.hasListenerFor(SWITCH_TO_SIM_TIME)) {
          eventDispatcher.dispatchEvent(new Event(SWITCH_TO_SIM_TIME, ref));
        }
      }
    }, StateMachineEvent.values());
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
        "Can not stop time in current state: %s",
        stateMachine.getCurrentState().name());
    stateMachine.handle(Trigger.STOP, this);
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public void tick() {
    throw new UnsupportedOperationException(
        "Calling tick directly is not supported in "
            + RealtimeModel.class.getSimpleName());
  }

  @Override
  public boolean isTicking() {
    return !stateMachine.stateIsOneOf(SimpleState.values());
  }

  @Override
  public void switchToRealTime() {
    checkState(stateMachine.isSupported(Trigger.REAL_TIME),
        "Can not switch to real time mode because clock is already stopped.");
    stateMachine.handle(Trigger.REAL_TIME, this);
  }

  @Override
  public void switchToSimulatedTime() {
    checkState(stateMachine.isSupported(Trigger.SIMULATE),
        "Can not switch to simulated time mode because clock is already "
            + "stopped.");
    stateMachine.handle(Trigger.SIMULATE, this);
  }

  @Override
  public ClockMode getClockMode() {
    return ((ClockState) stateMachine.getCurrentState()).getClockMode();
  }

  @Override
  public boolean register(TickListener tickListener) {
    checkArgument(!decoratorMap.containsKey(tickListener),
        "A TickListener can not be registered more than once: %s.",
        tickListener);
    final TickListenerTimingChecker decorated = new TickListenerTimingChecker(
        tickListener);
    decoratorMap.put(tickListener, decorated);
    return super.register(decorated);
  }

  @Override
  public boolean unregister(TickListener tickListener) {
    return super.unregister(decoratorMap.remove(tickListener));
  }

  @Override
  @Nonnull
  public <U> U get(Class<U> clazz) {
    if (clazz == RealtimeClockController.class) {
      return clazz.cast(this);
    }
    return super.get(clazz);
  }

  enum Trigger {
    START, STOP, SIMULATE, DO_SIMULATE, REAL_TIME, DO_REAL_TIME;
  }

  interface ClockState
      extends com.github.rinde.rinsim.fsm.State<Trigger, RealtimeModel> {

    /**
     * @return The {@link RealtimeClockController.ClockMode} the clock is in.
     */
    ClockMode getClockMode();
  }

  abstract static class AbstractClockState
      extends AbstractState<Trigger, RealtimeModel>
      implements ClockState {}

  static class SimulatedTime extends AbstractClockState {
    boolean isTicking;
    @Nullable
    Trigger nextTrigger;

    SimulatedTime() {}

    @Override
    @Nullable
    public Trigger handle(@Nullable Trigger trigger, RealtimeModel context) {
      if (trigger == Trigger.REAL_TIME) {
        isTicking = false;
        nextTrigger = Trigger.DO_REAL_TIME;
        return null;
      } else if (trigger == Trigger.SIMULATE) {
        if (nextTrigger == Trigger.DO_REAL_TIME) {
          return null;
        }
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
    public void onExit(Trigger event, RealtimeModel context) {
      isTicking = false;
    }

    @Override
    public ClockMode getClockMode() {
      return ClockMode.SIMULATED;
    }
  }

  static class Realtime extends AbstractClockState {
    // number of ticks that will be checked for consistency
    static final int CONSISTENCY_CHECK_LENGTH = 50;

    static final double MAX_STD_PERC = .1;
    static final double MAX_MEAN_DEVIATION_PERC = .05;

    ListeningScheduledExecutorService executor;
    final long tickNanoSeconds;
    final double maxStdNs;
    final double maxMeanDeviationNs;
    final List<Throwable> exceptions;
    @Nullable
    Trigger nextTrigger;

    // keeps time for last real-time request while in RT mode
    long lastRtRequest;

    @SuppressWarnings("null")
    Realtime(TimeLapse timeLapse) {
      tickNanoSeconds = Measure.valueOf(timeLapse.getTickLength(),
          timeLapse.getTimeUnit()).longValue(SI.NANO(SI.SECOND));
      maxStdNs = tickNanoSeconds * MAX_STD_PERC;
      maxMeanDeviationNs = tickNanoSeconds * MAX_MEAN_DEVIATION_PERC;

      exceptions = new ArrayList<>();
    }

    @Override
    public void onEntry(Trigger event, RealtimeModel context) {
      initExecutor();
    }

    @Override
    @Nullable
    public Trigger handle(@Nullable Trigger event,
        final RealtimeModel context) {
      if (event == Trigger.SIMULATE) {
        // RT takes precedence over ST, if a request for RT has been made during
        // the same tick, all ST requests are ignored.
        if (context.getCurrentTime() > lastRtRequest) {
          nextTrigger = Trigger.DO_SIMULATE;
        }
        return null;
      } else if (event == Trigger.REAL_TIME) {
        lastRtRequest = context.getCurrentTime();
        nextTrigger = null;
        return null;
      }

      final List<Long> timings = new ArrayList<>();
      final Realtime ref = this;
      @SuppressWarnings("unchecked")
      final ListenableScheduledFuture<Object> f =
          (ListenableScheduledFuture<Object>) executor
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
    public void onExit(Trigger event, RealtimeModel context) {
      if (!executor.isShutdown()) {
        executor.shutdown();
      }
    }

    void initExecutor() {
      executor = MoreExecutors.listeningDecorator(
          Executors.newSingleThreadScheduledExecutor(
              PriorityThreadFactory.INSTANCE));
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
        } else if (exceptions.get(0) instanceof Error) {
          throw (Error) exceptions.get(0);
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

      checkState(sum.getStandardDeviation() < maxStdNs,
          "Std is above threshold of %sns: %sns.", maxStdNs,
          sum.getStandardDeviation(), interArrivalTimes);
      checkState(
          Math.abs(tickNanoSeconds - sum.getMean()) < maxMeanDeviationNs,
          "Mean interval is above threshold of %sns: %s.",
          maxMeanDeviationNs, sum.getMean());
    }

    @Override
    public ClockMode getClockMode() {
      return ClockMode.REAL_TIME;
    }
  }

  @SuppressWarnings("null")
  enum SimpleState implements ClockState {
    INIT_RT {
      @Override
      public ClockMode getClockMode() {
        return ClockMode.REAL_TIME;
      }
    },
    INIT_ST {
      @Override
      public ClockMode getClockMode() {
        return ClockMode.SIMULATED;
      }
    },
    STOPPED {
      @Override
      public ClockMode getClockMode() {
        return ClockMode.STOPPED;
      }
    };

    @Override
    @Nullable
    public Trigger handle(@Nullable Trigger event, RealtimeModel context) {
      return null;
    }

    @Override
    public void onEntry(Trigger event, RealtimeModel context) {}

    @Override
    public void onExit(Trigger event, RealtimeModel context) {}

  }

  static class TickListenerTimingChecker implements TickListener {
    static final double MS_TO_NS = 1000000d;
    static final double UPPER_LIMIT_FACTOR = 1.2d;
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
          tickDuration + afterTickDuration < UPPER_LIMIT_FACTOR
              * timeLapse.getTickLength() * MS_TO_NS,
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

      @Override
      public String toString() {
        return PriorityThreadFactory.class.getSimpleName();
      }
    }
  }
}
