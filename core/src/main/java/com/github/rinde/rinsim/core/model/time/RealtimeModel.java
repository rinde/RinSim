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

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.unit.SI;

import net.openhft.affinity.AffinityLock;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.fsm.AbstractState;
import com.github.rinde.rinsim.fsm.StateMachine;
import com.github.rinde.rinsim.fsm.StateMachine.StateMachineEvent;
import com.github.rinde.rinsim.fsm.StateMachine.StateTransitionEvent;
import com.google.common.math.DoubleMath;
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
  // number of ticks that will be checked for consistency
  static final int CONSISTENCY_CHECK_LENGTH = 50;
  static final double MAX_STD_PERC = .1;
  static final double MAX_MEAN_DEVIATION_PERC = .05;
  static final double MAX_TICK_LENGTH_FACTOR = 1.2d;

  final StateMachine<Trigger, RealtimeModel> stateMachine;
  final Map<TickListener, TickListenerTimingChecker> decoratorMap;
  final AffinityLock affinityLock;
  final long maxTickDuration;

  RealtimeModel(RealtimeBuilder builder) {
    super(builder, RtClockEventType.values());
    decoratorMap = new LinkedHashMap<>();

    final long tickNanoSeconds = Measure.valueOf(timeLapse.getTickLength(),
        timeLapse.getTimeUnit()).longValue(SI.NANO(SI.SECOND));

    maxTickDuration = DoubleMath.roundToLong(
        MAX_TICK_LENGTH_FACTOR * tickNanoSeconds, RoundingMode.UP);

    final long maxStdNs =
        DoubleMath.roundToLong(tickNanoSeconds * MAX_STD_PERC, RoundingMode.UP);
    final long maxMeanDeviationNs = DoubleMath.roundToLong(
        tickNanoSeconds * MAX_MEAN_DEVIATION_PERC, RoundingMode.UP);

    affinityLock = AffinityLock.acquireLock();

    final Realtime rt =
        new Realtime(tickNanoSeconds, maxStdNs, maxMeanDeviationNs,
            maxTickDuration);
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
  void cleanUpAfterException() {
    affinityLock.release();
    super.cleanUpAfterException();
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
    affinityLock.release();
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
        tickListener, maxTickDuration);
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

  String printTLDump() {
    long sumDuration = 0;
    final StringBuilder sb = new StringBuilder();
    for (final TickListenerTimingChecker tl : decoratorMap.values()) {
      sb.append(tl.delegate)
          .append(" took ")
          .append(tl.tickDuration)
          .append(" ns")
          .append(System.lineSeparator());
      sumDuration += tl.tickDuration;
    }
    sb.append("sum duration ")
        .append(sumDuration)
        .append(" nano seconds")
        .append(System.lineSeparator());
    return sb.toString();
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
      try {
        while (isTicking) {
          context.tickImpl();
        }
      } catch (final RuntimeException e) {
        context.cleanUpAfterException();
        throw e;
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
    final long tickNanoSeconds;
    final double maxStdNs;
    final double maxMeanDeviationNs;
    final long maxTickDuration;
    final List<Throwable> exceptions;
    @Nullable
    Trigger nextTrigger;
    ListeningScheduledExecutorService executor;

    // keeps time for last real-time request while in RT mode
    long lastRtRequest;

    @SuppressWarnings("null")
    Realtime(long tickNs, long maxStd, long maxMeanDevNs, long mxTickDuration) {
      tickNanoSeconds = tickNs;
      maxStdNs = maxStd;
      maxMeanDeviationNs = maxMeanDevNs;
      maxTickDuration = mxTickDuration;
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

      final TimeRunner tr = new TimeRunner(context);

      @SuppressWarnings("unchecked")
      final ListenableScheduledFuture<Object> f =
          (ListenableScheduledFuture<Object>) executor.scheduleAtFixedRate(tr,
              0, tickNanoSeconds, TimeUnit.NANOSECONDS);

      Futures.addCallback(f, new FutureCallback<Object>() {
        @Override
        public void onFailure(Throwable t) {
          exceptions.add(t);
          executor.shutdown();
        }

        @Override
        public void onSuccess(@Nullable Object result) {}
      });
      awaitTermination(context, tr);
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
          Executors.newSingleThreadScheduledExecutor());
    }

    void awaitTermination(RealtimeModel context, TimeRunner timeRunner) {
      try {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
      } catch (final InterruptedException e) {
        throw new IllegalStateException(e);
      }
      if (!exceptions.isEmpty()) {
        context.cleanUpAfterException();
        if (exceptions.get(0) instanceof RuntimeException) {
          throw (RuntimeException) exceptions.get(0);
        } else if (exceptions.get(0) instanceof Error) {
          throw (Error) exceptions.get(0);
        }
        throw new IllegalStateException(exceptions.get(0));
      }
      timeRunner.awaitCorrectness();
    }

    @Override
    public ClockMode getClockMode() {
      return ClockMode.REAL_TIME;
    }

    class TimeRunner implements Runnable {
      final List<Long> interArrivalTimes;
      final LinkedList<Long> timeStamps;
      final LinkedList<Long> timeStampsBuffer;
      final RealtimeModel context;

      GCLogMonitor logMonitor;

      TimeRunner(RealtimeModel rm) {
        // System.out.println("new time runner");
        context = rm;
        interArrivalTimes = new ArrayList<>();
        timeStamps = new LinkedList<>();
        timeStampsBuffer = new LinkedList<>();
        logMonitor = GCLogMonitor.getInstance();
      }

      @Override
      public void run() {
        timeStampsBuffer.add(System.nanoTime());
        checkConsistency();
        context.tickImpl();
        if (nextTrigger != null) {
          executor.shutdown();
        }
      }

      void checkConsistency() {
        // System.out.println(timeStampsBuffer.size() + " " + timeStamps.size()
        // + " " + interArrivalTimes.size());
        // check if GCLogMonitor has a time AFTER the timestamp (in that
        // case we are sure that we have complete information)
        while (!timeStampsBuffer.isEmpty() &&
            logMonitor.hasSurpassed(timeStampsBuffer.peekFirst().longValue())) {
          // System.out.println("found one");
          timeStamps.add(timeStampsBuffer.removeFirst());
        }

        while (timeStamps.size() > 1) {
          final long ts1 = timeStamps.removeFirst();
          final long ts2 = timeStamps.peekFirst();
          long interArrivalTime = ts2 - ts1;
          checkState(interArrivalTime > 0);

          // compute correction in interval of [ts1, ts2)
          final long correction = GCLogMonitor.getInstance()
              .getPauseTimeInInterval(ts1, ts2);
          // System.out.println("correction: " + correction);

          if (interArrivalTime >= maxTickDuration) {
            // the max is taken because the correction can be too big in certain
            // situations. Example: if GC took 1500ms, tick size is 1000ms then
            // the actual inter arrival time is probably in the range
            // 1500-1600ms. If you apply the correction directly, the inter
            // arrival time would be incorrectly low, therefore we make sure
            // that the corrected inter arrival time cannot be lower than the
            // tick length.

            interArrivalTime =
                Math.max(interArrivalTime - correction, tickNanoSeconds);
          }
          if (interArrivalTime >= maxTickDuration
              || interArrivalTime <= 800000000) {
            throw new IllegalStateException(interArrivalTime
                + " is invalid (limit: " + maxTickDuration
                + ", correction: " + correction + "). Dump: "
                + context.printTLDump());
          }
          interArrivalTimes.add(interArrivalTime);
        }

        if (interArrivalTimes.size() < CONSISTENCY_CHECK_LENGTH) {
          return;
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

      // TODO is this needed?
      void awaitCorrectness() {
        if (!timeStampsBuffer.isEmpty() &&
            !logMonitor.hasSurpassed(timeStampsBuffer.peekLast().longValue())) {
          try {
            Thread.sleep(200);
          } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
          }
        }
        checkConsistency();
      }
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
    final TickListener delegate;
    final long maxTickDuration;
    long tickDuration;

    TickListenerTimingChecker(TickListener tl, long mxTickDuration) {
      delegate = tl;
      maxTickDuration = mxTickDuration;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
      final long start = System.nanoTime();
      delegate.afterTick(timeLapse);
      final long afterTickDuration = System.nanoTime() - start;

      checkState(
          tickDuration + afterTickDuration < maxTickDuration,
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
}
