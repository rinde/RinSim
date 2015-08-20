package com.github.rinde.rinsim.central.rt;

import static com.google.common.base.Verify.verifyNotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import net.openhft.affinity.AffinityLock;
import net.openhft.affinity.AffinitySupport;

public class AffinityGroupThreadFactory implements ThreadFactory {
  private final String name;
  private final boolean daemon;
  @Nullable
  private AffinityLock lastAffinityLock = null;

  private int id;
  private final AtomicInteger numThreads;

  public AffinityGroupThreadFactory(String name) {
    this(name, true);
  }

  public AffinityGroupThreadFactory(String name, boolean daemon) {
    numThreads = new AtomicInteger();
    this.name = name;
    this.daemon = daemon;
  }

  @Override
  public synchronized Thread newThread(final @Nullable Runnable r) {
    final String threadName = id == 0 ? name : name + '-' + id;
    numThreads.incrementAndGet();
    final Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        synchronized (numThreads) {
          if (lastAffinityLock != null) {
            final AffinityLock al = lastAffinityLock;
            AffinitySupport.setAffinity(1 << al.cpuId());
          } else {
            lastAffinityLock = AffinityLock.acquireLock();
          }
        }
        try {
          verifyNotNull(r).run();
        } finally {
          synchronized (numThreads) {
            if (numThreads.decrementAndGet() == 0 && lastAffinityLock != null) {
              lastAffinityLock.release();
              lastAffinityLock = null;
            }
          }
        }
      }
    }, threadName);
    t.setDaemon(daemon);
    return t;
  }
}
