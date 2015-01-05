package com.github.rinde.rinsim.experiment.base;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

final class LocalComputer implements Computer {

  @Override
  public ExperimentResults compute(AbstractExperimentBuilder<?> builder,
      Set<SimArgs> inputs) {
    final ImmutableList.Builder<ExperimentRunner> runnerBuilder = ImmutableList
        .builder();
    for (final SimArgs args : inputs) {
      runnerBuilder.add(builder.createRunner(args));
    }
    final List<ExperimentRunner> runners = runnerBuilder.build();

    final int threads = Math.min(builder.numThreads, runners.size());
    final ListeningExecutorService executor;
    if (threads > 1) {
      executor = MoreExecutors
          .listeningDecorator(Executors.newFixedThreadPool(threads));
    } else {
      executor = MoreExecutors.newDirectExecutorService();
    }
    final List<SimulationResult> results;
    try {
      // safe cast according to javadoc
      @SuppressWarnings({ "unchecked", "rawtypes" })
      final List<ListenableFuture<SimulationResult>> futures = (List) executor
      .invokeAll(runners);
      results = Futures.allAsList(futures).get();
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    } catch (final ExecutionException e) {
      // FIXME need some way to gracefully handle this error. All data
      // should be saved to reproduce this simulation.
      throw new IllegalStateException(e);
    }
    executor.shutdown();

    return new ExperimentResults(builder,
        ImmutableSet.copyOf(results));
  }

}
