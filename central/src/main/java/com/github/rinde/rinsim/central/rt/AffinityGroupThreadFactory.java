/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.central.rt;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.openhft.affinity.Affinity;
import net.openhft.affinity.AffinityLock;

/**
 * A {@link ThreadFactory} that creates {@link Thread}s that all have an
 * affinity for the <i>same</i> CPU. When used in a thread pool, the entire
 * thread pool will be run on the same CPU.
 * @author Rinde van Lon
 */
public final class AffinityGroupThreadFactory implements ThreadFactory {
  static final Logger LOGGER =
    LoggerFactory.getLogger(AffinityGroupThreadFactory.class);
  static final long SLEEP = 1000L;
  static final long SHORT_SLEEP = 10L;

  private final String threadNamePrefix;
  private final boolean createDaemonThreads;
  private final UncaughtExceptionHandler exceptionHandler;
  @Nullable
  private AffinityLock affinityLock;
  private final AtomicInteger numThreads;
  private final AtomicInteger id;

  @Nullable
  private Thread helperThread;

  /**
   * Create a new instance where threads get the specified name prefix. All
   * created threads are daemon threads, see {@link Thread#setDaemon(boolean)}
   * for more information.
   * @param name The thread name prefix.
   * @param uncaughtExceptionHandler The handler for exceptions that are not
   *          caught on the threads created by this factory.
   */
  public AffinityGroupThreadFactory(String name,
      UncaughtExceptionHandler uncaughtExceptionHandler) {
    this(name, uncaughtExceptionHandler, true);
  }

  /**
   * Create a new instance where threads get the specified name prefix. The
   * daemon property is set using {@link Thread#setDaemon(boolean)}.
   * @param name The thread name prefix.
   * @param uncaughtExceptionHandler The handler for exceptions that are not
   *          caught on the threads created by this factory.
   * @param daemon Indicates whether all threads created by this factory are
   *          daemon threads.
   */
  public AffinityGroupThreadFactory(String name,
      UncaughtExceptionHandler uncaughtExceptionHandler, boolean daemon) {
    LOGGER.trace("AffinityGroupThreadFactory {}", name);
    exceptionHandler = uncaughtExceptionHandler;
    numThreads = new AtomicInteger();
    id = new AtomicInteger();
    threadNamePrefix = name;
    createDaemonThreads = daemon;
  }

  void startHelperThread() {
    final Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          LOGGER.info("Start helper thread.");
          acquire();
          while (true) {
            Thread.sleep(SLEEP);
          }
        } catch (final InterruptedException e) {
          release();
        }
        LOGGER.info("End helper thread.");
      }
    });
    t.setName(threadNamePrefix);
    t.setDaemon(true);
    helperThread = t;
    t.start();
    while (affinityLock == null) {
      try {
        Thread.sleep(SHORT_SLEEP);
      } catch (final InterruptedException e) {
        return;
      }
    }
  }

  void acquire() {
    affinityLock = AffinityLock.acquireLock();
  }

  void release() {
    verifyNotNull(affinityLock).release();
    affinityLock = null;
  }

  @Override
  public synchronized Thread newThread(@Nullable final Runnable r) {
    final int num = id.getAndIncrement();
    final String threadName = String.format("%s-%s", threadNamePrefix, num);

    LOGGER.info("Create new thread called '{}'.", threadName);
    final Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        addThread();
        verifyNotNull(r).run();
        removeThread();
      }
    }, threadName);
    t.setUncaughtExceptionHandler(exceptionHandler);
    t.setDaemon(createDaemonThreads);
    return t;
  }

  synchronized void addThread() {
    if (helperThread == null) {
      startHelperThread();
    }

    numThreads.incrementAndGet();
    LOGGER.info("Starting {}.", Thread.currentThread().getName());
    final AffinityLock lock = verifyNotNull(affinityLock);
    checkState(lock.isAllocated(), "Failed to allocate lock: %s.", lock);
    Affinity.setAffinity(lock.cpuId());
  }

  synchronized void removeThread() {
    LOGGER.info("End of {}.", Thread.currentThread().getName());

    if (numThreads.decrementAndGet() == 0) {
      LOGGER.info("No more running threads");
      verifyNotNull(helperThread).interrupt();
      helperThread = null;
    }
  }

  @Override
  public String toString() {
    return String.format("%s{%s}", getClass().getSimpleName(),
      threadNamePrefix);
  }
}
