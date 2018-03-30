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

import static com.google.common.base.Verify.verifyNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.experiment.Experiment.Builder;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

final class LocalComputer implements Computer {
  static final Logger LOGGER = LoggerFactory.getLogger(LocalComputer.class);
  static final long THREAD_SLEEP_TIME_MS = 10L;
  static final long MAX_WAIT_FOR_SHUTDOWN_S = 10L;

  LocalComputer() {}

  @Override
  public ExperimentResults compute(Builder builder, Set<SimArgs> inputs) {
    final ImmutableList.Builder<ExperimentRunner> runnerBuilder =
      ImmutableList.builder();
    for (final SimArgs args : inputs) {
      runnerBuilder.add(new ExperimentRunner(args));
    }

    final List<ExperimentRunner> runners = runnerBuilder.build();

    final int threads = Math.min(builder.numThreads, runners.size());
    final ListeningExecutorService executor;
    if (builder.showGui) {
      executor = MoreExecutors.newDirectExecutorService();
    } else {
      executor = MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(threads, new LocalThreadFactory()));
    }

    final List<SimulationResult> results =
      Collections.synchronizedList(new ArrayList<SimulationResult>());
    final ResultCollector resultCollector =
      new ResultCollector(executor, results, builder.resultListeners);

    try {
      for (final ExperimentRunner r : runners) {
        checkForError(executor, resultCollector);
        final ListenableFuture<SimulationResult> f = executor.submit(r);
        Futures.addCallback(f, resultCollector);
      }
      while (results.size() < inputs.size() && !resultCollector.hasError()) {
        Thread.sleep(THREAD_SLEEP_TIME_MS);
      }
      checkForError(executor, resultCollector);
    } catch (final InterruptedException e) {
      LOGGER.trace("Interrupt, shutting down the executor.");
      executor.shutdownNow();
      LOGGER.trace("Waiting for executor to shutdown.");
      try {
        final boolean executorStopped =
          executor.awaitTermination(MAX_WAIT_FOR_SHUTDOWN_S, TimeUnit.SECONDS);
        if (executorStopped) {
          LOGGER.trace("Executor is shutdown.");
        } else {
          LOGGER.warn("Executor did not stop, timed out after {} seconds.",
            MAX_WAIT_FOR_SHUTDOWN_S);
        }
      } catch (final InterruptedException e1) {
        LOGGER.warn("Waiting for executor to shutdown is interrupted.");
      }
      return ExperimentResults.create(builder,
        ImmutableSet.<SimulationResult>of());
    }

    checkForError(executor, resultCollector);
    executor.shutdown();

    final ExperimentResults er =
      ExperimentResults.create(builder, ImmutableSet.copyOf(results));
    for (final ResultListener rl : builder.resultListeners) {
      rl.doneComputing(er);
    }
    return er;
  }

  static void checkForError(ListeningExecutorService executor,
      ResultCollector collector) {
    if (collector.hasError()) {
      LOGGER.info("Found error, shutting down. {}", collector.getFirstError());
      executor.shutdown();
      if (collector.getFirstError() instanceof RuntimeException) {
        throw (RuntimeException) collector.getFirstError();
      }
      throw new IllegalStateException(collector.getFirstError());
    }
  }

  static class LocalThreadFactory implements ThreadFactory {
    static final AtomicInteger THREAD_ID = new AtomicInteger(0);

    LocalThreadFactory() {}

    @Override
    public Thread newThread(@Nullable Runnable r) {
      return new Thread(r, "RinSim-exp-" + THREAD_ID.getAndIncrement());
    }
  }

  static class ResultCollector implements FutureCallback<SimulationResult> {
    final ListeningExecutorService executor;
    final List<SimulationResult> results;
    final List<ResultListener> resultListeners;

    volatile List<Throwable> throwables;

    ResultCollector(ListeningExecutorService ex, List<SimulationResult> res,
        List<ResultListener> listeners) {
      executor = ex;
      results = res;
      resultListeners = listeners;
      throwables = new ArrayList<>();
    }

    public boolean hasError() {
      return !throwables.isEmpty();
    }

    public Throwable getFirstError() {
      return throwables.get(0);
    }

    @Override
    public void onFailure(Throwable t) {
      throwables.add(t);
      executor.shutdownNow();
    }

    @Override
    public void onSuccess(@Nullable SimulationResult result) {
      final SimulationResult res = verifyNotNull(result);
      for (final ResultListener rl : resultListeners) {
        try {
          rl.receive(res);
        } catch (final RuntimeException e) {
          System.err
            .println("ResultListener " + rl + " failed to receive result.");
          e.printStackTrace(System.err);
        }
      }
      if (res.getResultObject() == FailureStrategy.RETRY) {
        final ExperimentRunner newRunner =
          new ExperimentRunner(res.getSimArgs());
        Futures.addCallback(executor.submit(newRunner), this);
      } else {
        results.add(result);
      }
    }
  }

  static class ExperimentRunner implements Callable<SimulationResult> {
    private final SimArgs arguments;

    ExperimentRunner(SimArgs args) {
      arguments = args;
    }

    @Override
    public SimulationResult call() {
      final Object resultObject = Experiment.perform(arguments);
      final SimulationResult result =
        SimulationResult.create(arguments, resultObject);
      return result;
    }
  }
}
