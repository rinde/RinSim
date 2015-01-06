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

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.experiment.base.ExperimentBuilder.FunctionRunner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

final class LocalComputer implements Computer {

  @Override
  public ExperimentResults compute(ExperimentBuilder<?> builder,
      Set<SimArgs> inputs) {
    final ImmutableList<ResultListener> listeners = ImmutableList
        .copyOf(builder.resultListeners);

    final ImmutableList.Builder<SimTask> tasksBuilder = ImmutableList
        .builder();
    for (final SimArgs args : inputs) {
      tasksBuilder.add(new FunctionRunner(args, builder.computeFunction));
    }

    final List<SimTask> tasks = tasksBuilder.build();

    for (final ResultListener l : listeners) {
      l.startComputing(tasks.size());
    }

    final int threads = Math.min(builder.numThreads, tasks.size());
    final ListeningExecutorService executor;
    if (threads > 1) {
      executor = MoreExecutors
          .listeningDecorator(Executors.newFixedThreadPool(threads));
    } else {
      executor = MoreExecutors.newDirectExecutorService();
    }

    final List<SimResult> results;
    try {
      // safe cast according to javadoc
      @SuppressWarnings({ "unchecked", "rawtypes" })
      final List<ListenableFuture<SimResult>> futures = (List) executor
          .invokeAll(tasks);

      if (!listeners.isEmpty()) {
        for (ListenableFuture<SimResult> fut : futures) {
          Futures.addCallback(fut, new FutureCallback<SimResult>() {
            @Override
            public void onSuccess(@Nullable SimResult result) {
              requireNonNull(result);
              for (ResultListener list : listeners) {
                list.receive(result);
              }
            }

            @Override
            public void onFailure(Throwable t) {
              // FIXME need some way to gracefully handle this error. All data
              // should be saved to reproduce this simulation.
              throw new IllegalStateException(t);
            }
          });
        }
      }

      results = Futures.allAsList(futures).get();
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    } catch (final ExecutionException e) {
      // FIXME need some way to gracefully handle this error. All data
      // should be saved to reproduce this simulation.
      throw new IllegalStateException(e);
    }
    executor.shutdown();
    for (final ResultListener l : listeners) {
      l.doneComputing();
    }
    return new ExperimentResults(builder,
        ImmutableSet.copyOf(results));
  }
}
