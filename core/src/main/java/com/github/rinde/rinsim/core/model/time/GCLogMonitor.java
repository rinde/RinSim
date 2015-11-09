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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;

/**
 *
 * @author Rinde van Lon
 */
public final class GCLogMonitor {
  static final long S_TO_MS = 1000L;
  static final long S_TO_NS = 1000000000L;
  static final long HISTORY_LENGTH_NS = 30 * S_TO_NS;
  static final long LOG_PARSER_DELAY_MS = 200L;
  static final int QUEUE_EXPECTED_SIZE = 50;
  static final String FILTER =
      "Total time for which application threads were stopped";
  static final String PRINT_ARG = "-XX:+PrintGCApplicationStoppedTime";
  static final String LOG_ARG = "-Xloggc:";

  private static volatile Optional<GCLogMonitor> instance = Optional.absent();

  final long startTimeMillis;
  final Deque<Range<Long>> pauseIntervals;
  final Tailer tailer;
  final List<Throwable> exceptions;
  final String logPath;

  GCLogMonitor(@Nullable String path) {
    exceptions = new ArrayList<>();
    if (path == null) {
      final RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
      final List<String> arguments = runtimeMxBean.getInputArguments();

      String providedLogFile = null;
      boolean foundPrintArg = false;
      for (final String arg : arguments) {
        if (arg.startsWith(LOG_ARG)) {
          providedLogFile = arg.substring(LOG_ARG.length());
        } else if (arg.equals(PRINT_ARG)) {
          foundPrintArg = true;
        }
      }
      checkArgument(providedLogFile != null && foundPrintArg,
          "Expected VM arguments: '%s<somefile> %s' but found '%s'.", LOG_ARG,
          PRINT_ARG, arguments);

      logPath = providedLogFile;
    } else {
      logPath = path;
    }
    pauseIntervals = Queues.synchronizedDeque(new LinkedList<Range<Long>>());
    startTimeMillis = ManagementFactory.getRuntimeMXBean().getStartTime();

    tailer =
        new Tailer(new File(logPath), new LogListener(), LOG_PARSER_DELAY_MS);
    final Thread thread = new Thread(tailer, "gclog-tailer");
    thread.setDaemon(true);
    thread.start();
  }

