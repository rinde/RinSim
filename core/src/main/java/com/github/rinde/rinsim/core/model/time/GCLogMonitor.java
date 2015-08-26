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

import static com.google.common.base.Verify.verifyNotNull;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.annotation.Nullable;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;

/**
 *
 * @author Rinde van Lon
 */
public final class GCLogMonitor {
  static final long S_TO_NS = 1000000000L;
  static final long HISTORY_LENGTH = 10 * S_TO_NS;
  static final long LOG_PARSER_DELAY = 200L;
  static final int QUEUE_EXPECTED_SIZE = 50;
  static final String FILTER =
      "Total time for which application threads were stopped";

  @Nullable
  private static volatile GCLogMonitor instance;

  Accumulator accum;
  long total;

  MinMaxPriorityQueue<PauseTime> pauseTimes;

  GCLogMonitor() {
    accum = new Accumulator();
    Tailer.create(new File("gclog.txt"), accum, LOG_PARSER_DELAY);

    pauseTimes = MinMaxPriorityQueue.orderedBy(Ordering.natural().reverse())
        .expectedSize(QUEUE_EXPECTED_SIZE)
        .create();
  }

  public static GCLogMonitor getInstance() {
    if (instance != null) {
      return instance;
    }
    return instance = new GCLogMonitor();
  }

  long getNewPauseTimes() {
    return total;
  }

  // should return pause time in interval [start,end)
  long getPauseTimeInLast(long ns) {
    final long uptime =
        ManagementFactory.getRuntimeMXBean().getUptime() * S_TO_NS;
    final long until = uptime - ns;

    long duration = 0;
    for (final PauseTime pt : pauseTimes) {
      if (pt.getTime() < until) {
        break;
      }
      duration += pt.getDuration();
    }
    return duration;
  }

  class Accumulator extends TailerListenerAdapter {

    Accumulator() {}

    @Override
    public void handle(@Nullable String line) {
      if (line != null && line.contains(FILTER)) {
        final String[] parts = line.split(": ");

        System.out.println(parts[0] + " " + parts[2]);

        final Double t = Doubles.tryParse(parts[0]);
        if (t == null) {
          return;
        }
        final long time = (long) (S_TO_NS * t);

        final Double d =
            Doubles.tryParse(parts[2].substring(0, parts[2].length() - 8));
        if (d == null) {
          return;
        }
        final long duration = (long) (S_TO_NS * d);

        pauseTimes.add(PauseTime.create(time, duration));

        while (time - pauseTimes.peekLast().getTime() > HISTORY_LENGTH) {
          pauseTimes.pollLast();
        }
        System.out.println("queue size: " + pauseTimes.size());
      }
    }
  }

  @AutoValue
  abstract static class PauseTime implements Comparable<PauseTime> {
    abstract long getTime();

    abstract long getDuration();

    @Override
    public int compareTo(@Nullable PauseTime o) {
      final PauseTime other = verifyNotNull(o);
      return ComparisonChain.start().compare(getTime(), other.getTime())
          .compare(getDuration(), other.getDuration())
          .result();
    }

    static PauseTime create(long time, long duration) {
      return new AutoValue_GCLogMonitor_PauseTime(time, duration);
    }

  }
}
