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
package com.github.rinde.rinsim.core.model.time;

import static com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType.SWITCH_TO_REAL_TIME;
import static com.github.rinde.rinsim.core.model.time.RealtimeClockController.RtClockEventType.SWITCH_TO_SIM_TIME;
import static com.github.rinde.rinsim.core.model.time.RealtimeModel.SimpleState.INIT_RT;
import static com.github.rinde.rinsim.core.model.time.RealtimeModel.SimpleState.INIT_ST;
import static com.github.rinde.rinsim.core.model.time.RealtimeModel.SimpleState.STOPPED;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.unit.SI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.fsm.AbstractState;
import com.github.rinde.rinsim.fsm.StateMachine;
import com.github.rinde.rinsim.fsm.StateMachine.StateMachineEvent;
import com.github.rinde.rinsim.fsm.StateMachine.StateTransitionEvent;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import net.openhft.affinity.AffinityLock;

/**
 * @author Rinde van Lon
 *
 */
class RealtimeModel extends TimeModel implements RealtimeClockController {
  static final Logger LOGGER = LoggerFactory.getLogger(RealtimeModel.class);

  final StateMachine<Trigger, RealtimeModel> stateMachine;

  @Nullable
  AffinityLock affinityLock;
  final Realtime realtimeState;

  RealtimeModel(RealtimeBuilder builder) {
    super(builder, RtClockEventType.values());
    LOGGER.trace("Constructor");

    final long tickNanos = Measure.valueOf(timeLapse.getTickLength(),
      timeLapse.getTimeUnit()).longValue(SI.NANO(SI.SECOND));

    realtimeState = new Realtime(tickNanos);
    final SimulatedTime st = new SimulatedTime();
    stateMachine = StateMachine
      .create(
        builder.getClockMode() == ClockMode.REAL_TIME ? INIT_RT : INIT_ST)
      .addTransition(INIT_RT, Trigger.SIMULATE, INIT_ST)
      .addTransition(INIT_RT, Trigger.START, realtimeState)
      .addTransition(INIT_ST, Trigger.REAL_TIME, INIT_RT)
      .addTransition(INIT_ST, Trigger.START, st)
      .addTransition(realtimeState, Trigger.SIMULATE, realtimeState)
      .addTransition(realtimeState, Trigger.REAL_TIME, realtimeState)
      .addTransition(realtimeState, Trigger.DO_SIMULATE, st)
      .addTransition(realtimeState, Trigger.STOP, STOPPED)
      .addTransition(st, Trigger.REAL_TIME, st)
      .addTransition(st, Trigger.SIMULATE, st)
      .addTransition(st, Trigger.DO_REAL_TIME, realtimeState)
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

        LOGGER.debug("{} {}", timeLapse, event);
        if ((event.newState == realtimeState || event.newState == INIT_RT)
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
    LOGGER.trace("cleanUpAfterException");
    shutdownExecutor();
    super.cleanUpAfterException();
  }

  void shutdownExecutor() {
    LOGGER.trace("Shutting down executor..");
    final ListeningScheduledExecutorService ex = realtimeState.executor;
    if (ex != null) {
      ex.shutdown();

      // in case the future could not be cancelled before, do it now
      final ListenableScheduledFuture<?> fut = realtimeState.schedulerFuture;
      if (fut != null && !fut.isDone()) {
        realtimeState.cancelTask();
      }

      try {
        ex.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
      } catch (final InterruptedException e) {
        throw new IllegalStateException(e);
      }
      LOGGER.trace("Executor shutdown.");
    }
    verifyNotNull(affinityLock).release();
  }

  boolean isExecutorAlive() {
    return realtimeState.executor != null
      && !realtimeState.executor.isTerminated();
  }

  @Override
  void doStart() {
    checkState(stateMachine.isSupported(Trigger.START),
      "%s can be started only once",
      getClass().getSimpleName());
    final AffinityLock lock = AffinityLock.acquireLock();
    LOGGER.info("Acquired lock to CPU {}.", lock.cpuId());
    affinityLock = lock;
    stateMachine.handle(Trigger.START, this);
  }

  @Override
  public void stop() {
    LOGGER.trace("stop");
    checkState(stateMachine.isSupported(Trigger.STOP),
      "Can not stop time in current state: %s",
      stateMachine.getCurrentState().name());
    final boolean rt = stateMachine.stateIs(realtimeState);
    stateMachine.handle(Trigger.STOP, this);
    if (!rt) {
      shutdownExecutor();
    }
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

  public ImmutableList<RealtimeTickInfo> getTickInfoList() {
    return realtimeState.getTickInfoList();
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
      try {
        while (isTicking) {
          context.tickImpl();
        }
      } catch (final RuntimeException e) {
        LOGGER.error(e.getMessage(), e);
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

    @Override
    public String toString() {
      return "SimulatedTime";
    }
  }

  static class Realtime extends AbstractClockState {
    // sleep period when waiting for clock termination
    private static final long THREAD_SLEEP_MS = 50L;

    final long tickNanos;
    final List<Throwable> exceptions;
    @Nullable
    Trigger nextTrigger;
    @Nullable
    ListeningScheduledExecutorService executor;

    @Nullable
    ListenableScheduledFuture<?> schedulerFuture;

    AtomicBoolean taskIsRunning;
    AtomicBoolean isShuttingDown;

    // keeps time for last real-time request while in RT mode
    long lastRtRequest;

    final List<TimeRunner> timeRunners;

    Realtime(long tickNs) {
      tickNanos = tickNs;
      taskIsRunning = new AtomicBoolean();
      isShuttingDown = new AtomicBoolean();
      exceptions = new ArrayList<>();
      timeRunners = new ArrayList<>();
    }

    public ImmutableList<RealtimeTickInfo> getTickInfoList() {
      final ImmutableList.Builder<RealtimeTickInfo> infoBuilder =
        ImmutableList.builder();
      for (final TimeRunner tr : timeRunners) {
        infoBuilder.addAll(tr.computeTickInfoList());
      }
      return infoBuilder.build();
    }

    @Override
    public void onEntry(Trigger event, RealtimeModel context) {
      if (executor == null) {
        LOGGER.trace("starting executor..");
        executor = MoreExecutors.listeningDecorator(
          Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@Nullable Runnable r) {
              return new Thread(r,
                Thread.currentThread().getName() + "-RealtimeModel");
            }
          }));
      }
    }

    @Override
    @Nullable
    public Trigger handle(@Nullable Trigger event,
        final RealtimeModel context) {
      LOGGER.trace("Realtime.handle {}", event);
      if (event == Trigger.SIMULATE) {
        // RT takes precedence over ST, if a request for RT has been made during
        // the same tick, all ST requests are ignored.
        if (context.getCurrentTime() > lastRtRequest) {
          LOGGER.trace("set trigger");
          nextTrigger = Trigger.DO_SIMULATE;
        }
        return null;
      } else if (event == Trigger.REAL_TIME) {
        lastRtRequest = context.getCurrentTime();
        nextTrigger = null;
        return null;
      }

      taskIsRunning.set(true);
      timeRunners.add(new TimeRunner(context));
      schedulerFuture =
        verifyNotNull(executor).scheduleAtFixedRate(
          timeRunners.get(timeRunners.size() - 1),
          0,
          tickNanos,
          TimeUnit.NANOSECONDS);
      final ListenableScheduledFuture<?> future = schedulerFuture;
      Futures.addCallback(future, new FutureCallback<Object>() {
        @Override
        public void onFailure(Throwable t) {
          if (!(t instanceof CancellationException)) {
            exceptions.add(t);
          } else {
            LOGGER.trace("{} cancel execution", context.timeLapse);
          }
          taskIsRunning.set(false);
        }

        @Override
        public void onSuccess(@Nullable Object result) {}
      });
      awaitTermination(context);
      LOGGER.trace("end of realtime, next trigger {}", nextTrigger);
      final Trigger t = nextTrigger;
      nextTrigger = null;
      return t;
    }

    @Override
    public void onExit(Trigger event, RealtimeModel context) {
      LOGGER.trace("Realtime onExit {}", event);
      cancelTask();
      if (event == Trigger.STOP) {
        isShuttingDown.set(true);
      }
    }

    void cancelTask() {
      final ListenableScheduledFuture<?> f = verifyNotNull(schedulerFuture);
      if (!f.isDone()) {
        LOGGER.trace("initiate cancel RT clock");
        f.cancel(true);
      }
    }

    void awaitTermination(RealtimeModel context) {
      LOGGER.trace("awaiting RT clock termination..");
      try {
        while (taskIsRunning.get()) {
          Thread.sleep(THREAD_SLEEP_MS);
        }
      } catch (final InterruptedException e) {
        LOGGER.warn("Received interrupt, stopping simulator.");
        context.stop();
      }
      checkExceptions(context);
      if (isShuttingDown.get()) {
        context.shutdownExecutor();
      }
    }

    void checkExceptions(RealtimeModel context) {
      if (!exceptions.isEmpty()) {
        context.cleanUpAfterException();
        LOGGER.error(exceptions.get(0).getMessage(), exceptions.get(0));
        if (exceptions.get(0) instanceof RuntimeException) {
          throw (RuntimeException) exceptions.get(0);
        } else if (exceptions.get(0) instanceof Error) {
          throw (Error) exceptions.get(0);
        }
        throw new IllegalStateException(exceptions.get(0));
      }
    }

    @Override
    public ClockMode getClockMode() {
      return ClockMode.REAL_TIME;
    }

    @Override
    public String toString() {
      return "Realtime";
    }

    class TimeRunner implements Runnable {
      final List<Timestamp> timeStamps;
      final RealtimeModel context;
      long counter;

      TimeRunner(RealtimeModel rm) {
        context = rm;
        timeStamps = new ArrayList<>();
      }

      Iterable<? extends RealtimeTickInfo> computeTickInfoList() {
        if (timeStamps.size() <= 1) {
          return Collections.emptySet();
        }
        final List<RealtimeTickInfo> deviations = new ArrayList<>();
        final PeekingIterator<Timestamp> it =
          Iterators.peekingIterator(timeStamps.iterator());

        Timestamp cur = it.next();
        while (it.hasNext()) {
          deviations.add(RealtimeTickInfo.create(cur, it.peek()));
          cur = it.next();
        }
        return deviations;
      }

      @Override
      public void run() {
        timeStamps.add(Timestamp.now(counter));
        context.tickImpl();
        LOGGER.trace("tick {} is done, nextTrigger: {} ", counter, nextTrigger);
        if (nextTrigger != null) {
          cancelTask();
        }
        counter++;
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
}