  void close() {
    tailer.stop();
    try {
      Thread.sleep(LOG_PARSER_DELAY_MS);
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Checks whether a log entry with the specified time (obtained from
   * {@link System#currentTimeMillis()}) or a later time exists in the log.
   * @param timeMillis The time in milliseconds.
   * @return <code>true</code> if there is a log entry later than or equal to
   *         the specified time, <code>false</code> otherwise.
   */
  public boolean hasSurpassed(long timeMillis) {
    checkInternalState();
    final long timeNs = (timeMillis - startTimeMillis) * 1000000L;

    return !pauseIntervals.isEmpty()
        && pauseIntervals.peekLast().upperEndpoint() - timeNs >= 0;
  }

  /**
   * Computes the pause duration (aka stop-the-world time) in the specified
   * closed interval <code>[ts1,ts2]</code>. These pause times can be caused by
   * the GC, JIT compiler, etc.
   * @param ts1 The start of the interval (inclusive) must be a time obtained
   *          via {@link System#currentTimeMillis()} since the startup of the
   *          VM.
   * @param ts2 The end of the interval (inclusive) must be &gt;
   *          <code>ts1</code>.
   * @return The accumulated duration of pause time (in ns) in the specified
   *         interval.
   * @throws IllegalArgumentException If an illegal interval is specified.
   */
  public long getPauseTimeInInterval(long ts1, long ts2) {
    checkInternalState();
    // convert system times to vm lifetime
    final long vmt1 = (ts1 - startTimeMillis) * 1000000;
    final long vmt2 = (ts2 - startTimeMillis) * 1000000;
    checkArgument(vmt1 >= 0,
        "ts1 must indicate a system time (in ms) since the start of the VM, "
            + "VM was started at %sms, ts1 is %sms.",
        startTimeMillis, ts1);
    checkArgument(vmt2 > vmt1, "ts2 must indicate a system time (in ms) since "
        + "ts1, ts1 %sms, ts2 %sms.", ts1, ts2);
    long durationNs = 0;

    final Range<Long> intervalOfInterest = Range.closedOpen(vmt1, vmt2);

    // iterator starts with oldest times
    synchronized (pauseIntervals) {
      for (final Range<Long> pt : pauseIntervals) {
        // if vmt2 < pt time -> we are outside the interval, we can stop the
        // loop

        if (intervalOfInterest.upperEndpoint() < pt.lowerEndpoint()) {
          break;
        }
        // if vmt1 <= pt time -> we are inside the interval, add the
        // intersection to the duration

        if (intervalOfInterest.isConnected(pt)) {
          final Range<Long> intersect = intervalOfInterest.intersection(pt);
          durationNs += intersect.upperEndpoint() - intersect.lowerEndpoint();
        }
      }
    }
    return durationNs;
  }

  public long toVMTime(long time) {
    return time - startTimeMillis;
  }

  public List<Range<Long>> getPauseIntervals() {
    synchronized (pauseIntervals) {

      return ImmutableList.copyOf(pauseIntervals);
    }
  }

  GCLogMonitor checkInternalState() {
    if (!exceptions.isEmpty()) {
      if (exceptions.get(0) instanceof RuntimeException) {
        throw (RuntimeException) exceptions.get(0);
      }
      throw new IllegalStateException(exceptions.get(0));
    }
    return this;
  }

  public static GCLogMonitor getInstance() {
    if (!instance.isPresent()) {
      synchronized (GCLogMonitor.class) {
        if (!instance.isPresent()) {
          return (instance = Optional.of(new GCLogMonitor(null))).get();
        }
      }
    }
    return instance.get().checkInternalState();
  }

  @VisibleForTesting
  static void createInstance(@Nullable String path) {
    synchronized (GCLogMonitor.class) {
      if (instance.isPresent()) {
        instance.get().close();
      }
      instance = Optional.of(new GCLogMonitor(path));
    }
  }

  class LogListener implements TailerListener {

    LogListener() {}

    @Override
    public void handle(@Nullable String line) {
      if (Thread.interrupted()) {
        close();
      } else if (line != null && line.contains(FILTER)) {
        final String[] parts = line.split(": ");

        final Double t = Doubles.tryParse(parts[0]);
        final Double d = Doubles.tryParse(parts[2].split(" ")[0]);
        if (t == null || d == null) {
          throw new IllegalStateException(
              "Encountered GC log entry in unknown format: " + line);
        }

        final long timeNs = (long) (S_TO_NS * t);
        final long durationNs = (long) (S_TO_NS * d);
        if (!pauseIntervals.isEmpty()) {
          checkState(pauseIntervals.peekLast().upperEndpoint() <= timeNs,
              "Time inconsistency detected in the gc log. Last entry: %s, "
                  + "new entry: %s. This may occur if multiple VMs are writing "
                  + "to the same log file (%s).",
              pauseIntervals.peekLast().upperEndpoint(), timeNs, logPath);
        }

        final long startNs = timeNs - durationNs;
        final long endNs = timeNs;

        // add new info at the back
        pauseIntervals.add(Range.closedOpen(startNs, endNs));

        // remove old info at the front
        while (timeNs
            - pauseIntervals.peekFirst().upperEndpoint() > HISTORY_LENGTH_NS) {
          pauseIntervals.pollFirst();
        }
      }
    }

    @Override
    public void handle(@SuppressWarnings("null") Exception e) {
      exceptions.add(e);
      close();
    }

    @Override
    public void init(@Nullable Tailer tl) {}

    @Override
    public void fileNotFound() {
      exceptions.add(new IllegalStateException(
          "The gc log file was not found:" + logPath));
    }

    @Override
    public void fileRotated() {
      exceptions.add(new IllegalStateException(
          "The gc log file (" + logPath + ") is rotated, presumably another VM "
              + "is writing to the same file."));
    }
  }

  @AutoValue
  abstract static class PauseTime implements Comparable<PauseTime> {
    // the time in millis at which the pause was reported
    abstract long getTimeMs();

    // the duration in nanos of the pause
    abstract long getDurationNs();

    @Override
    public int compareTo(@Nullable PauseTime o) {
      final PauseTime other = verifyNotNull(o);
      return ComparisonChain.start().compare(getTimeMs(), other.getTimeMs())
          .compare(getDurationNs(), other.getDurationNs())
          .result();
    }

    static PauseTime create(long time, long duration) {
      return new AutoValue_GCLogMonitor_PauseTime(time, duration);
    }
  }
}
